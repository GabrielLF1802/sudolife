package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.activity.SpringDataStravaActivityDetailSnapshotRepository;
import com.sudolife.adapter.driven.persistence.strava.activity.SpringDataStravaActivityStreamSnapshotRepository;
import com.sudolife.adapter.driven.persistence.strava.sync.SpringDataStravaActivityStreamSyncJobRepository;
import com.sudolife.adapter.driven.persistence.strava.activity.SpringDataStravaActivitySummaryRepository;
import com.sudolife.adapter.driven.persistence.strava.sync.SpringDataStravaSummarySyncJobRepository;
import com.sudolife.application.service.strava.ports.required.StravaImportedDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StravaImportedDataRepositoryJpaAdapter implements StravaImportedDataRepository {

    private final SpringDataStravaActivityStreamSyncJobRepository streamSyncJobRepository;
    private final SpringDataStravaSummarySyncJobRepository summarySyncJobRepository;
    private final SpringDataStravaActivityStreamSnapshotRepository streamSnapshotRepository;
    private final SpringDataStravaActivityDetailSnapshotRepository detailSnapshotRepository;
    private final SpringDataStravaActivitySummaryRepository activitySummaryRepository;

    @Override
    public void deleteByAccountLinkId(Long accountLinkId) {
        streamSyncJobRepository.deleteByAccountLinkId(accountLinkId);
        summarySyncJobRepository.deleteByAccountLinkId(accountLinkId);
        streamSnapshotRepository.deleteByAccountLinkId(accountLinkId);
        detailSnapshotRepository.deleteByAccountLinkId(accountLinkId);
        activitySummaryRepository.deleteByAccountLinkId(accountLinkId);
    }
}
