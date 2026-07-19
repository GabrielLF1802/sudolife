create table adaptive_running_plans (
    id bigserial primary key,
    user_email varchar(255) not null,
    safe_milestone_distance_kilometers double precision not null,
    safe_milestone_pace_seconds_per_kilometer integer,
    safe_milestone_target_date date,
    explanation text not null,
    accepted_at timestamp with time zone not null
);

create index ix_adaptive_running_plans_user_accepted
    on adaptive_running_plans(user_email, accepted_at desc);

create table adaptive_running_plan_sessions (
    id bigserial primary key,
    plan_id bigint not null,
    original_planned_session_id bigint,
    week_number integer not null,
    session_number integer not null,
    session_type varchar(32) not null,
    distance_kilometers double precision not null,
    target_type varchar(32) not null,
    minimum_heart_rate integer,
    maximum_heart_rate integer,
    minimum_perceived_effort integer,
    maximum_perceived_effort integer,
    scheduled_date date not null,
    status varchar(16) not null,
    constraint fk_adaptive_running_plan_sessions_plan foreign key (plan_id) references adaptive_running_plans(id),
    constraint fk_adaptive_running_plan_sessions_original foreign key (original_planned_session_id) references adaptive_running_plan_sessions(id),
    constraint ck_adaptive_running_plan_sessions_status check (status in ('PLANNED', 'REPLACED', 'COMPLETED', 'MISSED'))
);

create index ix_adaptive_running_plan_sessions_plan on adaptive_running_plan_sessions(plan_id);
