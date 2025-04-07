package com.catalogo;

import com.Entidades.Producto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CatalogoTestIntegracion {

    ObjectMapper objectMapper = new ObjectMapper();

    private Producto crearProductoTest() {
        return new Producto("Zapato", "Zapato deportivo", new BigDecimal("59.99"), 10, null);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testAddAndGetProduct() {
        Producto producto = crearProductoTest();

        // Añadir el producto
        given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(201)
                .body(containsString("Producto añadido"));

        // Obtener productos y verificar que existe
        when()
                .get("/catalogo")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[-1].nombre", equalTo("Zapato"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testUpdateProduct() {
        Producto producto = crearProductoTest();

        // Añadir primero
        Integer id = given()
            .contentType(ContentType.JSON)
            .body(producto)
            .when()
            .post("/catalogo")
            .then()
            .log().all()
            .statusCode(201)
            .extract()
            .path("producto.id");

        // Actualizar
        producto.setNombre("Zapatilla Pro");
        producto.setPrecio(new BigDecimal("79.99"));

        given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .put("/catalogo/" + id) // Usamos el ID extraído previamente
                .then()
                .log().all()
                .statusCode(200)
                .body(containsString("actualizado"));

        // Confirmar cambio
        when()
                .get("/catalogo")
                .then()
                .statusCode(200)
                .body("[0].nombre", equalTo("Zapatilla Pro"))
                .body("[0].precio", equalTo(79.99f));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testDeleteProduct() {
        Producto producto = crearProductoTest();

        // Añadir primero
        Integer id = given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(201)
                .extract()
                .path("producto.id");

        // Eliminar
        when()
                .delete("/catalogo/" + id)
                .then()
                .statusCode(200)
                .body(containsString("eliminado"));

        // Verificar que ya no está
        when()
                .get("/catalogo")
                .then()
                .statusCode(200)
                .body("findAll { it.id == " + id + " }", empty());
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testAddInvalidProduct() {
        Producto producto = new Producto("", "", new BigDecimal("-10"), -5, null);

        given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(400); // Esperado por Bean Validation
    }
}

