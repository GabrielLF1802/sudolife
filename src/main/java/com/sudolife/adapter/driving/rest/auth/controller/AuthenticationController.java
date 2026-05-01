package com.sudolife.adapter.driving.rest.auth.controller;

import com.sudolife.application.service.user.AuthenticateUserCommand;
import com.sudolife.application.service.user.AuthenticationResult;
import com.sudolife.application.service.user.ports.provided.AuthenticateUserUseCase;
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

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResult> login(@RequestBody AuthenticateUserCommand command) {
        AuthenticationResult result = authenticateUserUseCase.execute(command);

        return ResponseEntity.ok(result);
    }
}
