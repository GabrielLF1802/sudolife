package com.sudolife.adapter.driving.rest.auth.controller;

import com.sudolife.application.service.user.PasswordRecoveryStartResult;
import com.sudolife.application.service.user.StartPasswordRecoveryCommand;
import com.sudolife.application.service.user.ports.provided.StartPasswordRecoveryUseCase;
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

import static com.sudolife.helper.UserTestHelper.EMAIL;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PasswordRecoveryController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class PasswordRecoveryControllerWebMvcTest {

    private static final String GENERIC_MESSAGE = "Se uma conta existir para esse email, instruções de recuperação de senha serão enviadas.";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    StartPasswordRecoveryUseCase startPasswordRecoveryUseCase;

    @Test
    void start_returns_generic_success_response() throws Exception {
        StartPasswordRecoveryCommand command = new StartPasswordRecoveryCommand(EMAIL);
        when(startPasswordRecoveryUseCase.execute(command))
                .thenReturn(new PasswordRecoveryStartResult(GENERIC_MESSAGE));

        mockMvc.perform(post("/api/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }
}
