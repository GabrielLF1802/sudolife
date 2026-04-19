package com.sudolife.adapter.driving.rest.auth.controller;

import com.sudolife.adapter.driving.rest.auth.webmodel.LoginRequest;
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
    public ResponseEntity<String> login(@RequestBody LoginRequest login) {
        String authToken = authenticateUserUseCase.login(login.getEmail(), login.getPassword());

        return ResponseEntity.ok(authToken);
    }
}
