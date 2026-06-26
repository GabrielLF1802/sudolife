alter table strava_account_links add column granted_scopes varchar(1024);

update strava_account_links
set granted_scopes = 'read'
where active = true;

alter table strava_account_links drop constraint ck_strava_account_links_active_authorization;

alter table strava_account_links add constraint ck_strava_account_links_active_authorization check (
    (active = true and active_athlete_id = athlete_id and active_user_email = user_email and access_token is not null and refresh_token is not null and expires_at is not null and granted_scopes is not null and unlinked_at is null)
    or
    (active = false and active_athlete_id is null and active_user_email is null and access_token is null and refresh_token is null and expires_at is null and granted_scopes is null and unlinked_at is not null)
);
