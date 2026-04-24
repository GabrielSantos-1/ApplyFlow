package com.applyflow.jobcopilot.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "staging.load.enabled", matches = "true")
class StagingOperationalLoadTest {
    private static final String LOGIN_PAYLOAD = "{\"email\":\"load@test.local\",\"password\":\"WrongPassword123!\"}";

    @Test
    void shouldValidateDistributedRateLimitUnderControlledLoad() throws Exception {
        List<String> baseUrls = resolveBaseUrls();
        int totalRequests = Integer.getInteger("staging.load.totalRequests", 200);
        int concurrency = Integer.getInteger("staging.load.concurrency", 30);
        long maxP99Millis = Long.getLong("staging.load.maxP99Ms", 2000L);

        List<RequestResult> results = executeRequests(baseUrls, totalRequests, concurrency, this::loginRequest);
        Map<Integer, Long> byStatus = aggregateByStatus(results);
        long blocked429 = byStatus.getOrDefault(429, 0L);
        long serverErrors = countServerErrors(results);
        long redisMode = results.stream().filter(r -> "redis".equalsIgnoreCase(r.mode())).count();
        long fallbackOrUnavailable = results.stream()
                .filter(r -> "in-memory-fallback".equalsIgnoreCase(r.mode()) || "unavailable".equalsIgnoreCase(r.mode()))
                .count();

        long p95 = percentileMillis(results, 0.95);
        long p99 = percentileMillis(results, 0.99);

        assertEquals(0, serverErrors, "5xx nao esperado no baseline de staging");
        assertTrue(blocked429 > 0, "Esperado volume de 429 sob carga controlada");
        assertTrue(redisMode > 0, "Esperado modo redis para validar distribuicao entre instancias");
        assertEquals(0, fallbackOrUnavailable, "Nao esperado fallback/unavailable no baseline operacional");
        assertTrue(p99 <= maxP99Millis, "p99 acima do limite operacional configurado");

        System.out.printf(
                "STAGING_LOAD_SUMMARY total=%d status=%s p95Ms=%d p99Ms=%d redisMode=%d blocked429=%d%n",
                results.size(), byStatus, p95, p99, redisMode, blocked429
        );
    }

    @Test
    void shouldKeepAuthorizationResponsesStableUnderReadLoad() throws Exception {
        List<String> baseUrls = resolveBaseUrls();
        int totalRequests = Integer.getInteger("staging.authz.totalRequests", 120);
        int concurrency = Integer.getInteger("staging.authz.concurrency", 20);

        List<RequestResult> results = executeRequests(baseUrls, totalRequests, concurrency, this::vacanciesRequest);
        Map<Integer, Long> byStatus = aggregateByStatus(results);
        long serverErrors = countServerErrors(results);
        long unauthorized = byStatus.getOrDefault(401, 0L);
        long rateLimited = byStatus.getOrDefault(429, 0L);

        assertEquals(0, serverErrors, "Nao esperado 5xx em carga de authz sem autenticacao");
        assertTrue((unauthorized + rateLimited) > 0, "Esperado 401/429 durante carga de autorizacao");

        System.out.printf(
                "STAGING_AUTHZ_SUMMARY total=%d status=%s%n",
                results.size(), byStatus
        );
    }

    private HttpRequest loginRequest(String baseUrl) {
        return HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(LOGIN_PAYLOAD))
                .build();
    }

    private HttpRequest vacanciesRequest(String baseUrl) {
        return HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/vacancies?page=0&size=20"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
    }

    private List<RequestResult> executeRequests(List<String> baseUrls,
                                                int totalRequests,
                                                int concurrency,
                                                RequestFactory requestFactory) throws InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        ConcurrentLinkedQueue<RequestResult> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < totalRequests; i++) {
            final int requestIndex = i;
            executor.submit(() -> {
                String baseUrl = baseUrls.get(requestIndex % baseUrls.size());
                HttpRequest request = requestFactory.create(baseUrl);
                long started = System.nanoTime();
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                    String mode = response.headers().firstValue("X-RateLimit-Mode").orElse("none");
                    results.add(new RequestResult(baseUrl, response.statusCode(), mode, tookMs));
                } catch (Exception ex) {
                    long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                    results.add(new RequestResult(baseUrl, 599, "exception", tookMs));
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(3, TimeUnit.MINUTES), "Timeout aguardando requests de carga");

        List<RequestResult> output = new ArrayList<>(results);
        assertEquals(totalRequests, output.size(), "Quantidade de respostas diferente da carga enviada");
        return output;
    }

    private Map<Integer, Long> aggregateByStatus(List<RequestResult> results) {
        Map<Integer, Long> byStatus = new ConcurrentHashMap<>();
        for (RequestResult result : results) {
            byStatus.merge(result.status(), 1L, Long::sum);
        }
        return byStatus;
    }

    private long countServerErrors(List<RequestResult> results) {
        return results.stream().filter(r -> r.status() >= 500).count();
    }

    private long percentileMillis(List<RequestResult> results, double percentile) {
        if (results.isEmpty()) {
            return 0;
        }
        List<Long> latencies = results.stream()
                .map(RequestResult::latencyMs)
                .sorted(Comparator.naturalOrder())
                .toList();
        int index = (int) Math.ceil(percentile * latencies.size()) - 1;
        int safeIndex = Math.max(0, Math.min(index, latencies.size() - 1));
        return latencies.get(safeIndex);
    }

    private List<String> resolveBaseUrls() {
        String value = System.getProperty("staging.load.baseUrls", "http://localhost:8081,http://localhost:8082");
        String[] parts = value.split(",");
        List<String> urls = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                urls.add(trimmed);
            }
        }
        assertFalse(urls.isEmpty(), "Nenhuma URL de staging configurada");
        return urls;
    }

    @FunctionalInterface
    private interface RequestFactory {
        HttpRequest create(String baseUrl);
    }

    private record RequestResult(String baseUrl, int status, String mode, long latencyMs) {
    }
}
