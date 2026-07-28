alter table adaptive_running_plan_sessions
    add column adaptation_trigger varchar(64);

alter table adaptive_running_plan_sessions
    add constraint ck_adaptive_running_plan_sessions_trigger check (adaptation_trigger in (
        'MISSED_PLANNED_SESSION',
        'COMPLETED_PLANNED_SESSION',
        'INJURY_CONCERN',
        'LOW_READINESS',
        'UNEXPECTEDLY_HIGH_EFFORT',
        'UNEXPECTEDLY_LOW_EFFORT'
    ));
