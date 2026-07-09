package com.sudolife.application.model.training;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TrainingProfile {

    private Long id;
    private String userEmail;
    private int birthYear;

    public TrainingProfile(Long id, String userEmail, int birthYear) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("User email is invalid, null or empty");
        }

        this.id = id;
        this.userEmail = userEmail;
        this.birthYear = birthYear;
    }
}
