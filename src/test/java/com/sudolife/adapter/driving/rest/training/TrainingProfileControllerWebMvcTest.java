package com.sudolife.adapter.driving.rest.training;

import com.sudolife.application.service.training.SaveTrainingProfileCommand;
import com.sudolife.application.service.training.TrainingProfileResult;
import com.sudolife.application.service.training.exception.InvalidTrainingProfileException;
import com.sudolife.application.service.training.ports.provided.GetTrainingProfileUseCase;
import com.sudolife.application.service.training.ports.provided.SaveTrainingProfileUseCase;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = TrainingProfileController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class TrainingProfileControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetTrainingProfileUseCase getUseCase;

    @MockitoBean
    private SaveTrainingProfileUseCase saveUseCase;

    @Test
    void get_returns_training_profile_for_authenticated_user() throws Exception {
        when(getUseCase.execute("user@sudolife.com")).thenReturn(new TrainingProfileResult(1990, true));

        mockMvc.perform(get("/api/training-profile").principal(authenticated("user@sudolife.com", null, java.util.List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthYear").value(1990))
                .andExpect(jsonPath("$.adaptiveCoachingEligible").value(true));

        verify(getUseCase).execute("user@sudolife.com");
    }

    @Test
    void put_saves_training_profile_for_authenticated_user() throws Exception {
        SaveTrainingProfileCommand command = new SaveTrainingProfileCommand(1990);
        when(saveUseCase.execute("user@sudolife.com", command)).thenReturn(new TrainingProfileResult(1990, true));

        mockMvc.perform(put("/api/training-profile")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthYear").value(1990))
                .andExpect(jsonPath("$.adaptiveCoachingEligible").value(true));

        verify(saveUseCase).execute("user@sudolife.com", command);
    }

    @Test
    void put_returns_bad_request_when_training_profile_is_invalid() throws Exception {
        SaveTrainingProfileCommand command = new SaveTrainingProfileCommand(null);
        when(saveUseCase.execute("user@sudolife.com", command))
                .thenThrow(new InvalidTrainingProfileException("Birth year is required"));

        mockMvc.perform(put("/api/training-profile")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRAINING_PROFILE"))
                .andExpect(jsonPath("$.message").value("Birth year is required"));
    }
}
