package com.sudolife.application.model.training;

import lombok.Getter;

import java.time.DayOfWeek;
import java.util.List;

@Getter
public class RunningAvailability {

    private final List<DayOfWeek> preferredRunningDays;

    public RunningAvailability(List<DayOfWeek> preferredRunningDays) {
        if (preferredRunningDays == null) {
            this.preferredRunningDays = List.of();
            return;
        }

        if (preferredRunningDays.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Preferred running days cannot contain null values");
        }

        if (preferredRunningDays.stream().distinct().count() != preferredRunningDays.size()) {
            throw new IllegalArgumentException("Preferred running days cannot contain duplicates");
        }

        this.preferredRunningDays = preferredRunningDays.stream().sorted().toList();
    }

    public boolean isConfigured() {
        return !preferredRunningDays.isEmpty();
    }
}
