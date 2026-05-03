package com.sudolife.adapter.driving.rest.user.controller;

import com.sudolife.application.service.user.CurrentUserResult;
import com.sudolife.application.service.user.RegisterUserCommand;
import com.sudolife.application.service.user.ports.provided.GetCurrentUserUseCase;
import com.sudolife.application.service.user.ports.provided.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@RequestBody RegisterUserCommand command) {
        registerUserUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResult> getCurrentUser(Authentication authentication) {
        CurrentUserResult result = getCurrentUserUseCase.execute(authentication.getName());

        return ResponseEntity.ok(result);
    }
}
