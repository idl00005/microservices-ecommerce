package com.Integracion;

import com.Recursos.AutenticacionResource.RegisterRequest;
import com.Recursos.AutenticacionResource.UserCredentials;
import com.Repositorios.RepositorioUsuario;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AutenticacionResourceTest {

    @Inject
    RepositorioUsuario userRepository;

    @Inject
    BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    @Transactional
    public void cleanDb() {
        userRepository.deleteAll();
    }

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
                .post("/autenticacion/register")
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
                .post("/autenticacion/register")
                .then()
                .statusCode(201);

        given()
                .contentType("application/json")
                .body(newUser)
                .when()
                .post("/autenticacion/register")
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
                .post("/autenticacion/register")
                .then()
                .statusCode(201);

        UserCredentials credentials = new UserCredentials(
                "john.doe@example.com",
                "password123"
        );

        given()
                .contentType("application/json")
                .body(credentials)
                .when()
                .post("/autenticacion/login")
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
                .post("/autenticacion/login")
                .then()
                .statusCode(500);
    }

    @Test
    public void testLoginValidacionCampos() {
        String jsonInvalido = """
        {
            "username": "",
            "password": ""
        }
        """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonInvalido)
                .when()
                .post("/autenticacion/login")
                .then()
                .statusCode(400)
                .body("parameterViolations.message", hasItems(
                        containsString("El usuario no puede quedar vacío"),
                        containsString("La contraseña no puede quedar vacía")
                ));
    }

    @Test
    public void testRegisterValidacionCampos() {
        String jsonInvalido = """
        {
            "firstName": "%s",
            "lastName": "",
            "email": "correo-no-valido",
            "phone": "abc123",
            "password": "123"
        }
        """.formatted("a".repeat(60));

        given()
                .contentType(ContentType.JSON)
                .body(jsonInvalido)
                .when()
                .post("/autenticacion/register")
                .then()
                .statusCode(400)
                // Comprobamos que alguno de los mensajes de parameterViolations contiene el texto esperado
                .body("parameterViolations.message", hasItems(
                        containsString("El nombre debe tener un máximo de 50 caracteres"),
                        containsString("El correo debe tener un formato válido"),
                        containsString("El apellido no puede quedar vacío"),
                        containsString("La contraseña no tener una longitud menor a 8 caracteres")
                ));
    }
}
