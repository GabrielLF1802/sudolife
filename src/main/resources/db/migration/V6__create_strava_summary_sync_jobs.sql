create table strava_summary_sync_jobs (
    id bigserial primary key,
    account_link_id bigint not null,
    open_account_link_id bigint,
    user_email varchar(255) not null,
    status varchar(32) not null,
    attempt_count integer not null,
    imported_activity_count integer not null,
    run_after timestamp with time zone not null,
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    failure_reason varchar(64),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_strava_summary_sync_jobs_account_link foreign key (account_link_id) references strava_account_links(id),
    constraint uk_strava_summary_sync_jobs_open_account_link unique (open_account_link_id)
);

create index ix_strava_summary_sync_jobs_runnable
    on strava_summary_sync_jobs (status, run_after, created_at);

create index ix_strava_summary_sync_jobs_account_link
    on strava_summary_sync_jobs (account_link_id);
