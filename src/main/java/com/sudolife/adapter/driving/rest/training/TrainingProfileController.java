package com.sudolife.adapter.driving.rest.training;

import com.sudolife.application.service.training.SaveTrainingProfileCommand;
import com.sudolife.application.service.training.TrainingProfileResult;
import com.sudolife.application.service.training.ports.provided.GetTrainingProfileUseCase;
import com.sudolife.application.service.training.ports.provided.SaveTrainingProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/training-profile")
public class TrainingProfileController {

    private final GetTrainingProfileUseCase getTrainingProfileUseCase;
    private final SaveTrainingProfileUseCase saveTrainingProfileUseCase;

    @GetMapping
    public ResponseEntity<TrainingProfileResult> get(Authentication authentication) {
        TrainingProfileResult result = getTrainingProfileUseCase.execute(authentication.getName());

        return ResponseEntity.ok(result);
    }

    @PutMapping
    public ResponseEntity<TrainingProfileResult> save(
            Authentication authentication,
            @RequestBody SaveTrainingProfileCommand command
    ) {
        TrainingProfileResult result = saveTrainingProfileUseCase.execute(authentication.getName(), command);

        return ResponseEntity.ok(result);
    }
}
