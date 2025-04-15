package Integracion;

import Cliente.ProductoClient;
import DTO.ProductoDTO;
import Entidades.CarritoItem;
import Recursos.CarritoResource.AgregarProductoRequest;
import Repositorios.CarritoItemRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
public class CarritoResourceIT {

    @InjectMock
    ProductoClient productoClient;

    @InjectMock
    CarritoItemRepository carritoItemRepository;

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testAgregarProducto() {
        // Mock del cliente de producto
        ProductoDTO productoMock = new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10);
        when(productoClient.obtenerProductoPorId(1L)).thenReturn(productoMock);

        AgregarProductoRequest request = new AgregarProductoRequest();
        request.productoId = 1L;
        request.cantidad = 2;

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/carrito/agregar")
                .then()
                .statusCode(200)
                .body("nombreProducto", notNullValue())
                .body("cantidad", equalTo(2))
                .body("precio", notNullValue());
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testObtenerCarrito() {
        CarritoItem item = new CarritoItem();
        item.userId = "user";
        item.productoId = 1L;
        item.nombreProducto = "Producto Test";
        item.precio = BigDecimal.valueOf(100);
        item.cantidad = 2;
        carritoItemRepository.persist(item);

        given()
                .when()
                .get("/carrito/")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].nombreProducto", equalTo("Producto Test"))
                .body("[0].cantidad", equalTo(2));
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testEliminarProducto() {
        // Agregar producto primero
        AgregarProductoRequest request = new AgregarProductoRequest();
        request.productoId = 1L;
        request.cantidad = 1;

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/carrito/agregar");

        // Eliminar producto
        given()
                .when()
                .delete("/carrito/eliminar/1")
                .then()
                .statusCode(204); // No Content
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testVaciarCarrito() {
        // Agregar producto primero
        AgregarProductoRequest request = new AgregarProductoRequest();
        request.productoId = 1L;
        request.cantidad = 1;

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/carrito/agregar");

        // Vaciar carrito
        given()
                .when()
                .delete("/carrito/vaciar")
                .then()
                .statusCode(204); // No Content
    }

}