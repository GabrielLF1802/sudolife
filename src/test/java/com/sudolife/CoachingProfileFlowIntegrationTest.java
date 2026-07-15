package com.sudolife;

import com.sudolife.application.service.training.SaveCoachingProfileCommand;
import com.sudolife.application.service.user.AuthenticateUserCommand;
import com.sudolife.application.service.user.RegisterUserCommand;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static com.sudolife.helper.UserTestHelper.EMAIL;
import static com.sudolife.helper.UserTestHelper.NAME;
import static com.sudolife.helper.UserTestHelper.PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedTimeProvider.class)
class CoachingProfileFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from coaching_profiles");
        jdbcTemplate.update("delete from training_profiles");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void authenticated_user_saves_and_retrieves_coaching_profiles() throws Exception {
        registerUser();
        String token = login();
        SaveCoachingProfileCommand command = new SaveCoachingProfileCommand(10.0, 330, LocalDate.parse("2026-05-12"),
                "LOW", true, List.of("TUESDAY", "SATURDAY"));

        mockMvc.perform(put("/api/coaching-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDistanceKilometers").value(10.0))
                .andExpect(jsonPath("$.targetPaceSecondsPerKilometer").value(330))
                .andExpect(jsonPath("$.targetDate").value("2026-05-12"))
                .andExpect(jsonPath("$.readiness").value("LOW"))
                .andExpect(jsonPath("$.injuryConcern").value(true))
                .andExpect(jsonPath("$.preferredRunningDays[0]").value("TUESDAY"))
                .andExpect(jsonPath("$.preferredRunningDays[1]").value("SATURDAY"));

        mockMvc.perform(get("/api/coaching-profiles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDistanceKilometers").value(10.0))
                .andExpect(jsonPath("$.readiness").value("LOW"))
                .andExpect(jsonPath("$.injuryConcern").value(true))
                .andExpect(jsonPath("$.preferredRunningDays[0]").value("TUESDAY"))
                .andExpect(jsonPath("$.preferredRunningDays[1]").value("SATURDAY"));
    }

    @Test
    void authenticated_user_gets_unconfigured_coaching_profiles_before_saving() throws Exception {
        registerUser();
        String token = login();

        mockMvc.perform(get("/api/coaching-profiles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.targetDistanceKilometers").doesNotExist())
                .andExpect(jsonPath("$.readiness").doesNotExist());
    }

    @Test
    void authenticated_user_cannot_save_invalid_coaching_profiles() throws Exception {
        registerUser();
        String token = login();
        SaveCoachingProfileCommand command = new SaveCoachingProfileCommand(10.0, 330, LocalDate.parse("2026-05-10"),
                "LOW", false);

        mockMvc.perform(put("/api/coaching-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COACHING_PROFILE"))
                .andExpect(jsonPath("$.message").value("Target date cannot be in the past"));
    }

    private void registerUser() throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, PASSWORD);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated());
    }

    private String login() throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(EMAIL, PASSWORD);

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
}
