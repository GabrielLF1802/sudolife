package com.sudolife.adapter.driving.rest.training;

import com.sudolife.application.service.training.CoachingProfileResult;
import com.sudolife.application.service.training.SaveCoachingProfileCommand;
import com.sudolife.application.service.training.ports.provided.GetCoachingProfileUseCase;
import com.sudolife.application.service.training.ports.provided.SaveCoachingProfileUseCase;
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
@RequestMapping("/api/coaching-profiles")
public class CoachingProfileController {

    private final GetCoachingProfileUseCase getCoachingProfileUseCase;
    private final SaveCoachingProfileUseCase saveCoachingProfileUseCase;

    @GetMapping
    public ResponseEntity<CoachingProfileResult> get(Authentication authentication) {
        CoachingProfileResult result = getCoachingProfileUseCase.execute(authentication.getName());

        return ResponseEntity.ok(result);
    }

    @PutMapping
    public ResponseEntity<CoachingProfileResult> save(
            Authentication authentication,
            @RequestBody SaveCoachingProfileCommand command
    ) {
        CoachingProfileResult result = saveCoachingProfileUseCase.execute(authentication.getName(), command);

        return ResponseEntity.ok(result);
    }
}
