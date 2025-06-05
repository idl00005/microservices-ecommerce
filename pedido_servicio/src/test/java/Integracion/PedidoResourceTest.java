package Integracion;

import Entidades.Pedido;
import Recursos.PedidoResource;
import Repositorios.PedidoRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class PedidoResourceTest {

    @InjectMock
    PedidoRepository pedidoRepository;

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testCrearPedido() {
        Pedido pedido = new Pedido();
        pedido.setUsuarioId("user1");
        pedido.setProductoId(1L);
        pedido.setCantidad(2);
        pedido.setPrecioTotal(BigDecimal.valueOf(200));
        pedido.setEstado("PENDIENTE");
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido.setOrdenId(1L);

        given()
                .contentType(ContentType.JSON)
                .body(pedido)
                .when()
                .post("/pedido")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body("usuarioId", equalTo("user1"))
                .body("productoId", equalTo(1))
                .body("estado", equalTo("PENDIENTE"));
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    public void testObtenerPedidosPorUsuario() {
        // Crear un pedido de prueba
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId("user1");
        pedido.setProductoId(1L);
        pedido.setCantidad(2);
        pedido.setPrecioTotal(BigDecimal.valueOf(200));
        pedido.setEstado("PENDIENTE");
        pedido.setFechaCreacion(LocalDateTime.now());

        Mockito.when(pedidoRepository.buscarPorUsuarioId("user1")).thenReturn(List.of(pedido));

        given()
                .auth().basic("user1", "password")
                .when()
                .get("/pedido")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("$", not(empty()))
                .body("[0].productoId", equalTo(1))
                .body("[0].cantidad", equalTo(2))
                .body("[0].estado", equalTo("PENDIENTE"))
                .body("[0].precioTotal", equalTo(200.0f)); // Ojo, compara float/double
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testListarPedidos_Admin() {
        // Crear un pedido de prueba
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId("user1");
        pedido.setProductoId(1L);
        pedido.setCantidad(2);
        pedido.setPrecioTotal(BigDecimal.valueOf(200));
        pedido.setEstado("PENDIENTE");
        pedido.setFechaCreacion(LocalDateTime.now());

        // Configurar el mock para devolver una lista de pedidos
        Mockito.when(pedidoRepository.buscarPorEstadoYUsuarioConPaginacion("PENDIENTE", null, 0, 10))
                .thenReturn(List.of(pedido));

        // Crear el filtro de búsqueda
        PedidoResource.FiltroPedidoRequest filtro = new PedidoResource.FiltroPedidoRequest();
        filtro.setEstado("PENDIENTE");
        filtro.setUsuarioId(null);
        filtro.setPagina(1);
        filtro.setTamanio(10);

        // Ejecutar el test
        given()
                .auth().basic("admin", "adminpassword")
                .contentType(MediaType.APPLICATION_JSON)
                .body(filtro)
                .when()
                .get("/pedido/filtro")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("$", not(empty()))
                .body("[0].estado", equalTo("PENDIENTE"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testCambiarEstadoPedido() {
        // Crear un pedido de prueba
        Pedido pedido = new Pedido();
        pedido.setId(1L); // Asignar un ID válido
        pedido.setUsuarioId("user1");
        pedido.setProductoId(1L);
        pedido.setCantidad(2);
        pedido.setPrecioTotal(BigDecimal.valueOf(200));
        pedido.setEstado("PENDIENTE");
        pedido.setFechaCreacion(LocalDateTime.now());

        // Configurar el mock para devolver el pedido
        Mockito.when(pedidoRepository.buscarPorId(1L)).thenReturn(pedido);

        // Ejecutar el test
        given()
                .auth().basic("admin", "adminpassword")
                .queryParam("estado", "ENVIADO")
                .when()
                .patch("/pedido/1/estado")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("Estado del pedido actualizado correctamente"));
    }
}