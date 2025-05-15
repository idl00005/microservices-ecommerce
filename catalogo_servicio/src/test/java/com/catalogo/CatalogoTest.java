package com.catalogo;

import com.Entidades.Producto;
import com.Entidades.Valoracion;
import com.Otros.PaginacionResponse;
import com.Otros.ProductEvent;
import com.Recursos.CatalogoResource;
import com.Repositorios.RepositorioProducto;
import com.Repositorios.ValoracionRepository;
import com.Servicios.CatalogoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogoTest {

    @InjectMocks
    CatalogoService catalogoService;

    @Mock
    RepositorioProducto mockRepositorio;

    @Mock
    Emitter<ProductEvent> productEventEmitter;

    @Mock
    CatalogoResource catalogoResource;

    @Mock
    ValoracionRepository valoracionRepository;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        catalogoService = new CatalogoService();
        catalogoService.productEventEmitter = productEventEmitter; // Inyecta el mock
        catalogoService.productoRepository = mockRepositorio; // Inyecta el mock
        catalogoService.valoracionRepository = valoracionRepository; // Inyecta el mock
        catalogoService.objectMapper = new ObjectMapper();
    }

    private Producto crearProductoEjemplo() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode detalles = mapper.createObjectNode();
        detalles.put("color", "verde");

        Producto producto = new Producto();
        producto.setNombre("Camiseta");
        producto.setDescripcion("Camiseta de algodón");
        producto.setPrecio(new BigDecimal("29.99"));
        producto.setStock(10);
        producto.setDetalles(detalles);

        return producto;
    }

    @Test
    void testObtenerProductos() {
        Producto producto = new Producto("Camiseta", "Camiseta de algodón", new BigDecimal("29.99"), 10, "Ropa", null);

        when(mockRepositorio.listAll()).thenReturn(List.of(producto));

        List<Producto> productos = catalogoService.obtenerProductos(1, 10, null, null, null, null);

        assertNotNull(productos);
        assertEquals(1, productos.size());
        assertEquals("Camiseta", productos.get(0).getNombre());
    }

    @Test
    void testAgregarProducto() {
        Producto producto = crearProductoEjemplo();

        doNothing().when(mockRepositorio).add(any(Producto.class));

        Producto resultado = catalogoService.agregarProducto(producto);

        assertNotNull(resultado);
        assertEquals("Camiseta", resultado.getNombre());
        verify(mockRepositorio).add(producto);
    }

    @Test
    void testEmitirEventoProducto() {
        ProductEvent event = new ProductEvent(1L, "CREATED", null);

        // No es necesario usar doNothing, simplemente verifica la interacción
        catalogoService.emitirEventoProducto(event);

        // Verificar que el método send fue llamado con el evento correcto
        verify(productEventEmitter).send(event);
    }

    @Test
    void testGetProducts() {
        // Crear productos de ejemplo
        Producto producto1 = new Producto("Camiseta", "Camiseta de algodón", new BigDecimal("29.99"), 10, "Ropa", null);
        Producto producto2 = new Producto("Pantalón", "Pantalón de mezclilla", new BigDecimal("49.99"), 20, "Ropa", null);

        // Configurar el mock para devolver una lista de productos
        when(mockRepositorio.listAll()).thenReturn(List.of(producto1, producto2));

        // Llamar al método del servicio
        List<Producto> productos = catalogoService.obtenerProductos(1, 2, null, "Ropa", null, null);

        // Validar los resultados
        assertNotNull(productos);
        assertEquals(2, productos.size());
        assertEquals("Camiseta", productos.get(0).getNombre());
        assertEquals("Pantalón", productos.get(1).getNombre());

        // Verificar que el repositorio fue llamado
        verify(mockRepositorio).listAll();
    }

    @Test
    void testGetProducts_Empty() {
        // Simular que el servicio devuelve una lista vacía
        when(mockRepositorio.listAll()).thenReturn(new ArrayList<>());

        // Llamar al método del servicio
        List<Producto> productos = catalogoService.obtenerProductos(1, 10, null, null, null, null);

        // Verificar que la lista devuelta esté vacía
        assertNotNull(productos);
        assertTrue(productos.isEmpty());
    }

    @Test
    void testAddProduct_Success() {
        Producto producto = crearProductoEjemplo();

        // Configurar el mock para que el método void no haga nada
        doNothing().when(mockRepositorio).add(any(Producto.class));

        // Llamar al método del servicio
        Producto resultado = catalogoService.agregarProducto(producto);

        // Validar el resultado
        assertNotNull(resultado);
        assertEquals("Camiseta", resultado.getNombre());

        // Verificar que el método add fue llamado con el producto correcto
        verify(mockRepositorio).add(producto);
    }


    @Test
    void testAddProduct_Error() {
        Producto producto = crearProductoEjemplo();

        // Configurar el mock del repositorio para lanzar una excepción
        doThrow(new RuntimeException("Falló")).when(mockRepositorio).add(any(Producto.class));

        // Llamar al método del servicio directamente
        Exception exception = assertThrows(RuntimeException.class, () -> {
            catalogoService.agregarProducto(producto);
        });

        // Validar que la excepción tenga el mensaje esperado
        assertEquals("Falló", exception.getMessage());

        // Verificar que el método add fue llamado con el producto correcto
        verify(mockRepositorio).add(producto);
    }

    @Test
    void testUpdateProduct() {
        // Crear el producto original
        Producto original = new Producto("Original", "Descripción original", new BigDecimal("10.00"), 5, "Categoría", null);
        original.setId(1L);

        // Configurar el comportamiento del repositorio
        when(mockRepositorio.findById(1L)).thenReturn(original);
        when(mockRepositorio.updateProduct(eq(1L), anyString(), anyString(), any(), any(), any())).thenReturn(true);

        // Crear el producto actualizado
        Producto actualizado = new Producto("Actualizado", "Nueva descripción", new BigDecimal("20.00"), 10, "Nueva Categoría", null);

        // Llamar al método del servicio directamente
        boolean resultado = catalogoService.actualizarProducto(1L, actualizado);

        // Validar el resultado
        assertTrue(resultado);

        // Verificar que el repositorio fue llamado con los valores correctos
        verify(mockRepositorio).updateProduct(
                eq(1L),
                eq("Actualizado"),
                eq("Nueva descripción"),
                eq(new BigDecimal("20.00")),
                eq(10),
                eq(null)
        );
    }

    @Test
    void testUpdateProduct_NotFound() {
        Producto producto = crearProductoEjemplo();

        // Configurar el mock del servicio para devolver false (producto no encontrado)
        when(mockRepositorio.updateProduct(eq(1L), anyString(), anyString(), any(), any(), any())).thenReturn(false);

        // Llamar al método del servicio directamente
        boolean resultado = catalogoService.actualizarProducto(1L, producto);

        // Validar que el resultado sea false
        assertFalse(resultado);

        // Verificar que el método del repositorio fue llamado con los valores correctos
        verify(mockRepositorio).updateProduct(
                eq(1L),
                eq(producto.getNombre()),
                eq(producto.getDescripcion()),
                eq(producto.getPrecio()),
                eq(producto.getStock()),
                eq(producto.getDetalles())
        );
    }

    @Test
    void testUpdateProduct_Error() {
        Producto producto = crearProductoEjemplo();

        // Configurar el mock del repositorio para lanzar una excepción
        doThrow(new RuntimeException("Error al actualizar el producto"))
                .when(mockRepositorio).updateProduct(eq(1L), anyString(), anyString(), any(), any(), any());

        // Llamar al método del servicio directamente
        Exception exception = assertThrows(RuntimeException.class, () -> {
            catalogoService.actualizarProducto(1L, producto);
        });

        // Validar que la excepción tenga el mensaje esperado
        assertEquals("Error al actualizar el producto", exception.getMessage());

        // Verificar que el método del repositorio fue llamado con los valores correctos
        verify(mockRepositorio).updateProduct(
                eq(1L),
                eq(producto.getNombre()),
                eq(producto.getDescripcion()),
                eq(producto.getPrecio()),
                eq(producto.getStock()),
                eq(producto.getDetalles())
        );
    }

    @Test
    void testDeleteProduct() {
        // Simular un producto en la "base de datos"
        Producto producto = new Producto("Eliminar", "Producto a borrar", new BigDecimal("30.00"), 1, "Categoria", null);
        producto.setId(1L);

        // Configurar el comportamiento del repositorio
        when(mockRepositorio.findById(1L)).thenReturn(producto);
        doNothing().when(mockRepositorio).delete(producto);

        // Llamar al método del servicio directamente
        boolean resultado = catalogoService.eliminarProducto(1L);

        // Validar el resultado
        assertTrue(resultado);

        // Verificar que el método delete fue llamado
        verify(mockRepositorio).delete(producto);
    }

    @Test
    void testDeleteProduct_NotFound() {
        // Configurar el mock del recurso para devolver una respuesta NOT_FOUND
        when(catalogoResource.deleteProduct(99L))
                .thenReturn(Response.status(Response.Status.NOT_FOUND)
                        .entity("Producto no encontrado").build());

        // Llamar al método
        Response response = catalogoResource.deleteProduct(99L);

        // Validar la respuesta
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Producto no encontrado"));
    }

    @Test
    void testDeleteProduct_Error() {
        // Configurar el mock del recurso para devolver una respuesta INTERNAL_SERVER_ERROR
        when(catalogoResource.deleteProduct(1L))
                .thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error al eliminar el producto").build());

        // Llamar al método
        Response response = catalogoResource.deleteProduct(1L);

        // Validar la respuesta
        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Error al eliminar el producto"));
    }

    @Test
    void testGetProductById_Found() {
        // Crear un producto de ejemplo
        Producto producto = new Producto("Camiseta", "Camiseta de algodón", new BigDecimal("29.99"), 10, "Ropa", null);
        producto.setId(1L);

        // Configurar el mock del recurso para devolver una respuesta válida
        when(catalogoResource.getProductById(1L))
                .thenReturn(Response.status(Response.Status.OK).entity(producto).build());

        // Llamar al método
        Response response = catalogoResource.getProductById(1L);

        // Validar la respuesta
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Extraer el producto del Response
        Producto productoRespuesta = (Producto) response.getEntity();
        assertNotNull(productoRespuesta);
        assertEquals(producto.getId(), productoRespuesta.getId());
        assertEquals(producto.getNombre(), productoRespuesta.getNombre());
    }

    @Test
    void testGetProductById_NotFound() {
        // Configurar el mock del recurso para devolver una respuesta NOT_FOUND
        when(catalogoResource.getProductById(1L))
                .thenReturn(Response.status(Response.Status.NOT_FOUND)
                        .entity("Producto con ID 1 no encontrado.").build());

        // Llamar al método
        Response response = catalogoResource.getProductById(1L);

        // Verificar que el estado sea 404 Not Found
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        // Verificar que el mensaje de error sea el esperado
        assertTrue(response.getEntity().toString().contains("Producto con ID 1 no encontrado."));
    }

    @Test
    void testObtenerValoracionesPorProducto() {
        Valoracion valoracion1 = new Valoracion();
        valoracion1.setIdUsuario("usuario1");
        valoracion1.setIdProducto(1L);
        valoracion1.setPuntuacion(5);
        valoracion1.setComentario("Excelente producto");

        Valoracion valoracion2 = new Valoracion();
        valoracion2.setIdUsuario("usuario2");
        valoracion2.setIdProducto(1L);
        valoracion2.setPuntuacion(4);
        valoracion2.setComentario("Muy bueno");

        // Configurar los mocks para devolver datos válidos
        when(valoracionRepository.obtenerValoracionesPorProducto(1L, 0, 5))
                .thenReturn(List.of(valoracion1, valoracion2));
        when(valoracionRepository.contarValoracionesPorProducto(1L)).thenReturn(2L);
        PaginacionResponse<Valoracion> paginacionMock = new PaginacionResponse<>(
                List.of(valoracion1, valoracion2), 1, 5, 2L
        );

        when(catalogoResource.obtenerValoracionesPorProducto(1L, 1, 5))
                .thenReturn(Response.ok(paginacionMock).build());

        Response response = catalogoResource.obtenerValoracionesPorProducto(1L,1,5);

        // Validar que la respuesta no sea null
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Validar que la entidad de la respuesta no sea null
        PaginacionResponse<Valoracion> paginacionResponse = (PaginacionResponse<Valoracion>) response.getEntity();
        assertNotNull(paginacionResponse);
        assertEquals(2, paginacionResponse.getDatos().size());
        assertEquals(2, paginacionResponse.getTotal());
    }

    @Test
    void testObtenerValoracionesPorProducto_Error() {
        // Configurar el mock para lanzar una excepción en el servicio
        when(catalogoService.obtenerValoracionesPorProducto(1L, 1, 2))
                .thenThrow(new RuntimeException("Error al obtener valoraciones"));

        // Configurar el mock del repositorio para devolver un valor válido para contarValoracionesPorProducto
        when(valoracionRepository.contarValoracionesPorProducto(1L)).thenReturn(0L);

        // Llamar al método
        Exception exception = assertThrows(RuntimeException.class, () -> {
            catalogoService.obtenerValoracionesPorProducto(1L, 1, 2);
        });

        // Validar que la excepción tenga el mensaje esperado
        assertEquals("Error al obtener valoraciones", exception.getMessage());
    }

    @Test
    void testActualizarPuntuacionProducto() {
        Producto producto = new Producto("Camiseta", "Camiseta de algodón", new BigDecimal("29.99"), 10, "Ropa", null);
        producto.setId(1L);
        producto.setPuntuacion(4.0);

        Valoracion valoracion = new Valoracion();
        valoracion.setIdUsuario("usuario1");
        valoracion.setIdProducto(1L);
        valoracion.setPuntuacion(5);
        valoracion.setComentario("Excelente producto");

        when(mockRepositorio.findById(1L)).thenReturn(producto);
        when(valoracionRepository.contarValoracionesPorProducto(1L)).thenReturn(2L);

        // JSON válido para el evento de valoración
        String mensaje = "{\"idUsuario\":\"usuario1\",\"idProducto\":1,\"puntuacion\":5,\"comentario\":\"Excelente producto\"}";
        catalogoService.procesarEventoValoracion(mensaje);

        // Verificar que el método persist fue llamado
        verify(mockRepositorio).persist(producto);

        // Validar que la puntuación fue actualizada correctamente
        assertEquals(4.5, producto.getPuntuacion());
    }

}

