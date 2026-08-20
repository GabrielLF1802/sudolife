package com.sudolife.adapter.driving.rest.auth.controller;

import com.sudolife.adapter.driving.rest.ratelimit.HttpRequestOriginResolver;
import com.sudolife.adapter.driving.rest.ratelimit.LoginRateLimitPolicy;
import com.sudolife.application.service.user.AuthenticateUserCommand;
import com.sudolife.application.service.user.AuthenticationResult;
import com.sudolife.application.service.user.exception.InvalidCredentialsException;
import com.sudolife.application.service.user.ports.provided.AuthenticateUserUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class AuthenticationController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final LoginRateLimitPolicy loginRateLimitPolicy;
    private final HttpRequestOriginResolver originResolver;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResult> login(
            @RequestBody AuthenticateUserCommand command,
            HttpServletRequest request
    ) {
        String origin = originResolver.resolveOrigin(request);
        loginRateLimitPolicy.consumePreAuthenticationOrigin(origin);
        loginRateLimitPolicy.assertFailedAttemptAllowed(command, origin);
        AuthenticationResult result = authenticate(command, origin);

        return ResponseEntity.ok(result);
    }

    private AuthenticationResult authenticate(AuthenticateUserCommand command, String origin) {
        try {
            AuthenticationResult result = authenticateUserUseCase.execute(command);
            loginRateLimitPolicy.clearFailedAttempts(command, origin);

            return result;
        } catch (InvalidCredentialsException exception) {
            loginRateLimitPolicy.consumeFailedAttempt(command, origin);
            throw exception;
        }
    }
}
