package com.Integracion;

import com.Autenticacion.AuthResource.RegisterRequest;
import com.Autenticacion.AuthResource.UserCredentials;
import com.Entidades.Usuario;
import com.Repositorios.RepositorioUsuario;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class AuthResourceIntegracion {

    @Inject
    RepositorioUsuario userRepository;

    @Inject
    BCryptPasswordEncoder passwordEncoder;

    @Test
    @TestTransaction
    public void testRegisterSuccess() {
        RegisterRequest newUser = new RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890",
                "password123"
        );

        given()
                .contentType("application/json")
                .body(newUser)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201)
                .body("token", Matchers.notNullValue());
    }

    @Test
    @TestTransaction
    public void testRegisterConflict() {
        RegisterRequest newUser = new RegisterRequest(
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "1234567890",
                "password123"
        );

        given()
                .contentType("application/json")
                .body(newUser)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        given()
                .contentType("application/json")
                .body(newUser)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    @TestTransaction
    public void testLoginSuccess() {
        // Register a new user
        RegisterRequest newUser = new RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890",
                "password123"
        );

        given()
                .contentType("application/json")
                .body(newUser)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        // Attempt to log in with the registered user's credentials
        UserCredentials credentials = new UserCredentials(
                "john.doe@example.com",
                "password123"
        );

        given()
                .contentType("application/json")
                .body(credentials)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body(Matchers.notNullValue());
    }

    @Test
    @TestTransaction
    public void testLoginUnauthorized() {
        UserCredentials credentials = new UserCredentials(
                "nonexistent@example.com",
                "wrongpassword"
        );

        given()
                .contentType("application/json")
                .body(credentials)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);
    }
}
