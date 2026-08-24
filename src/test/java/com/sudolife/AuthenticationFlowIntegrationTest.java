package com.sudolife;

import com.sudolife.adapter.driving.rest.user.webmodel.ChangePasswordRequest;
import com.sudolife.adapter.driving.rest.user.webmodel.DeleteAccountRequest;
import com.sudolife.application.service.user.AuthenticateUserCommand;
import com.sudolife.application.service.user.RegisterUserCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.sudolife.helper.UserTestHelper.EMAIL;
import static com.sudolife.helper.UserTestHelper.NAME;
import static com.sudolife.helper.UserTestHelper.NEW_PASSWORD;
import static com.sudolife.helper.UserTestHelper.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthenticationFlowIntegrationTest.ProtectedController.class)
class AuthenticationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from strava_activity_stream_sync_jobs");
        jdbcTemplate.update("delete from strava_summary_sync_jobs");
        jdbcTemplate.update("delete from strava_activity_stream_snapshots");
        jdbcTemplate.update("delete from strava_activity_detail_snapshots");
        jdbcTemplate.update("delete from strava_activity_summaries");
        jdbcTemplate.update("delete from strava_authorization_states");
        jdbcTemplate.update("delete from strava_account_links");
        jdbcTemplate.update("delete from strava_data_consent_records");
        jdbcTemplate.update("delete from adaptive_running_plan_sessions");
        jdbcTemplate.update("delete from adaptive_running_plans");
        jdbcTemplate.update("delete from coaching_profiles");
        jdbcTemplate.update("delete from training_profiles");
        jdbcTemplate.update("delete from password_recovery_tokens");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void register_login_and_access_protected_endpoint() throws Exception {
        registerUser();
        String token = login();

        String response = mockMvc.perform(get("/api/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).isEqualTo("ok");
    }

    @Test
    void public_api_response_includes_security_headers() throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, PASSWORD);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("Permissions-Policy",
                        "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void authenticated_api_response_includes_security_headers() throws Exception {
        registerUser();
        String token = login();

        mockMvc.perform(get("/api/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("Permissions-Policy",
                        "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void protected_endpoint_rejects_request_without_token() throws Exception {
        int status = mockMvc.perform(get("/api/protected"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void password_change_rejects_request_without_token() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, NEW_PASSWORD);
        int status = mockMvc.perform(patch("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void password_change_rejects_wrong_current_password() throws Exception {
        registerUser();
        String token = login(PASSWORD);
        ChangePasswordRequest request = new ChangePasswordRequest("wrong-password", NEW_PASSWORD);

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void password_change_rejects_weak_new_password_with_policy_details() throws Exception {
        registerUser();
        String token = login(PASSWORD);
        ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, "weak");

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_POLICY_VIOLATION"))
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void password_change_rejects_reusing_current_password() throws Exception {
        registerUser();
        String token = login(PASSWORD);
        ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, PASSWORD);

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NEW_PASSWORD_MATCHES_CURRENT_PASSWORD"));
    }

    @Test
    void password_change_updates_password_used_for_login() throws Exception {
        registerUser();
        String token = login(PASSWORD);
        ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, NEW_PASSWORD);

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        loginFails(PASSWORD);
        login(NEW_PASSWORD);
    }

    @Test
    void account_deletion_rejects_request_without_token() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest(PASSWORD);
        int status = mockMvc.perform(delete("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void account_deletion_rejects_wrong_current_password() throws Exception {
        registerUser();
        String token = login(PASSWORD);
        DeleteAccountRequest request = new DeleteAccountRequest("wrong-password");

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void account_deletion_deletes_user_and_allows_same_email_registration() throws Exception {
        registerUser();
        String token = login(PASSWORD);
        DeleteAccountRequest request = new DeleteAccountRequest(PASSWORD);

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("select count(*) from users where email = ?", Long.class, EMAIL))
                .isZero();
        registerUser();
    }

    @Test
    void account_deletion_deletes_account_owned_data() throws Exception {
        registerUser();
        seedAccountOwnedData();
        String token = login(PASSWORD);
        DeleteAccountRequest request = new DeleteAccountRequest(PASSWORD);

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        assertThat(countRows("training_profiles")).isZero();
        assertThat(countRows("coaching_profiles")).isZero();
        assertThat(countRows("adaptive_running_plans")).isZero();
        assertThat(countRows("adaptive_running_plan_sessions")).isZero();
        assertThat(countRows("strava_authorization_states")).isZero();
        assertThat(countRows("strava_account_links")).isZero();
        assertThat(countRows("strava_activity_summaries")).isZero();
        assertThat(countRows("strava_activity_detail_snapshots")).isZero();
        assertThat(countRows("strava_activity_stream_snapshots")).isZero();
        assertThat(countRows("strava_summary_sync_jobs")).isZero();
        assertThat(countRows("strava_activity_stream_sync_jobs")).isZero();
        assertThat(countRows("strava_data_consent_records")).isZero();
    }

    private void registerUser() throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, PASSWORD);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated());
    }

    private String login() throws Exception {
        return login(PASSWORD);
    }

    private String login(String password) throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(EMAIL, password);

        String response = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        return body.get("token").asText();
    }

    private void loginFails(String password) throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(EMAIL, password);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isUnauthorized());
    }

    private void seedAccountOwnedData() {
        jdbcTemplate.update("insert into training_profiles (user_email) values (?)", EMAIL);
        jdbcTemplate.update("""
                insert into coaching_profiles (
                    user_email,
                    target_distance_kilometers,
                    readiness,
                    injury_concern
                ) values (?, 5.0, 'READY', false)
                """, EMAIL);
        jdbcTemplate.update("""
                insert into adaptive_running_plans (
                    id,
                    user_email,
                    safe_milestone_distance_kilometers,
                    explanation,
                    accepted_at
                ) values (100, ?, 5.0, 'Plan', current_timestamp)
                """, EMAIL);
        jdbcTemplate.update("""
                insert into adaptive_running_plan_sessions (
                    id,
                    plan_id,
                    week_number,
                    session_number,
                    session_type,
                    distance_kilometers,
                    target_type,
                    scheduled_date,
                    status
                ) values (101, 100, 1, 1, 'EASY_RUN', 5.0, 'PERCEIVED_EFFORT', current_date, 'PLANNED')
                """);
        jdbcTemplate.update("""
                insert into strava_account_links (
                    id,
                    user_email,
                    athlete_id,
                    active,
                    linked_at,
                    unlinked_at,
                    reconnect_required
                ) values (200, ?, 9001, false, current_timestamp, current_timestamp, false)
                """, EMAIL);
        jdbcTemplate.update("""
                insert into strava_authorization_states (
                    state,
                    user_email,
                    expires_at
                ) values ('state-token', ?, current_timestamp)
                """, EMAIL);
        jdbcTemplate.update("""
                insert into strava_data_consent_records (
                    user_email,
                    purpose,
                    consent_version,
                    language,
                    consented_at,
                    source
                ) values (?, 'STRAVA_DATA_IMPORT_AND_COACHING', 'strava-data-import-and-coaching-v1',
                    'pt-BR', current_timestamp, 'STRAVA_CONNECTION')
                """, EMAIL);
        jdbcTemplate.update("""
                insert into strava_activity_summaries (
                    id,
                    user_email,
                    account_link_id,
                    source_activity_id,
                    activity_type,
                    raw_sport_type,
                    name,
                    start_date,
                    imported_at
                ) values (300, ?, 200, 457, 'RUN', 'Run', 'Morning Run', current_timestamp, current_timestamp)
                """, EMAIL);
        jdbcTemplate.update("""
                insert into strava_activity_detail_snapshots (
                    activity_summary_id,
                    user_email,
                    source_activity_id,
                    activity_type,
                    raw_sport_type,
                    name,
                    start_date,
                    fetched_at
                ) values (300, ?, 457, 'RUN', 'Run', 'Morning Run', current_timestamp, current_timestamp)
                """, EMAIL);
        jdbcTemplate.update("""
                insert into strava_activity_stream_snapshots (
                    activity_summary_id,
                    account_link_id,
                    user_email,
                    source_activity_id,
                    available_metric_names,
                    stream_samples_json,
                    fetched_at
                ) values (300, 200, ?, 457, 'time,distance', '[]', current_timestamp)
                """, EMAIL);
        jdbcTemplate.update("""
                insert into strava_summary_sync_jobs (
                    account_link_id,
                    open_account_link_id,
                    user_email,
                    status,
                    attempt_count,
                    imported_activity_count,
                    run_after,
                    created_at,
                    updated_at
                ) values (200, 200, ?, 'QUEUED', 0, 0, current_timestamp, current_timestamp, current_timestamp)
                """, EMAIL);
        jdbcTemplate.update("""
                insert into strava_activity_stream_sync_jobs (
                    activity_summary_id,
                    open_activity_summary_id,
                    account_link_id,
                    user_email,
                    source_activity_id,
                    priority,
                    status,
                    attempt_count,
                    run_after,
                    created_at,
                    updated_at
                ) values (300, 300, 200, ?, 457, 'NORMAL', 'QUEUED', 0, current_timestamp, current_timestamp, current_timestamp)
                """, EMAIL);
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    @RestController
    @RequestMapping("/api/protected")
    static class ProtectedController {

        @GetMapping
        String protectedResource() {
            return "ok";
        }
    }
}
