package com.example.taskmanager.integration;

import com.example.taskmanager.dto.AuthenticationRequest;
import com.example.taskmanager.dto.AuthenticationResponse;
import com.example.taskmanager.dto.UserRequestDto;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserControllerIntegrationTest {

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:8-alpine")
            .withExposedPorts(6379);

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testDb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgresContainer::getDriverClassName);
    }

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void checkUsers() {
        List<User> users = userRepository.findAll();
        System.out.println("Users in db:");
        users.forEach(user -> System.out.println(user.getUsername()));
    }

    @AfterEach
    void checkUsersAfterEach() {
        List<User> users = userRepository.findAll();
        System.out.println("Users in db:");
        users.forEach(user -> System.out.println(user.getUsername()));
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    void getAllUsers_ShouldReturnAllUsers_WhenRoleAdmin() throws Exception {
        mvc.perform(
                get("/users")
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists());
    }

    @Test
    @Order(2)
    void getUserById_ShouldReturnUserById_WhenRoleAdmin() throws Exception {
        mvc.perform(
                get("/users/1")
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test user"));
    }

    @Test
    @Order(3)
    void createUser_ShouldCreateUser_WhenRoleAdmin() throws Exception {
        var request = new UserRequestDto("newUser", "password");

        mvc.perform(
                post("/users")
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(request.getUsername()));
    }

    @Test
    @Order(4)
    void updateUser_ShouldUpdateUser_WhenRoleAdmin() throws Exception {
        var request = new UserRequestDto("updatedUser", "password");

        mvc.perform(
                put("/users/3")
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(request.getUsername()));
    }

    @Test
    @Order(5)
    void deleteUser_ShouldDeleteUser_WhenRoleAdmin() throws Exception {
        mvc.perform(
                delete("/users/3")
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(6)
    void deleteUser_ShouldReturn403_WhenRoleIsNotAdmin() throws Exception {
        mvc.perform(
                delete("/users/2")
                        .header(HttpHeaders.AUTHORIZATION, getUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    void deleteUser_ShouldReturn404_WhenUserIsNotFound() throws Exception {
        mvc.perform(
                delete("/users/999")
                                .header(HttpHeaders.AUTHORIZATION, getAdminJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    void create_user_ShouldReturn400_WhenMissingRequiredFields() throws Exception {
        String invalidJson = """
                {
                "username": "",
                "password": "",
                }
                """;

        mvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .header(HttpHeaders.AUTHORIZATION, getAdminJwt()))
                .andExpect(status().isBadRequest());
    }

    private String getAdminJwt() throws Exception {
        var login = new AuthenticationRequest("test admin", "admin123");

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
        var login = new AuthenticationRequest("test user", "user123");

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
}







































