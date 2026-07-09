package com.sudolife.adapter.driving.rest.training;

import com.sudolife.application.service.training.CoachingProfileResult;
import com.sudolife.application.service.training.SaveCoachingProfileCommand;
import com.sudolife.application.service.training.exception.InvalidCoachingProfileException;
import com.sudolife.application.service.training.ports.provided.GetCoachingProfileUseCase;
import com.sudolife.application.service.training.ports.provided.SaveCoachingProfileUseCase;
import com.sudolife.config.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CoachingProfileController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class CoachingProfileControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetCoachingProfileUseCase getUseCase;

    @MockitoBean
    private SaveCoachingProfileUseCase saveUseCase;

    @Test
    void get_returns_coaching_profiles_for_authenticated_user() throws Exception {
        when(getUseCase.execute("user@sudolife.com")).thenReturn(result("LOW", true));

        mockMvc.perform(get("/api/coaching-profiles").principal(authenticated("user@sudolife.com", null,
                        java.util.List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDistanceKilometers").value(10.0))
                .andExpect(jsonPath("$.targetPaceSecondsPerKilometer").value(330))
                .andExpect(jsonPath("$.targetDate").value("2026-05-12"))
                .andExpect(jsonPath("$.readiness").value("LOW"))
                .andExpect(jsonPath("$.injuryConcern").value(true))
                .andExpect(jsonPath("$.configured").value(true));

        verify(getUseCase).execute("user@sudolife.com");
    }

    @Test
    void put_saves_coaching_profiles_for_authenticated_user() throws Exception {
        SaveCoachingProfileCommand command = command("LOW", true);
        when(saveUseCase.execute("user@sudolife.com", command)).thenReturn(result("LOW", true));

        mockMvc.perform(put("/api/coaching-profiles")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDistanceKilometers").value(10.0))
                .andExpect(jsonPath("$.readiness").value("LOW"))
                .andExpect(jsonPath("$.injuryConcern").value(true));

        verify(saveUseCase).execute("user@sudolife.com", command);
    }

    @Test
    void put_returns_bad_request_when_coaching_profiles_are_invalid() throws Exception {
        SaveCoachingProfileCommand command = command("UNSUPPORTED", false);
        when(saveUseCase.execute("user@sudolife.com", command))
                .thenThrow(new InvalidCoachingProfileException("Readiness is unsupported"));

        mockMvc.perform(put("/api/coaching-profiles")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COACHING_PROFILE"))
                .andExpect(jsonPath("$.message").value("Readiness is unsupported"));
    }

    private SaveCoachingProfileCommand command(String readiness, boolean injuryConcern) {
        return new SaveCoachingProfileCommand(10.0, 330, LocalDate.parse("2026-05-12"), readiness, injuryConcern);
    }

    private CoachingProfileResult result(String readiness, boolean injuryConcern) {
        return new CoachingProfileResult(10.0, 330, LocalDate.parse("2026-05-12"), readiness, injuryConcern, true);
    }
}
