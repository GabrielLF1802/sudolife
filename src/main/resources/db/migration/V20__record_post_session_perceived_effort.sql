alter table adaptive_running_plan_sessions
    add column post_session_perceived_effort integer;

alter table adaptive_running_plan_sessions
    add constraint ck_adaptive_running_plan_sessions_perceived_effort check (
        post_session_perceived_effort between 1 and 10
    );
