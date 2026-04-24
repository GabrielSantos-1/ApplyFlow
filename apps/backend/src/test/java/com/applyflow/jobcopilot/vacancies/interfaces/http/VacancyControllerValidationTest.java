package com.applyflow.jobcopilot.vacancies.interfaces.http;

import com.applyflow.jobcopilot.shared.application.dto.PageResponse;
import com.applyflow.jobcopilot.shared.interfaces.http.GlobalExceptionHandler;
import com.applyflow.jobcopilot.vacancies.application.dto.response.VacancyResponse;
import com.applyflow.jobcopilot.vacancies.application.usecase.VacancyIngestionUseCase;
import com.applyflow.jobcopilot.vacancies.application.usecase.VacancyUseCase;
import com.applyflow.jobcopilot.vacancies.domain.VacancyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VacancyControllerValidationTest {
    @Test
    void shouldListVacanciesWithValidParams() throws Exception {
        VacancyUseCase useCase = mock(VacancyUseCase.class);
        VacancyIngestionUseCase ingestionUseCase = mock(VacancyIngestionUseCase.class);
        when(useCase.list(any())).thenReturn(new PageResponse<>(List.of(new VacancyResponse(
                UUID.randomUUID(),
                "t",
                "c",
                "l",
                true,
                "senior",
                "https://example.com/jobs/1",
                OffsetDateTime.now(),
                VacancyStatus.PUBLISHED,
                List.of())), 0, 20, 1, 1));
        VacancyController controller = new VacancyController(useCase, ingestionUseCase);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/v1/vacancies").param("sortBy", "title").param("sortDirection", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidIngestionSource() throws Exception {
        VacancyUseCase useCase = mock(VacancyUseCase.class);
        VacancyIngestionUseCase ingestionUseCase = mock(VacancyIngestionUseCase.class);
        VacancyController controller = new VacancyController(useCase, ingestionUseCase);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(post("/api/v1/vacancies/ingestion/runs")
                        .contentType("application/json")
                        .content("{\"source\":\"unknown\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
