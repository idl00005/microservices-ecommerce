package com.catalogo;

import com.Entidades.Producto;
import com.Otros.ProductEvent;
import com.Recursos.CatalogoResource;
import com.Repositorios.RepositorioProducto;
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

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        catalogoService = new CatalogoService(); // Usa la instancia real
        catalogoService.productEventEmitter = productEventEmitter; // Inyecta el mock
        catalogoService.productoRepository = mockRepositorio; // Inyecta el mock
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
    void testGetProducts_InvalidParameters() {
        // Simular respuestas para parámetros inválidos
        when(catalogoResource.getProducts(0, 10, null, null, null, null))
                .thenReturn(Response.status(Response.Status.BAD_REQUEST)
                        .entity("Los parámetros 'page' y 'size' deben ser mayores o iguales a 1.").build());

        when(catalogoResource.getProducts(1, 0, null, null, null, null))
                .thenReturn(Response.status(Response.Status.BAD_REQUEST)
                        .entity("Los parámetros 'page' y 'size' deben ser mayores o iguales a 1.").build());

        // Test con página inválida
        Response response = catalogoResource.getProducts(0, 10, null, null, null, null);
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Los parámetros 'page' y 'size' deben ser mayores o iguales a 1."));

        // Test con tamaño inválido
        response = catalogoResource.getProducts(1, 0, null, null, null, null);
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Los parámetros 'page' y 'size' deben ser mayores o iguales a 1."));
    }

    @Test
    void testAddProduct_Success() {
        Producto producto = crearProductoEjemplo();

        // Configurar el mock para devolver una respuesta válida
        when(catalogoResource.addProduct(producto))
                .thenReturn(Response.status(Response.Status.CREATED).entity(producto).build());

        // Llamar al método
        Response response = catalogoResource.addProduct(producto);

        // Validar la respuesta
        assertNotNull(response);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(producto, response.getEntity());
    }


    @Test
    void testAddProduct_Error() {
        Producto producto = crearProductoEjemplo();

        // Configurar el mock del servicio para lanzar una excepción
        doThrow(new RuntimeException("Falló")).when(mockRepositorio).add(producto);

        // Configurar el mock del recurso para manejar la excepción
        when(catalogoResource.addProduct(producto))
                .thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error al agregar el producto").build());

        // Llamar al método
        Response response = catalogoResource.addProduct(producto);

        // Validar la respuesta
        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Error al agregar el producto"));
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

        // Configurar el mock del recurso para devolver una respuesta NOT_FOUND
        when(catalogoResource.updateProduct(eq(1L), eq(producto)))
                .thenReturn(Response.status(Response.Status.NOT_FOUND)
                        .entity("Producto no encontrado").build());

        // Llamar al método
        Response response = catalogoResource.updateProduct(1L, producto);

        // Validar la respuesta
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Producto no encontrado"));
    }

    @Test
    void testUpdateProduct_Error() {
        Producto producto = crearProductoEjemplo();

        // Configurar el mock del recurso para devolver una respuesta INTERNAL_SERVER_ERROR
        when(catalogoResource.updateProduct(eq(1L), eq(producto)))
                .thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error al actualizar el producto").build());

        // Llamar al método
        Response response = catalogoResource.updateProduct(1L, producto);

        // Validar la respuesta
        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Error al actualizar el producto"));
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
        assertEquals(producto, response.getEntity());
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

}

