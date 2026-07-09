package com.sudolife.adapter.driving.scheduler.strava.sync;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.sync.EnqueueStravaSummarySyncResult;
import com.sudolife.application.service.strava.ports.provided.EnqueueStravaSummarySyncUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static com.sudolife.helper.StravaTestHelper.reconnectRequiredStravaAccountLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StravaSummarySyncSchedulerUnitTest {

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private EnqueueStravaSummarySyncUseCase enqueueStravaSummarySyncUseCase;

    @InjectMocks
    private StravaSummarySyncScheduler scheduler;

    @Test
    void enqueueScheduledSummarySyncJobs_only_enqueues_permission_ready_links() {
        when(accountLinkRepository.findAllActive()).thenReturn(List.of(activeStravaAccountLink(), readOnlyLink(),
                reconnectRequiredStravaAccountLink()));
        when(enqueueStravaSummarySyncUseCase.execute(any())).thenReturn(new EnqueueStravaSummarySyncResult(true));

        scheduler.enqueueScheduledSummarySyncJobs();

        ArgumentCaptor<com.sudolife.application.service.strava.sync.EnqueueStravaSummarySyncCommand> captor =
                ArgumentCaptor.forClass(com.sudolife.application.service.strava.sync.EnqueueStravaSummarySyncCommand.class);
        verify(enqueueStravaSummarySyncUseCase).execute(captor.capture());
        assertThat(captor.getValue().accountLinkId()).isEqualTo(LINK_ID);
    }

    private StravaAccountLink readOnlyLink() {
        return StravaAccountLink.active(LINK_ID + 1, USER_EMAIL, ATHLETE_ID + 1, ACCESS_TOKEN, REFRESH_TOKEN,
                EXPIRES_AT, "read", LINKED_AT);
    }
}
