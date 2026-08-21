package com.sudolife;

import com.sudolife.adapter.driving.rest.user.webmodel.ChangePasswordRequest;
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

    @RestController
    @RequestMapping("/api/protected")
    static class ProtectedController {

        @GetMapping
        String protectedResource() {
            return "ok";
        }
    }
}
