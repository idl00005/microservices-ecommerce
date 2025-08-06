package Integracion;

import Entidades.Pedido;
import Repositorios.PedidoRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
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
                .post("/pedidos")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body("usuarioId", equalTo("user1"))
                .body("productoId", equalTo(1))
                .body("estado", equalTo("PENDIENTE"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testCrearPedidoInvalido() {
        Pedido pedido = new Pedido();
        pedido.setUsuarioId("user1");
        pedido.setProductoId(1L);
        pedido.setCantidad(-1); // Cantidad negativa, inválida
        pedido.setPrecioTotal(BigDecimal.valueOf(200));
        pedido.setEstado("INVALIDO"); // Estado inválido
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido.setOrdenId(1L);

        given()
                .contentType(ContentType.JSON)
                .body(pedido)
                .when()
                .post("/pedidos")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("parameterViolations", not(empty()));
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    public void testObtenerPedidosPorUsuario() {
        // Pedido de prueba para user1
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId("user1");
        pedido.setProductoId(1L);
        pedido.setCantidad(2);
        pedido.setPrecioTotal(BigDecimal.valueOf(200));
        pedido.setEstado("PENDIENTE");
        pedido.setFechaCreacion(LocalDateTime.now());

        // El repositorio solo debe buscar por usuarioId
        Mockito.when(pedidoRepository.buscarPorEstadoYUsuarioConPaginacion(null, "user1", 0, 10))
                .thenReturn(List.of(pedido));

        given()
                .auth().basic("user1", "password")
                .when()
                .get("/pedidos")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("$", not(empty()))
                .body("[0].productoId", equalTo(1))
                .body("[0].cantidad", equalTo(2))
                .body("[0].estado", equalTo("PENDIENTE"))
                .body("[0].precioTotal", equalTo(200.0f));
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    public void testListarPedidosConParametrosInvalidos() {
        given()
                .queryParam("estado", "PENDIENTE")
                .queryParam("pagina", 0) // inválido: debe ser > 0
                .queryParam("tamanio", 5)
                .when()
                .get("/pedidos")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("parameterViolations", not(empty()))
                .body("parameterViolations[0].path", containsString("pagina"))
                .body("parameterViolations[0].message", containsString("El número de página debe ser mayor o igual a 0"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testObtenerPedidoPorIdExistente_Admin() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId("user1");
        pedido.setProductoId(1L);
        pedido.setCantidad(2);
        pedido.setPrecioTotal(BigDecimal.valueOf(150));
        pedido.setEstado("PENDIENTE");
        pedido.setFechaCreacion(LocalDateTime.now());

        Mockito.when(pedidoRepository.buscarPorId(1L)).thenReturn(pedido);

        given()
                .auth().basic("admin", "adminpassword")
                .when()
                .get("/pedidos/1")
                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("usuarioId", equalTo("user1"))
                .body("estado", equalTo("PENDIENTE"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testObtenerPedidoPorIdInexistente() {
        Mockito.when(pedidoRepository.buscarPorId(999L))
                .thenReturn(null);

        given()
                .auth().basic("admin", "adminpassword")
                .when()
                .get("/pedidos/999")
                .then()
                .statusCode(404)
                .body(containsString("Pedido no encontrado"));
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
                .contentType("application/json")
                .body("{ \"estado\": \"ENVIADO\" }")
                .when()
                .patch("/pedidos/1/estado")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("Estado del pedido actualizado correctamente"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testCambiarEstadoPedidoConEstadoInvalido() {
        Long pedidoId = 1L;

        String cuerpoInvalido = """
        {
            "estado": "ESTADOINVALIDO"
        }
    """;

        given()
                .auth().basic("admin", "adminpassword")
                .contentType(ContentType.JSON)
                .body(cuerpoInvalido)
                .when()
                .patch("/pedidos/" + pedidoId + "/estado")
                .then()
                .statusCode(400) // BAD_REQUEST
                .body("parameterViolations[0].message", containsString("El estado debe ser uno de los valores permitidos"))
                .body("parameterViolations[0].path", containsString("estado"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testCrearValoracionValidaConPedidoCompleto() {
        Pedido pedido = new Pedido();
        pedido.setUsuarioId("admin");
        pedido.setProductoId(1L);
        pedido.setCantidad(2);
        pedido.setPrecioTotal(BigDecimal.valueOf(200));
        pedido.setEstado("COMPLETADO");
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido.setOrdenId(1L);

        Mockito.when(pedidoRepository.buscarPorId(1L)).thenReturn(pedido);
        // Crear valoración
        String valoracionJson = """
        {
            "puntuacion": 5,
            "comentario": "Muy buen producto, entrega rápida."
        }
    """;

        given()
                .auth().basic("admin", "adminpassword")
                .contentType(ContentType.JSON)
                .body(valoracionJson)
                .when()
                .post("/pedidos/" + 1 + "/valoracion")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body(equalTo("Valoración creada y enviada correctamente"));
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    public void testCrearValoracionInvalida() {
        Long pedidoId = 1L;

        String valoracionJson = """
        {
            "puntuacion": -1,
            "comentario": "No me gustó nada."
        }
    """;

        given()
                .auth().basic("user1", "password")
                .contentType(ContentType.JSON)
                .body(valoracionJson)
                .when()
                .post("/pedidos/" + pedidoId + "/valoracion")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("parameterViolations[0].message", containsString("La puntuación debe ser mayor que 0"));  // ajusta al mensaje real de tu validación
    }

}