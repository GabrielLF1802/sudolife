package com.sudolife.adapter.driving.rest.training;

import com.sudolife.application.service.training.CoachingProfileResult;
import com.sudolife.application.service.training.AdaptiveRunningPlanResult;
import com.sudolife.application.service.training.ConservativeRunningPlanResult;
import com.sudolife.application.service.training.SaveCoachingProfileCommand;
import com.sudolife.application.service.training.ports.provided.GenerateConservativeRunningPlanUseCase;
import com.sudolife.application.service.training.ports.provided.GenerateAdaptiveRunningPlanUseCase;
import com.sudolife.application.service.training.ports.provided.GetCoachingProfileUseCase;
import com.sudolife.application.service.training.ports.provided.SaveCoachingProfileUseCase;
import com.sudolife.application.service.training.RunningHistorySnapshotResult;
import com.sudolife.application.service.training.RunningGoalAssessmentResult;
import com.sudolife.application.service.training.ports.provided.EvaluateRunningGoalUseCase;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coaching-profiles")
public class CoachingProfileController {

    private final GetCoachingProfileUseCase getCoachingProfileUseCase;
    private final SaveCoachingProfileUseCase saveCoachingProfileUseCase;
    private final GetRunningHistorySnapshotUseCase getRunningHistorySnapshotUseCase;
    private final GenerateConservativeRunningPlanUseCase generateConservativeRunningPlanUseCase;
    private final GenerateAdaptiveRunningPlanUseCase generateAdaptiveRunningPlanUseCase;
    private final EvaluateRunningGoalUseCase evaluateRunningGoalUseCase;

    @GetMapping("/running-goal-assessment")
    public ResponseEntity<RunningGoalAssessmentResult> evaluateRunningGoal(Authentication authentication) {
        return ResponseEntity.ok(evaluateRunningGoalUseCase.execute(authentication.getName()));
    }

    @PostMapping("/running-plan")
    public ResponseEntity<ConservativeRunningPlanResult> generateRunningPlan(Authentication authentication) {
        return ResponseEntity.ok(generateConservativeRunningPlanUseCase.execute(authentication.getName()));
    }

    @PostMapping("/adaptive-running-plan")
    public ResponseEntity<AdaptiveRunningPlanResult> generateAdaptiveRunningPlan(Authentication authentication) {
        return ResponseEntity.ok(generateAdaptiveRunningPlanUseCase.execute(authentication.getName()));
    }

    @GetMapping("/running-history")
    public ResponseEntity<RunningHistorySnapshotResult> getRunningHistory(Authentication authentication) {
        return ResponseEntity.ok(getRunningHistorySnapshotUseCase.execute(authentication.getName()));
    }

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
