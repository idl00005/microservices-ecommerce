package com.catalogo;

import com.Entidades.Producto;
import com.DTO.ValoracionDTO;
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

    private Producto crearProductoTest() {
        return new Producto("Zapato", "Zapato deportivo", new BigDecimal("59.99"), 10, "Ropa", null);
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

        // Confirmar cambio buscando por ID
        when()
                .get("/catalogo/" + id)
                .then()
                .statusCode(200)
                .body("nombre", equalTo("Zapatilla Pro"))
                .body("precio", equalTo(79.99f));
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
        Producto producto = new Producto("", "", new BigDecimal("-10"), -5, "Ropa", null);

        given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(400); // Esperado por Bean Validation
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testSearchProductByName() {
        Producto producto = new Producto("Zapato Deportivo", "Zapato cómodo para correr", new BigDecimal("59.99"), 10, "Calzado", null);

        // Añadir el producto
        given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(201);

        // Buscar por nombre
        when()
                .get("/catalogo?nombre=Zapato")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].nombre", containsString("Zapato"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testSearchProductByCategory() {
        Producto producto = new Producto("Camiseta Deportiva", "Camiseta de algodón", new BigDecimal("29.99"), 20, "Ropa", null);

        // Añadir el producto
        given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(201);

        // Buscar por categoría
        when()
                .get("/catalogo?categoria=Ropa")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].categoria", equalTo("Ropa"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testSearchProductByPriceRange() {
        Producto producto = new Producto("Monitor", "Monitor Full HD", new BigDecimal("199.99"), 15, "Electrónica", null);

        // Añadir el producto
        given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(201);

        // Buscar por rango de precio
        when()
                .get("/catalogo?precioMin=100&precioMax=200")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].precio", equalTo(199.99f));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testGetProductById_Found() {
        Producto producto = new Producto("Laptop", "Laptop de alta gama", new BigDecimal("999.99"), 5, "Electrónica", null);

        // Añadir el producto
        Integer id = given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(201)
                .extract()
                .path("producto.id");

        // Obtener el producto por ID
        when()
                .get("/catalogo/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("nombre", equalTo("Laptop"))
                .body("categoria", equalTo("Electrónica"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testGetProductById_NotFound() {
        // Intentar obtener un producto con un ID inexistente
        when()
                .get("/catalogo/9999")
                .then()
                .statusCode(404)
                .body(containsString("Producto con ID 9999 no encontrado."));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testReservarStock() {
        Producto producto = crearProductoTest();

        // Añadir el producto
        Integer id = given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(201)
                .extract()
                .path("producto.id");

        // Reservar stock
        given()
                .contentType(ContentType.JSON) // Asegura que el encabezado Content-Type esté presente
                .queryParam("cantidad", 5)
                .when()
                .post("/catalogo/" + id + "/reservar")
                .then()
                .statusCode(200);

        // Verificar que el stock reservado se actualizó
        when()
                .get("/catalogo/" + id)
                .then()
                .statusCode(200)
                .body("stockReservado", equalTo(5));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testReservarStockInsuficiente() {
        Producto producto = crearProductoTest();

        // Añadir el producto
        Integer id = given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(201)
                .extract()
                .path("producto.id");

        // Intentar reservar más stock del disponible
        given()
                .contentType(ContentType.JSON) // Asegura que el encabezado Content-Type esté presente
                .queryParam("cantidad", 20)
                .when()
                .post("/catalogo/" + id + "/reservar")
                .then()
                .statusCode(409) // Conflicto
                .body(containsString("Stock insuficiente"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @TestTransaction
    void testObtenerValoracionesPorProducto() {
        Producto producto = crearProductoTest();

        // Añadir el producto
        Integer id = given()
                .contentType(ContentType.JSON)
                .body(producto)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(201)
                .extract()
                .path("producto.id");

        // Obtener valoraciones (debería estar vacío inicialmente)
        when()
                .get("/catalogo/" + id + "/valoraciones")
                .then()
                .statusCode(200)
                .body("datos.size()", equalTo(0))
                .body("total", equalTo(0));
    }
}

