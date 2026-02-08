package com.devoops.user.integration;

import com.devoops.user.dto.request.LoginRequest;
import com.devoops.user.dto.request.RegisterRequest;
import com.devoops.user.entity.Role;
import com.devoops.user.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthenticationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("user_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/user/auth";
    }

    @Nested
    @DisplayName("Registration Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RegistrationTests {

        @Test
        @Order(1)
        @DisplayName("Should register a new GUEST user successfully")
        void register_WithValidGuestData_ReturnsCreatedWithToken() {
            RegisterRequest request = new RegisterRequest(
                    "guestuser",
                    "password123",
                    "guest@example.com",
                    "Guest",
                    "User",
                    "Belgrade",
                    Role.GUEST
            );

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/register")
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("accessToken", notNullValue())
                .body("accessToken", not(emptyString()))
                .body("tokenType", equalTo("Bearer"))
                .body("user.username", equalTo("guestuser"))
                .body("user.email", equalTo("guest@example.com"))
                .body("user.role", equalTo("GUEST"))
                .body("user.id", notNullValue());
        }

        @Test
        @Order(2)
        @DisplayName("Should register a new HOST user successfully")
        void register_WithValidHostData_ReturnsCreatedWithToken() {
            RegisterRequest request = new RegisterRequest(
                    "hostuser",
                    "password123",
                    "host@example.com",
                    "Host",
                    "User",
                    "Novi Sad",
                    Role.HOST
            );

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/register")
            .then()
                .statusCode(201)
                .body("user.role", equalTo("HOST"));
        }

        @Test
        @Order(3)
        @DisplayName("Should fail when username already exists")
        void register_WithExistingUsername_ReturnsConflict() {
            RegisterRequest request = new RegisterRequest(
                    "guestuser",
                    "password123",
                    "different@example.com",
                    "Different",
                    "User",
                    "Belgrade",
                    Role.GUEST
            );

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/register")
            .then()
                .statusCode(409)
                .body("title", equalTo("User Already Exists"))
                .body("detail", containsString("Username already taken"));
        }

        @Test
        @Order(4)
        @DisplayName("Should fail when email already exists")
        void register_WithExistingEmail_ReturnsConflict() {
            RegisterRequest request = new RegisterRequest(
                    "differentuser",
                    "password123",
                    "guest@example.com",
                    "Different",
                    "User",
                    "Belgrade",
                    Role.GUEST
            );

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/register")
            .then()
                .statusCode(409)
                .body("detail", containsString("Email already registered"));
        }

        @Test
        @DisplayName("Should fail with validation error when username is blank")
        void register_WithBlankUsername_ReturnsBadRequest() {
            RegisterRequest request = new RegisterRequest(
                    "",
                    "password123",
                    "new@example.com",
                    "Test",
                    "User",
                    "Belgrade",
                    Role.GUEST
            );

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/register")
            .then()
                .statusCode(400)
                .body("title", equalTo("Validation Error"))
                .body("errors.username", notNullValue());
        }

        @Test
        @DisplayName("Should fail with validation error when password is too short")
        void register_WithShortPassword_ReturnsBadRequest() {
            RegisterRequest request = new RegisterRequest(
                    "newuser",
                    "short",
                    "new@example.com",
                    "Test",
                    "User",
                    "Belgrade",
                    Role.GUEST
            );

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/register")
            .then()
                .statusCode(400)
                .body("errors.password", notNullValue());
        }

        @Test
        @DisplayName("Should fail with validation error when email is invalid")
        void register_WithInvalidEmail_ReturnsBadRequest() {
            RegisterRequest request = new RegisterRequest(
                    "newuser2",
                    "password123",
                    "invalid-email",
                    "Test",
                    "User",
                    "Belgrade",
                    Role.GUEST
            );

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/register")
            .then()
                .statusCode(400)
                .body("errors.email", notNullValue());
        }
    }

    @Nested
    @DisplayName("Login Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class LoginTests {

        @BeforeEach
        void setUpLoginUser() {
            if (userRepository.findByUsername("loginuser").isEmpty()) {
                RegisterRequest request = new RegisterRequest(
                        "loginuser",
                        "password123",
                        "login@example.com",
                        "Login",
                        "User",
                        "Belgrade",
                        Role.GUEST
                );

                given()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post("/register")
                .then()
                    .statusCode(201);
            }
        }

        @Test
        @Order(1)
        @DisplayName("Should login successfully with username")
        void login_WithValidUsername_ReturnsToken() {
            LoginRequest request = new LoginRequest("loginuser", "password123");

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/login")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("accessToken", notNullValue())
                .body("accessToken", not(emptyString()))
                .body("tokenType", equalTo("Bearer"))
                .body("user.username", equalTo("loginuser"));
        }

        @Test
        @Order(2)
        @DisplayName("Should login successfully with email")
        void login_WithValidEmail_ReturnsToken() {
            LoginRequest request = new LoginRequest("login@example.com", "password123");

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/login")
            .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("user.email", equalTo("login@example.com"));
        }

        @Test
        @DisplayName("Should fail when user does not exist")
        void login_WithNonExistentUser_ReturnsUnauthorized() {
            LoginRequest request = new LoginRequest("nonexistent", "password123");

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/login")
            .then()
                .statusCode(401)
                .body("title", equalTo("Invalid Credentials"));
        }

        @Test
        @DisplayName("Should fail when password is incorrect")
        void login_WithWrongPassword_ReturnsUnauthorized() {
            LoginRequest request = new LoginRequest("loginuser", "wrongpassword");

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/login")
            .then()
                .statusCode(401)
                .body("title", equalTo("Invalid Credentials"));
        }

        @Test
        @DisplayName("Should fail with validation error when username is blank")
        void login_WithBlankUsername_ReturnsBadRequest() {
            LoginRequest request = new LoginRequest("", "password123");

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/login")
            .then()
                .statusCode(400)
                .body("errors.usernameOrEmail", notNullValue());
        }

        @Test
        @DisplayName("Should fail with validation error when password is blank")
        void login_WithBlankPassword_ReturnsBadRequest() {
            LoginRequest request = new LoginRequest("loginuser", "");

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/login")
            .then()
                .statusCode(400)
                .body("errors.password", notNullValue());
        }
    }
}
