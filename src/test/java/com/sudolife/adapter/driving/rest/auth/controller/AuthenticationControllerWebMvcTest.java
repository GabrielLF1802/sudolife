package com.sudolife.adapter.driving.rest.auth.controller;

import com.sudolife.application.service.user.AuthenticateUserCommand;
import com.sudolife.application.service.user.AuthenticationResult;
import com.sudolife.application.service.user.exception.InvalidCredentialsException;
import com.sudolife.application.service.user.ports.provided.AuthenticateUserUseCase;
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

import static com.sudolife.helper.UserTestHelper.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuthenticationController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
public class AuthenticationControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthenticateUserUseCase authenticateUserUseCase;

    @Test
    void authenticateUser_returns_authenticated_when_user_is_valid() throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(EMAIL, PASSWORD);

        when(authenticateUserUseCase.execute(command))
                .thenReturn(new AuthenticationResult(TOKEN));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(TOKEN));
    }

    @Test
    void authenticateUser_returns_unauthorized_when_user_is_invalid() throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(EMAIL, PASSWORD);
        doThrow(new InvalidCredentialsException())
                .when(authenticateUserUseCase)
                .execute(command);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isUnauthorized());

        verify(authenticateUserUseCase).execute(command);
    }
}
