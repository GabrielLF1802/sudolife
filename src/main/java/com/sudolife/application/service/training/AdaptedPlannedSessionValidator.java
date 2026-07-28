package com.sudolife.application.service.training;

import com.sudolife.application.service.training.exception.UnsafeAdaptedPlannedSessionException;
import org.springframework.stereotype.Service;

@Service
public class AdaptedPlannedSessionValidator {

    private static final double MAXIMUM_DISTANCE_MULTIPLIER = 1.10;

    public void validate(PlannedSessionResult original, PlannedSessionResult replacement) {
        boolean identityChanged = original.weekNumber() != replacement.weekNumber()
                || original.sessionNumber() != replacement.sessionNumber()
                || !original.scheduledDate().equals(replacement.scheduledDate());
        boolean unsafeDistance = replacement.distanceKilometers() < 0
                || replacement.distanceKilometers() > original.distanceKilometers() * MAXIMUM_DISTANCE_MULTIPLIER;

        if (identityChanged || unsafeDistance || replacement.target() == null) {
            throw new UnsafeAdaptedPlannedSessionException();
        }
    }
}
