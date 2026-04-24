package com.applyflow.jobcopilot.vacancies.infrastructure.integration.remotive;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.IngestionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RemotiveVacancySourceConnectorTest {

    @Test
    void shouldSendEffectiveLimitToRemotiveApi() {
        ObjectMapper objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://remotive.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://remotive.com/api/remote-jobs?limit=150"))
                .andRespond(withSuccess("""
                        {"jobs":[{"id":1,"url":"https://remotive.com/jobs/1","title":"Backend Engineer","company_name":"Acme","candidate_required_location":"Worldwide","job_type":"full_time","tags":["Java","Senior"],"published_date":"2026-04-23T10:00:00Z","description":"Build backend services with Java and PostgreSQL."}]}
                        """, MediaType.APPLICATION_JSON));

        IngestionProperties properties = new IngestionProperties();
        properties.getSources().getRemotive().setMaxJobsPerRun(300);

        ObjectNode configJson = objectMapper.createObjectNode()
                .put("tenant", "remotive.com")
                .put("maxJobsPerRun", 150);
        VacancySourceConfiguration sourceConfig = new VacancySourceConfiguration(
                UUID.randomUUID(),
                VacancyIngestionSource.REMOTIVE,
                "Remotive Public",
                configJson,
                true
        );

        RemotiveVacancySourceConnector connector = new RemotiveVacancySourceConnector(
                builder.build(),
                objectMapper,
                properties,
                new RemotivePayloadMapper(objectMapper)
        );

        List<ExternalVacancyRecord> result = connector.fetch(sourceConfig, 0);

        assertEquals(1, result.size());
        assertEquals("1", result.getFirst().externalJobId());
        server.verify();
    }

    @Test
    void shouldFetchConfiguredCategoriesWithSafeLimitAndDeduplicate() {
        ObjectMapper objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://remotive.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://remotive.com/api/remote-jobs?limit=100&category=software-dev"))
                .andRespond(withSuccess("""
                        {"jobs":[{"id":1,"url":"https://remotive.com/jobs/1","title":"Backend Engineer","company_name":"Acme","candidate_required_location":"Worldwide","job_type":"full_time","tags":["Java"],"published_date":"2026-04-23T10:00:00Z","description":"Build backend services."}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://remotive.com/api/remote-jobs?limit=99&category=devops"))
                .andRespond(withSuccess("""
                        {"jobs":[{"id":1,"url":"https://remotive.com/jobs/1","title":"Backend Engineer","company_name":"Acme","candidate_required_location":"Worldwide","job_type":"full_time","tags":["Java"],"published_date":"2026-04-23T10:00:00Z","description":"Build backend services."},{"id":2,"url":"https://remotive.com/jobs/2","title":"SRE","company_name":"OpsCo","candidate_required_location":"Worldwide","job_type":"full_time","tags":["DevOps"],"published_date":"2026-04-23T11:00:00Z","description":"Operate production systems."}]}
                        """, MediaType.APPLICATION_JSON));

        IngestionProperties properties = new IngestionProperties();
        ObjectNode configJson = objectMapper.createObjectNode()
                .put("tenant", "remotive.com")
                .put("maxJobsPerRun", 100);
        configJson.putArray("categories")
                .add("software-dev")
                .add("devops");
        VacancySourceConfiguration sourceConfig = new VacancySourceConfiguration(
                UUID.randomUUID(),
                VacancyIngestionSource.REMOTIVE,
                "Remotive Public",
                configJson,
                true
        );

        RemotiveVacancySourceConnector connector = new RemotiveVacancySourceConnector(
                builder.build(),
                objectMapper,
                properties,
                new RemotivePayloadMapper(objectMapper)
        );

        List<ExternalVacancyRecord> result = connector.fetch(sourceConfig, 0);

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).externalJobId());
        assertEquals("2", result.get(1).externalJobId());
        server.verify();
    }
}
