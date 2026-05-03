package com.sudolife.adapter.driving.rest.user.controller;

import com.sudolife.application.service.user.CurrentUserResult;
import com.sudolife.application.service.user.RegisterUserCommand;
import com.sudolife.application.service.user.exception.UserAlreadyExistsException;
import com.sudolife.application.service.user.ports.provided.GetCurrentUserUseCase;
import com.sudolife.application.service.user.ports.provided.RegisterUserUseCase;
import com.sudolife.config.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static com.sudolife.helper.UserTestHelper.EMAIL;
import static com.sudolife.helper.UserTestHelper.PASSWORD;
import static com.sudolife.helper.UserTestHelper.NAME;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Test
    void registerUser_returns_created_when_command_is_valid() throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, PASSWORD);

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command))).andExpect(status().isCreated());

        verify(registerUserUseCase).execute(command);
    }

    @Test
    void registerUser_returns_conflict_when_user_already_exists() throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, PASSWORD);
        doThrow(new UserAlreadyExistsException()).when(registerUserUseCase).execute(command);

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("User already exists"));
    }

    @Test
    void getCurrentUser_returns_current_user_when_authenticated() throws Exception {
        CurrentUserResult result = new CurrentUserResult(1L, NAME, EMAIL);
        when(getCurrentUserUseCase.execute(EMAIL)).thenReturn(result);

        mockMvc.perform(get("/api/users/me").principal(authenticated(EMAIL, null, java.util.List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value(NAME))
                .andExpect(jsonPath("$.email").value(EMAIL));

        verify(getCurrentUserUseCase).execute(EMAIL);
    }
}
