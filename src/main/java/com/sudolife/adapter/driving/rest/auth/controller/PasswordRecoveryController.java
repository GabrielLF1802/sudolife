package com.sudolife.adapter.driving.rest.auth.controller;

import com.sudolife.adapter.driving.rest.ratelimit.HttpRequestOriginResolver;
import com.sudolife.adapter.driving.rest.ratelimit.PasswordRecoveryRateLimitPolicy;
import com.sudolife.application.service.user.CompletePasswordRecoveryCommand;
import com.sudolife.application.service.user.PasswordRecoveryStartResult;
import com.sudolife.application.service.user.StartPasswordRecoveryCommand;
import com.sudolife.application.service.user.ports.provided.CompletePasswordRecoveryUseCase;
import com.sudolife.application.service.user.ports.provided.StartPasswordRecoveryUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private final StartPasswordRecoveryUseCase startPasswordRecoveryUseCase;
    private final CompletePasswordRecoveryUseCase completePasswordRecoveryUseCase;
    private final PasswordRecoveryRateLimitPolicy passwordRecoveryRateLimitPolicy;
    private final HttpRequestOriginResolver originResolver;

    @PostMapping("/password-recovery")
    public ResponseEntity<PasswordRecoveryStartResult> start(
            @RequestBody StartPasswordRecoveryCommand command,
            HttpServletRequest request
    ) {
        passwordRecoveryRateLimitPolicy.consumeStartAttempt(command, originResolver.resolveOrigin(request));

        return ResponseEntity.ok(startPasswordRecoveryUseCase.execute(command));
    }

    @PostMapping("/password-recovery/complete")
    public ResponseEntity<Void> complete(
            @RequestBody CompletePasswordRecoveryCommand command,
            HttpServletRequest request
    ) {
        passwordRecoveryRateLimitPolicy.consumeCompleteAttempt(originResolver.resolveOrigin(request));
        completePasswordRecoveryUseCase.execute(command);

        return ResponseEntity.noContent().build();
    }
}
