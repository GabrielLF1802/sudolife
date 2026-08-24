package com.sudolife.adapter.driving.rest.auth.controller;

import com.sudolife.application.service.user.CompletePasswordRecoveryCommand;
import com.sudolife.application.service.user.PasswordRecoveryStartResult;
import com.sudolife.application.service.user.StartPasswordRecoveryCommand;
import com.sudolife.application.service.user.ports.provided.CompletePasswordRecoveryUseCase;
import com.sudolife.application.service.user.ports.provided.StartPasswordRecoveryUseCase;
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

    @PostMapping("/password-recovery")
    public ResponseEntity<PasswordRecoveryStartResult> start(@RequestBody StartPasswordRecoveryCommand command) {
        return ResponseEntity.ok(startPasswordRecoveryUseCase.execute(command));
    }

    @PostMapping("/password-recovery/complete")
    public ResponseEntity<Void> complete(@RequestBody CompletePasswordRecoveryCommand command) {
        completePasswordRecoveryUseCase.execute(command);

        return ResponseEntity.noContent().build();
    }
}
