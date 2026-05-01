package com.sudolife.adapter.driving.rest.user.controller;

import com.sudolife.application.service.user.RegisterUserCommand;
import com.sudolife.application.service.user.ports.provided.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@RequestBody RegisterUserCommand command) {
        registerUserUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
