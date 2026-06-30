package com.sudolife.application.service.strava;

import com.sudolife.application.service.strava.ports.provided.ListStravaActivitiesUseCase;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListStravaActivitiesUseCaseImpl implements ListStravaActivitiesUseCase {

    private final StravaActivitySummaryRepository activitySummaryRepository;
    private final StravaActivityStreamSnapshotRepository streamSnapshotRepository;
    private final StravaActivityListMapper mapper;

    @Override
    public StravaActivityListResult execute(ListStravaActivitiesCommand command) {
        StravaActivitySummaryPage page = activitySummaryRepository.findByUserEmail(command.userEmail(),
                command.page(), command.size());
        List<StravaActivityListItemResult> activities = page.activities().stream()
                .map(activity -> mapper.toResult(activity, streamSnapshotRepository.findByActivitySummaryId(
                        activity.getId()).isPresent()))
                .toList();

        return new StravaActivityListResult(activities, page.page(), page.size(), page.totalElements(),
                page.totalPages());
    }
}
