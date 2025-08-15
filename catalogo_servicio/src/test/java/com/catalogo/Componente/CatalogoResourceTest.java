package com.catalogo.Componente;

import com.Entidades.Producto;
import com.Entidades.Valoracion;
import com.Recursos.CatalogoResource;
import com.Repositorios.RepositorioProducto;
import com.Repositorios.ValoracionRepository;
import com.Servicios.CatalogoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

@QuarkusTest
public class CatalogoResourceTest {

    @Inject
    CatalogoResource catalogoResource;

    RepositorioProducto productoRepositoryMock;

    CatalogoService catalogoService;

    Emitter productEventEmitter;

    ValoracionRepository valoracionRepository;

    // JSON válido para crear un producto
    String productoValidoJson = """
        {
            "nombre": "Zapato",
            "descripcion": "Zapato deportivo",
            "precio": 59.99,
            "stock": 10,
            "categoria": "Ropa",
            "detalles": null
        }
        """;

    // JSON inválido (nombre vacío)
    String productoInvalidoJson = """
        {
            "nombre": "",
            "descripcion": "Zapato deportivo",
            "precio": 59.99,
            "stock": 10,
            "categoria": "Ropa",
            "detalles": null
        }
        """;

    @BeforeEach
    public void setup() {
        productoRepositoryMock = mock(RepositorioProducto.class);
        productEventEmitter = mock(Emitter.class);
        valoracionRepository = mock(ValoracionRepository.class);
        catalogoService = new CatalogoService();
        catalogoService.productoRepository = productoRepositoryMock;
        catalogoService.objectMapper = new ObjectMapper();
        catalogoResource.catalogoService = catalogoService;
        catalogoService.productEventEmitter = productEventEmitter;
        catalogoService.valoracionRepository = valoracionRepository;
    }



    @Test
    public void validarTamanoMaximoPagina() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 101)
                .when()
                .get("/catalogo")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("tamaño máximo permitido"));
    }

    @Test
    public void testObtenerProductosConFiltros() {
        // Datos simulados que devuelve el repositorio mockeado
        List<Producto> productosMock = List.of(
                new Producto("Zapato", "Zapato deportivo", BigDecimal.valueOf(59.99), 10, "Ropa", null),
                new Producto("Camisa", "Camisa formal", BigDecimal.valueOf(39.99), 5, "Ropa", null)
        );

        // Configuramos el mock para devolver la lista sin filtrar
        Mockito.when(productoRepositoryMock.buscarProductos(1,10,"zapato","Ropa",30.0,70.0))
                .thenReturn(productosMock);

        given()
                .contentType(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 10)
                .queryParam("nombre", "zapato")
                .queryParam("categoria", "Ropa")
                .queryParam("precioMin", 30.0)
                .queryParam("precioMax", 70.0)
                .when()
                .get("/catalogo")
                .then()
                .statusCode(200)
                .body("size()", is(productosMock.size()))
                .body("[0].nombre", equalTo("Zapato"))
                .body("[1].nombre", equalTo("Camisa"));
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin"})
    public void agregarProductoValido() {
        given()
                .contentType(ContentType.JSON)
                .body(productoValidoJson)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body("nombre", equalTo("Zapato"))
                .body("categoria", equalTo("Ropa"));
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin"})
    public void agregarProductoInvalido() {
        given()
                .contentType(ContentType.JSON)
                .body(productoInvalidoJson)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("El nombre del producto no puede estar vacío"));
    }

    @Test
    @TestSecurity(user = "normalUser", roles = {"user"})
    public void agregarProducto_accesoDenegado() {
        given()
                .contentType(ContentType.JSON)
                .body(productoValidoJson)
                .when()
                .post("/catalogo")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin"})
    public void actualizarProductoExistente() {
        Mockito.when(productoRepositoryMock.updateProduct(
                Mockito.anyLong(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(BigDecimal.class),
                Mockito.anyInt(),
                Mockito.any()
        )).thenReturn(true);
        given()
                .contentType(ContentType.JSON)
                .body(productoValidoJson)
                .when()
                .put("/catalogo/1")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin"})
    public void actualizarProductoInexistente() {
        given()
                .contentType(ContentType.JSON)
                .body(productoValidoJson)
                .when()
                .put("/catalogo/99999") // id que no existe
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin"})
    public void actualizarProducto() {
        given()
                .contentType(ContentType.JSON)
                .body(productoInvalidoJson)
                .when()
                .put("/catalogo/1")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("El nombre del producto no puede estar vacío"));
    }

    @Test
    @TestSecurity(user = "normalUser", roles = {"user"})
    public void actualizarProducto_accesoDenegado() {
        given()
                .contentType(ContentType.JSON)
                .body(productoValidoJson)
                .when()
                .put("/catalogo/1")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin"})
    public void eliminarProductoExistente() {
        Mockito.when(productoRepositoryMock.eliminarPorId(1L)).thenReturn(true);
        given()
                .when()
                .delete("/catalogo/1")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin"})
    public void eliminarProductoInexistente() {
        given()
                .when()
                .delete("/catalogo/99999")  // id que no existe
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @TestSecurity(user = "normalUser", roles = {"user"})
    public void eliminarProducto__accesoDenegado() {
        given()
                .when()
                .delete("/catalogo/1")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void obtenerProductoPorIdExistente() {
        // Mock producto existente
        Producto producto = new Producto("Zapato", "Zapato deportivo", BigDecimal.valueOf(59.99), 10, "Ropa", null);
        producto.setId(1L);

        Mockito.when(productoRepositoryMock.findById(1L)).thenReturn(producto);

        given()
                .when()
                .get("/catalogo/1")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("nombre", equalTo("Zapato"));
    }

    @Test
    public void obtenerProductoPorIdInexistente() {
        given()
                .when()
                .get("/catalogo/99999")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void obtenerProductoPorIdInvalido() {
        given()
                .when()
                .get("/catalogo/0")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        given()
                .when()
                .get("/catalogo/-1")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private static final Long PRODUCTO_ID = 1L;

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void reservarStock_suficiente() {
        Producto producto = new Producto("Teclado", "Teclado mecánico", new BigDecimal("89.99"), 10, "Electrónica", null);
        producto.setId(PRODUCTO_ID);

        Mockito.when(productoRepositoryMock.findById(PRODUCTO_ID))
                .thenReturn(producto);

        given()
                .contentType(ContentType.JSON)
                .body("{\"cantidad\": 3}")
                .when()
                .post("/catalogo/" + PRODUCTO_ID + "/reserva")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void reservarStock_insuficiente() {
        Producto producto = new Producto("Mouse", "Mouse inalámbrico", new BigDecimal("49.99"), 2, "Electrónica", null);
        producto.setId(PRODUCTO_ID);

        Mockito.when(productoRepositoryMock.findById(PRODUCTO_ID))
                .thenReturn(producto);

        given()
                .contentType(ContentType.JSON)
                .body("{\"cantidad\": 5}")
                .when()
                .post("/catalogo/" + PRODUCTO_ID + "/reserva")
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode())
                .body(containsString("Stock insuficiente"));
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void reservarStock_productoInexistente() {
        Mockito.when(productoRepositoryMock.findById(PRODUCTO_ID))
                .thenReturn(null);

        given()
                .contentType(ContentType.JSON)
                .body("{\"cantidad\": 2}")
                .when()
                .post("/catalogo/" + PRODUCTO_ID + "/reserva")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .body(containsString("Error reservando stock: Producto no encontrado con id 1"));
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    public void reservarStock_cantidadInvalida() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"cantidad\": 0}")
                .when()
                .post("/catalogo/" + PRODUCTO_ID + "/reserva")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void listarValoracionesProductoExistente() {
        List<Valoracion> valoracionesMock = List.of(
                new Valoracion("user1", new Producto(), 5,"Muy buen producto"),
                new Valoracion("user1", new Producto(), 5,"Muy buen producto")
        );

        Mockito.when(productoRepositoryMock.findValoracionesPaginadas(PRODUCTO_ID,1,10))
                .thenReturn(valoracionesMock);

        given()
                .contentType(ContentType.JSON)
                .queryParam("pagina", 1)
                .queryParam("tamanio", 10)
                .when()
                .get("/catalogo/" + PRODUCTO_ID + "/valoraciones")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("datos.size()", is(2));
    }

    @Test
    public void paginacionInvalida_sizeMayorA100() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("pagina", 1)
                .queryParam("tamanio", 101)
                .when()
                .get("/catalogo/" + PRODUCTO_ID + "/valoraciones")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("Parámetros de paginación inválidos"));
    }

    @Test
    public void paginacionInvalida_parametrosNegativos() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("pagina", -1)
                .queryParam("tamanio", 10)
                .when()
                .get("/catalogo/" + PRODUCTO_ID + "/valoraciones")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        given()
                .contentType(ContentType.JSON)
                .queryParam("pagina", 1)
                .queryParam("tamanio", 0)
                .when()
                .get("/catalogo/" + PRODUCTO_ID + "/valoraciones")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void productoSinValoraciones_devuelveListaVacia() {
        Mockito.when(productoRepositoryMock.findValoracionesPaginadas(PRODUCTO_ID, 1, 10))
                .thenReturn(List.of());

        given()
                .contentType(ContentType.JSON)
                .queryParam("pagina", 1)
                .queryParam("tamanio", 10)
                .when()
                .get("/catalogo/" + PRODUCTO_ID + "/valoraciones")
                .then()
                .log().all()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("datos.size()", is(0)) // ⬅️ Adaptación clave
                .body("pagina", is(1))
                .body("tamanio", is(10))
                .body("total", is(0));
    }


    private static final String USER_ID = "usuario1";
    @Test
    @TestSecurity(user = USER_ID, roles = {"user"})
    public void existeValoracion_paraUsuarioYProducto() {
        Mockito.when(valoracionRepository.count("producto.id = ?1 and usuarioId = ?2", PRODUCTO_ID, USER_ID)).thenReturn(1L);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/catalogo/" + PRODUCTO_ID + "/valoracion/existe")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("true"));
    }

    @Test
    @TestSecurity(user = USER_ID, roles = {"user"})
    public void noExisteValoracion_paraUsuarioYProducto() {
        Mockito.when(valoracionRepository.existsByProductoIdAndUsuarioId(PRODUCTO_ID, USER_ID)).thenReturn(false);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/catalogo/" + PRODUCTO_ID + "/valoracion/existe")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("false"));
    }

    @Test
    public void comprobarValoracionExistente_accesoDenegado() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/catalogo/" + PRODUCTO_ID + "/valoracion/existe")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }
}
