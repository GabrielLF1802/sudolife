package com.sudolife.adapter.driving.rest.auth.controller;

import com.sudolife.adapter.driving.rest.ratelimit.HttpRequestOriginResolver;
import com.sudolife.application.service.ratelimit.exception.LoginRateLimitExceededException;
import com.sudolife.adapter.driving.rest.ratelimit.LoginRateLimitPolicy;
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

    private static final String ORIGIN = "203.0.113.10";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthenticateUserUseCase authenticateUserUseCase;

    @MockitoBean
    LoginRateLimitPolicy loginRateLimitPolicy;

    @MockitoBean
    HttpRequestOriginResolver originResolver;

    @Test
    void authenticateUser_returns_authenticated_when_user_is_valid() throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(EMAIL, PASSWORD);
        when(originResolver.resolveOrigin(any())).thenReturn(ORIGIN);
        when(authenticateUserUseCase.execute(command)).thenReturn(new AuthenticationResult(TOKEN));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(TOKEN));

        verify(loginRateLimitPolicy).consumePreAuthenticationOrigin(ORIGIN);
        verify(loginRateLimitPolicy).assertFailedAttemptAllowed(command, ORIGIN);
        verify(loginRateLimitPolicy).clearFailedAttempts(command, ORIGIN);
    }

    @Test
    void authenticateUser_returns_unauthorized_when_user_is_invalid() throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(EMAIL, PASSWORD);
        when(originResolver.resolveOrigin(any())).thenReturn(ORIGIN);
        doThrow(new InvalidCredentialsException())
                .when(authenticateUserUseCase)
                .execute(command);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(authenticateUserUseCase).execute(command);
        verify(loginRateLimitPolicy).consumeFailedAttempt(command, ORIGIN);
    }

    @Test
    void authenticateUser_returns_too_many_requests_when_login_rate_limit_is_exceeded() throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(EMAIL, PASSWORD);
        when(originResolver.resolveOrigin(any())).thenReturn(ORIGIN);
        doThrow(new LoginRateLimitExceededException()).when(loginRateLimitPolicy)
                .consumePreAuthenticationOrigin(ORIGIN);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("Login rate limit exceeded"));

        verifyNoInteractions(authenticateUserUseCase);
    }
}
