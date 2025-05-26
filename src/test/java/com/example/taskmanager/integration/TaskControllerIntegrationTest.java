package com.example.taskmanager.integration;

import com.example.taskmanager.controller.TaskController;
import com.example.taskmanager.dto.AuthenticationRequest;
import com.example.taskmanager.dto.AuthenticationResponse;
import com.example.taskmanager.dto.TaskRequestDto;
import com.example.taskmanager.dto.TaskStatusUpdateRequest;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.service.TokenBlacklistService;
import com.example.taskmanager.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskControllerIntegrationTest {

    @Value("${jwt.secret}")
    String secret;

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testDb")
            .withUsername("test")
            .withPassword("test");
    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgreSQLContainer::getDriverClassName);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void checkUsersAndTasksBeforeEach() {
        List<User> users = userRepository.findAll();
        List<Task> tasks = taskRepository.findAll();
        System.out.println("Users in db:");
        users.forEach(user -> System.out.println(user.getUsername()));
        System.out.println("Tasks in db:");
        tasks.forEach(task -> System.out.println(task.getTitle()));
    }

    @AfterEach
    void checkUsersAndTasksAfterEach() {
        List<User> users = userRepository.findAll();
        List<Task> tasks = taskRepository.findAll();
        System.out.println("Users in db:");
        users.forEach(user -> System.out.println(user.getUsername()));
        System.out.println("Tasks in db:");
        tasks.forEach(task -> System.out.println(task.getTitle()));
    }

    @Test
    @Order(1)
    void getTasksByStatus_ShouldReturnTasksByStatus() throws Exception {
        mvc.perform(
                get("/tasks/filter")
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt())
                        .param("status", TaskStatus.PENDING.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.[0]").exists());
    }

    @Test
    @Order(2)
    void getAllTasks_ShouldReturnAllTasksForCurrentUser() throws Exception {
        mvc.perform(
                get("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, getUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.[0]").exists());
    }

    @Test
    @Order(3)
    void getTaskById_ShouldReturnTaskByUserUsernameAndId() throws Exception {
        mvc.perform(
                get("/tasks/1")
                        .header(HttpHeaders.AUTHORIZATION, getUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Task 1"));
    }

    @Test
    @Order(4)
    void createTask_ShouldCreateTask_WhenRoleIsAdmin() throws Exception {
        var request = new TaskRequestDto("created task", "new created task", TaskStatus.PENDING, 1L);

        mvc.perform(
                post("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(request.getTitle()))
                .andExpect(jsonPath("$.user.username").value("john"));
    }

    @Test
    @Order(5)
    void updateTaskByAdmin_ShouldUpdateTask_WhenRoleIsAdmin() throws Exception {
        var request = new TaskRequestDto("updated task", "new updated task", TaskStatus.PENDING, 1L);

        mvc.perform(
                put("/tasks/3")
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(request.getTitle()));
    }

    @Test
    @Order(6)
    void updateTaskStatus_ShouldUpdateTaskStatusForCurrentUserTask() throws Exception {
        var request = new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS);

        mvc.perform(
                patch("/tasks/3/status")
                        .header(HttpHeaders.AUTHORIZATION, getUserJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TaskStatus.IN_PROGRESS.name()));
    }

    @Test
    @Order(7)
    void deleteTask_ShouldDeleteTask_WhenRoleIsAdmin() throws Exception {
        mvc.perform(
                delete("/tasks/3")
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(8)
    void createTask_ShouldReturn403_WhenRoleIsNotAdmin() throws Exception {
        var request = new TaskRequestDto("some task", "created not by admin", TaskStatus.PENDING, 1L);

        mvc.perform(
                        post("/tasks")
                                .header(HttpHeaders.AUTHORIZATION, getUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    void deleteTask_ShouldReturn403_WhenRoleIsNotAdmin() throws Exception {
        mvc.perform(
                        delete("/tasks/3")
                                .header(HttpHeaders.AUTHORIZATION, getUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    void updateTaskStatus_ShouldReturn403_WhenUserUpdateNotHisOwnTask() throws Exception {
        var request = new TaskStatusUpdateRequest(TaskStatus.CANCELLED);

        mvc.perform(
                        patch("/tasks/2/status")
                                .header(HttpHeaders.AUTHORIZATION, getUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    void getAllTasks_ShouldReturn401_WhenNoAuthHeader() throws Exception {
        mvc.perform(
                        get("/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    void createTask_ShouldReturn400_WhenInvalidRequest() throws Exception {
        var request = new TaskRequestDto("", "", null, null);

        mvc.perform(
                        post("/tasks")
                                .header(HttpHeaders.AUTHORIZATION, getAdminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(13)
    void shouldRefreshAccessToken_WhenAccessTokenIsExpired_AndRefreshTokenIsValid() throws Exception {
        var login = new AuthenticationRequest("john", "user123");

        var result = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        var userDetails = userDetailsServiceImpl.loadUserByUsername("john");
        var expiredAccessToken = generateExpiredAccessToken(userDetails);
        var setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        var refreshToken = parseRefreshTokenFromSetCookie(setCookieHeader);

        mvc.perform(
                get("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccessToken)
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    @Order(14)
    void shouldReturn401_WhenTokenIsBlacklisted() throws Exception {
        var jwt = getUserJwt().replace("Bearer ", "");

        tokenBlacklistService.blacklistToken(jwt, 60000L);

        mvc.perform(
                get("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(15)
    void shouldReturn401_WhenAuthHeaderIsIncorrect() throws Exception {
        mvc.perform(
                get("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(16)
    void getTaskById_ShouldReturn404_WhenTaskNotFound() throws Exception {
        mvc.perform(
                get("/tasks/999")
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt()))
                .andExpect(status().isNotFound());
    }

    private String getAdminJwt() throws Exception {
        var login = new AuthenticationRequest("alice", "admin123");

        var result = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();

        var response = objectMapper.readValue(body, AuthenticationResponse.class);

        return "Bearer " + response.getAccessToken();
    }

    private String getUserJwt() throws Exception {
        var login = new AuthenticationRequest("john", "user123");

        var result = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();

        var response = objectMapper.readValue(body, AuthenticationResponse.class);

        return "Bearer " + response.getAccessToken();
    }

    private String generateExpiredAccessToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 1000);

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiredDate)
                .signWith(key)
                .compact();
    }

    private String parseRefreshTokenFromSetCookie(String header) {
        if (header == null) {
            return null;
        }
        for (String part : header.split(";")) {
            if (part.trim().startsWith("refreshToken=")) {
                return part.trim().substring("refreshToken=".length());
            }
        }
        return null;
    }
}
