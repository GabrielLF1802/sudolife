package com.sudolife.adapter.driven.persistence.strava.repository;

import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaActivityDetailSnapshotRepository;
import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaActivityStreamSnapshotRepository;
import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaActivityStreamSyncJobRepository;
import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaActivitySummaryRepository;
import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaSummarySyncJobRepository;
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
