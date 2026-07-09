package com.sudolife;

import com.sudolife.application.service.training.SaveTrainingProfileCommand;
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
class TrainingProfileFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from training_profiles");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void authenticated_user_saves_and_retrieves_training_profile() throws Exception {
        registerUser();
        String token = login();

        mockMvc.perform(put("/api/training-profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveTrainingProfileCommand(1990))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthYear").value(1990))
                .andExpect(jsonPath("$.adaptiveCoachingEligible").value(true));

        mockMvc.perform(get("/api/training-profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthYear").value(1990))
                .andExpect(jsonPath("$.adaptiveCoachingEligible").value(true));
    }

    @Test
    void authenticated_user_gets_ineligible_profile_when_birth_year_is_missing() throws Exception {
        registerUser();
        String token = login();

        mockMvc.perform(get("/api/training-profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthYear").doesNotExist())
                .andExpect(jsonPath("$.adaptiveCoachingEligible").value(false));
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
