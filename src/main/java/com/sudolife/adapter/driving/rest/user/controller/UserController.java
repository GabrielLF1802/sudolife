package com.sudolife.adapter.driving.rest.user.controller;

import com.sudolife.adapter.driving.rest.user.UserWebMapper;
import com.sudolife.adapter.driving.rest.user.webmodel.UserRequest;
import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.ports.provided.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/register")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final UserWebMapper mapper;

    public ResponseEntity<Void> registerUser(@RequestBody UserRequest request) {
        User user = mapper.toDomain(request);
        registerUserUseCase.execute(user);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
