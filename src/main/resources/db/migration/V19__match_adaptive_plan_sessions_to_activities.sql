alter table adaptive_running_plan_sessions
    add column duration_seconds integer;

alter table adaptive_running_plan_sessions
    add column matched_activity_id bigint;

alter table adaptive_running_plan_sessions
    add constraint uq_adaptive_running_plan_sessions_matched_activity unique (matched_activity_id);
