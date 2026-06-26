package com.sudolife.application.service.strava;

import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static com.sudolife.helper.StravaTestHelper.getStravaAccountLinkStatusCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStravaAccountLinkStatusUseCaseImplUnitTest {

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @InjectMocks
    private GetStravaAccountLinkStatusUseCaseImpl useCase;

    @Test
    void execute_with_active_link_returns_linked_status_with_athlete_id() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.linked()).isTrue();
        assertThat(result.athleteId()).isEqualTo(ATHLETE_ID);
        assertThat(result.permissionState()).isEqualTo(StravaPermissionState.READY);
    }

    @Test
    void execute_with_read_only_link_returns_permission_upgrade_required() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(readOnlyLink()));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.linked()).isTrue();
        assertThat(result.athleteId()).isEqualTo(ATHLETE_ID);
        assertThat(result.permissionState()).isEqualTo(StravaPermissionState.PERMISSION_UPGRADE_REQUIRED);
    }

    @Test
    void execute_without_active_link_returns_unlinked_status_without_athlete_id() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.empty());

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.athleteId()).isNull();
        assertThat(result.permissionState()).isEqualTo(StravaPermissionState.UNLINKED);
    }

    @Test
    void result_does_not_include_token_fields() {
        String[] componentNames = Arrays.stream(StravaLinkStatusResult.class.getRecordComponents())
                .map(component -> component.getName())
                .toArray(String[]::new);

        assertThat(componentNames).containsExactly("linked", "athleteId", "permissionState");
    }

    private com.sudolife.application.model.strava.StravaAccountLink readOnlyLink() {
        return com.sudolife.application.model.strava.StravaAccountLink.active(LINK_ID, USER_EMAIL, ATHLETE_ID,
                ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT, "read", LINKED_AT);
    }
}
