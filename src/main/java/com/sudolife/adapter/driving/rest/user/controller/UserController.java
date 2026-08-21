package com.sudolife.adapter.driving.rest.user.controller;

import com.sudolife.adapter.driving.rest.ratelimit.HttpRequestOriginResolver;
import com.sudolife.adapter.driving.rest.ratelimit.RegistrationRateLimitPolicy;
import com.sudolife.adapter.driving.rest.user.webmodel.ChangePasswordRequest;
import com.sudolife.adapter.driving.rest.user.webmodel.DeleteAccountRequest;
import com.sudolife.application.service.user.ChangePasswordCommand;
import com.sudolife.application.service.user.CurrentUserResult;
import com.sudolife.application.service.user.DeleteAccountCommand;
import com.sudolife.application.service.user.RegisterUserCommand;
import com.sudolife.application.service.user.ports.provided.ChangePasswordUseCase;
import com.sudolife.application.service.user.ports.provided.DeleteAccountUseCase;
import com.sudolife.application.service.user.ports.provided.GetCurrentUserUseCase;
import com.sudolife.application.service.user.ports.provided.RegisterUserUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final RegistrationRateLimitPolicy registrationRateLimitPolicy;
    private final HttpRequestOriginResolver originResolver;

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@RequestBody RegisterUserCommand command, HttpServletRequest request) {
        String origin = originResolver.resolveOrigin(request);
        registrationRateLimitPolicy.consumeRegistrationAttempt(command, origin);
        registerUserUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResult> getCurrentUser(Authentication authentication) {
        CurrentUserResult result = getCurrentUserUseCase.execute(authentication.getName());

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication authentication, @RequestBody ChangePasswordRequest request) {
        ChangePasswordCommand command = new ChangePasswordCommand(
                authentication.getName(),
                request.currentPassword(),
                request.newPassword()
        );
        changePasswordUseCase.execute(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(Authentication authentication, @RequestBody DeleteAccountRequest request) {
        DeleteAccountCommand command = new DeleteAccountCommand(authentication.getName(), request.currentPassword());
        deleteAccountUseCase.execute(command);

        return ResponseEntity.noContent().build();
    }
}
