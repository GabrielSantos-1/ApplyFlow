package com.applyflow.jobcopilot.shared.infrastructure.observability;

public final class EndpointTagResolver {
    private EndpointTagResolver() {
    }

    public static String resolve(String method, String uri) {
        if (uri == null || uri.isBlank()) {
            return "unknown";
        }
        if ("POST".equals(method) && "/api/v1/auth/login".equals(uri)) return "auth.login";
        if ("POST".equals(method) && "/api/v1/auth/refresh".equals(uri)) return "auth.refresh";
        if ("POST".equals(method) && "/api/v1/auth/logout".equals(uri)) return "auth.logout";
        if ("GET".equals(method) && "/api/v1/auth/me".equals(uri)) return "auth.me";

        if ("GET".equals(method) && "/api/v1/vacancies".equals(uri)) return "vacancies.list";
        if ("GET".equals(method) && uri.matches("^/api/v1/vacancies/[^/]+$")) return "vacancies.detail";

        if ("GET".equals(method) && "/api/v1/resumes".equals(uri)) return "resumes.list";
        if ("POST".equals(method) && "/api/v1/resumes".equals(uri)) return "resumes.create";
        if ("GET".equals(method) && uri.matches("^/api/v1/resumes/[^/]+$")) return "resumes.detail";
        if ("POST".equals(method) && uri.matches("^/api/v1/resumes/[^/]+/variants$")) return "resumes.variant.create";

        if ("GET".equals(method) && "/api/v1/applications".equals(uri)) return "applications.list";
        if ("POST".equals(method) && "/api/v1/applications/drafts".equals(uri)) return "applications.draft.create";
        if ("GET".equals(method) && uri.matches("^/api/v1/applications/[^/]+$")) return "applications.detail";
        if ("PATCH".equals(method) && uri.matches("^/api/v1/applications/[^/]+/status$")) return "applications.status.patch";

        if ("POST".equals(method) && "/api/v1/matches".equals(uri)) return "matches.generate";
        if ("GET".equals(method) && uri.matches("^/api/v1/matches/vacancy/[^/]+$")) return "matches.read";
        if ("GET".equals(method) && uri.matches("^/api/v1/matches/vacancy/[^/]+/summary$")) return "matches.summary";
        if ("GET".equals(method) && uri.matches("^/api/v1/matches/[^/]+$")) return "matches.read.legacy";

        if (uri.startsWith("/actuator/")) return "actuator";
        if (uri.startsWith("/api/v1/")) return "api.other";
        return "other";
    }
}
