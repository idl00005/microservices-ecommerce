package Componente;

import Cliente.StockClient;
import DTO.ProductoDTO;
import Entidades.CarritoItem;
import Entidades.OrdenPago;
import Recursos.CarritoResource;
import Recursos.CarritoResource.IniciarPagoRequest;
import Recursos.CarritoResource.AgregarProductoRequest;
import Repositorios.CarritoItemRepository;
import Repositorios.OrdenPagoRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CarritoResourceTest {

    @InjectMock
    CarritoItemRepository carritoItemRepository;

    @InjectMock
    OrdenPagoRepository ordenPagoRepository;

    @InjectMock
    StockClient stockClient;

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testAgregarEliminarProducto() {
        // Mock del cliente de producto
        ProductoDTO productoMock = new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10,"url");
        when(stockClient.obtenerProductoPorId(1L)).thenReturn(productoMock);
        // Agregar producto
        AgregarProductoRequest request = new AgregarProductoRequest();
        request.productoId = 1L;
        request.cantidad = 2;

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/carrito/")
                .then()
                .statusCode(200)
                .body("nombre", notNullValue())
                .body("cantidad", equalTo(2))
                .body("precio", notNullValue());
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testObtenerCarrito() {
        // Configura el mock del repositorio
        CarritoItem item = new CarritoItem();
        item.setUserId("user");
        item.setProductoId(1L);
        item.setCantidad(2);

        when(carritoItemRepository.findByUserId("user")).thenReturn(List.of(item));
        when(stockClient.obtenerProductoPorId(item.getProductoId())).thenReturn(new ProductoDTO(item.getProductoId(), "Producto Test", BigDecimal.valueOf(100), 10,"url"));

        // Realiza la solicitud y verifica la respuesta
        given()
                .when()
                .get("/carrito/")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].nombre", equalTo("Producto Test"))
                .body("[0].cantidad", equalTo(2));
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testEliminarProducto() {
        // Mock del repositorio para la búsqueda
        CarritoItem item = new CarritoItem();
        item.setUserId("user");
        item.setProductoId(1L);
        item.setCantidad(2);
        when(carritoItemRepository.findByUserAndProducto("user", 1L)).thenReturn(Optional.of(item));

        // Eliminar producto
        given()
                .when()
                .delete("/carrito/1")
                .then()
                .statusCode(204); // No Content
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testVaciarCarrito() {
        CarritoItem item = new CarritoItem();
        item.setUserId("user");
        item.setProductoId(1L);
        item.setCantidad(1);
        carritoItemRepository.persist(item);

        given()
                .when()
                .delete("/carrito/")
                .then()
                .statusCode(204); // No Content
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testActualizarCantidad() {
        // Mock del cliente de producto
        ProductoDTO productoMock = new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10,"url");
        when(stockClient.obtenerProductoPorId(1L)).thenReturn(productoMock);

        // Mock del repositorio para la búsqueda
        CarritoItem item = new CarritoItem();
        item.setUserId("user");
        item.setProductoId(1L);
        item.setCantidad(1);

        when(carritoItemRepository.findByUserAndProducto("user", 1L)).thenReturn(Optional.of(item));

        // Configurar la solicitud para actualizar la cantidad
        CarritoResource.ActualizarCantidadRequest request = new CarritoResource.ActualizarCantidadRequest(5);

        // Realizar la solicitud y verificar la respuesta
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/carrito/1")
                .then()
                .statusCode(200)
                .body("cantidad", equalTo(5));
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testIniciarPagoExitoso() {
        // Mock del carrito con productos
        CarritoItem item = new CarritoItem();
        item.setUserId("user");
        item.setProductoId(1L);
        item.setCantidad(2);

        when(carritoItemRepository.findByUserId("user")).thenReturn(List.of(item));

        IniciarPagoRequest requestBody = new IniciarPagoRequest("2123456789","Calle Test");

        when(stockClient.obtenerProductoPorId(item.getProductoId())).thenReturn(new ProductoDTO(item.getProductoId(), "Producto Test", BigDecimal.valueOf(100), 10,"url"));
        Mockito.doNothing().when(stockClient).reservarStock(Mockito.anyMap(), Mockito.anyString());
        Mockito.doNothing().when(ordenPagoRepository).persist(Mockito.any(OrdenPago.class));

        // Realizar la solicitud
        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/carrito/ordenes-pago")
                .then()
                .statusCode(200)
                .body("estado", equalTo("CREADO"))
                .body("montoTotal", equalTo(200));
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testIniciarPagoCarritoVacio() {
        when(carritoItemRepository.findByUserId("user")).thenReturn(List.of());

        IniciarPagoRequest requestBody = new IniciarPagoRequest("2123456789","Calle Test");

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/carrito/ordenes-pago")
                .then()
                .statusCode(400)
                .body(containsString("El carrito está vacío"));
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void testIniciarPagoMontoCero() {
        // Mock del carrito con productos gratuitos
        CarritoItem item = new CarritoItem();
        item.setUserId("user");
        item.setProductoId(1L);
        item.setCantidad(1);

        when(carritoItemRepository.findByUserId("user")).thenReturn(List.of(item));

        IniciarPagoRequest requestBody = new IniciarPagoRequest("2123456789","Calle Test");

        when(stockClient.obtenerProductoPorId(item.getProductoId())).thenReturn(new ProductoDTO(item.getProductoId(), "Producto Test", BigDecimal.valueOf(100), 10,"url"));
        Mockito.doNothing().when(stockClient).reservarStock(Mockito.anyMap(), Mockito.anyString());
        Mockito.doNothing().when(ordenPagoRepository).persist(Mockito.any(OrdenPago.class));

        // Realizar la solicitud
        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/carrito/ordenes-pago")
                .then()
                .statusCode(200)
                .body("estado", equalTo("CREADO"))
                .body("montoTotal", equalTo(100));
    }

}