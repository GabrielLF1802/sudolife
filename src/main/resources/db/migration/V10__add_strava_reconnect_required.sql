alter table strava_account_links
    add column reconnect_required boolean not null default false;

alter table strava_account_links
    add constraint ck_strava_account_links_reconnect_required check (
        active = true or reconnect_required = false
    );
