package com.sudolife.application.service.training;

import com.sudolife.application.service.training.ports.provided.GetTrainingProfileUseCase;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetTrainingProfileUseCaseImpl implements GetTrainingProfileUseCase {

    private final TrainingProfileRepository repository;

    @Override
    public TrainingProfileResult execute(String userEmail) {
        return repository.findByUserEmail(userEmail)
                .map(profile -> TrainingProfileResult.existing(profile.getBirthYear()))
                .orElseGet(TrainingProfileResult::missing);
    }
}
