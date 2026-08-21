package com.sudolife.application.service.user;

import com.sudolife.application.model.user.User;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.user.exception.AuthenticatedUserNotFoundException;
import com.sudolife.application.service.user.exception.InvalidCredentialsException;
import com.sudolife.application.service.user.ports.provided.DeleteAccountUseCase;
import com.sudolife.application.service.user.ports.required.AccountDeletionDataRepository;
import com.sudolife.application.service.user.ports.required.UserHashPassword;
import com.sudolife.application.service.user.ports.required.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteAccountUseCaseImpl implements DeleteAccountUseCase {

    private final UserRepository userRepository;
    private final UserHashPassword userHashPassword;
    private final AccountDeletionDataRepository accountDeletionDataRepository;
    private final StravaOAuthProvider stravaOAuthProvider;
    private final TimeProvider timeProvider;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void execute(DeleteAccountCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        if (!userHashPassword.matches(command.currentPassword(), user.getPassword().value())) {
            throw new InvalidCredentialsException();
        }

        List<StravaDeauthorization> deauthorizations = accountDeletionDataRepository
                .findStravaDeauthorizations(user.getEmail().value());

        transactionTemplate.executeWithoutResult(status -> {
            accountDeletionDataRepository.deleteAccountOwnedData(user.getEmail().value(), timeProvider.now());
            userRepository.deleteByEmail(user.getEmail().value());
        });

        deauthorizeStrava(deauthorizations);
    }

    private void deauthorizeStrava(List<StravaDeauthorization> deauthorizations) {
        Instant now = timeProvider.now();

        deauthorizations.forEach(deauthorization -> deauthorize(deauthorization, now));
    }

    private void deauthorize(StravaDeauthorization deauthorization, Instant now) {
        String accessToken = accessTokenFor(deauthorization, now);

        if (!hasText(accessToken)) {
            return;
        }

        try {
            stravaOAuthProvider.deauthorize(accessToken);
        } catch (RuntimeException exception) {
            log.warn("Strava deauthorization failed during account deletion athleteId={}", deauthorization.athleteId());
        }
    }

    private String accessTokenFor(StravaDeauthorization deauthorization, Instant now) {
        if (deauthorization.hasUsableAccessToken(now)) {
            return deauthorization.accessToken();
        }

        if (!deauthorization.hasRefreshToken()) {
            return null;
        }

        try {
            return stravaOAuthProvider.refresh(deauthorization.refreshToken()).accessToken();
        } catch (RuntimeException exception) {
            log.warn("Strava token refresh failed during account deletion athleteId={}", deauthorization.athleteId());

            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
