package com.catalogo;

import com.Entidades.Producto;
import com.Repositorios.RepositorioProducto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.Response;
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
    CatalogoResource catalogoResource;

    @Mock
    RepositorioProducto mockRepositorio;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
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
    void testGetProducts() {
        Producto producto1 = new Producto("Camiseta", "Camiseta de algodón", new BigDecimal("29.99"), 10, "Ropa", null);
        Producto producto2 = new Producto("Pantalón", "Pantalón de mezclilla", new BigDecimal("49.99"), 20, "Ropa", null);
        Producto producto3 = new Producto("Zapatos", "Zapatos deportivos", new BigDecimal("79.99"), 15, "Calzado", null);

        when(mockRepositorio.listAll()).thenReturn(List.of(producto1, producto2, producto3));

        // Solicitar la primera página con tamaño 2 y filtro por categoría "Ropa"
        Response response = catalogoResource.getProducts(1, 2, null, "Ropa", null, null);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Validar el contenido de la respuesta
        List<?> resultado = (List<?>) response.getEntity();
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("Camiseta", ((Producto) resultado.get(0)).getNombre());
        assertEquals("Pantalón", ((Producto) resultado.get(1)).getNombre());
    }

    @Test
    void testGetProducts_Empty() {
        // Simular que el repositorio no tiene productos
        when(mockRepositorio.listAll()).thenReturn(new ArrayList<>());

        // Llamar al método con parámetros de paginación y filtros
        Response response = catalogoResource.getProducts(1, 10, null, null, null, null);

        // Verificar que el código de estado HTTP sea 204 No Content
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // La entidad debería ser null en este caso
        assertNull(response.getEntity());
    }

    @Test
    void testGetProducts_InvalidParameters() {
        // Test with invalid page number
        Response response = catalogoResource.getProducts(0, 10, null, null, null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Los parámetros 'page' y 'size' deben ser mayores o iguales a 1."));

        // Test with invalid size
        response = catalogoResource.getProducts(1, 0, null, null, null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Los parámetros 'page' y 'size' deben ser mayores o iguales a 1."));

        // Test with invalid page
        response = catalogoResource.getProducts(0, 1, null, null, null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Los parámetros 'page' y 'size' deben ser mayores o iguales a 1."));
    }

    @Test
    void testAddProduct_Success() {
        Producto producto = crearProductoEjemplo();

        // No se necesita mockear 'add' si no lanza excepción
        Response response = catalogoResource.addProduct(producto);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(mockRepositorio).add(producto);
    }


    @Test
    void testAddProduct_Error() {
        Producto producto = crearProductoEjemplo();
        doThrow(new RuntimeException("Falló")).when(mockRepositorio).add(producto);

        Response response = catalogoResource.addProduct(producto);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testUpdateProduct() {
        // Create the original product
        Producto original = new Producto("Original", "Descripción original", new BigDecimal("10.00"), 5, "Categoría", null);
        original.setId(1L);

        // Mock repository behavior
        when(mockRepositorio.findById(1L)).thenReturn(original);
        when(mockRepositorio.updateProduct(eq(1L), anyString(), anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    original.setNombre("Actualizado");
                    original.setDescripcion("Nueva descripción");
                    original.setPrecio(new BigDecimal("20.00"));
                    original.setStock(10);
                    original.setCategoria("Nueva Categoría");
                    return true;
                });

        // Call the update method
        Producto actualizado = new Producto("Actualizado", "Nueva descripción", new BigDecimal("20.00"), 10, "Nueva Categoría", null);
        Response updateResponse = catalogoResource.updateProduct(1L, actualizado);

        // Validate the response
        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());
        assertTrue(updateResponse.getEntity().toString().contains("actualizado"));

        // Verify the updated product
        assertEquals("Actualizado", original.getNombre());
        assertEquals("Nueva descripción", original.getDescripcion());
        assertEquals(new BigDecimal("20.00"), original.getPrecio());
        assertEquals(10, original.getStock());
        assertEquals("Nueva Categoría", original.getCategoria());
    }

    @Test
    void testUpdateProduct_NotFound() {
        Producto producto = crearProductoEjemplo();
        when(mockRepositorio.updateProduct(anyLong(), any(), any(), any(), any(), any())).thenReturn(false);

        Response response = catalogoResource.updateProduct(1L, producto);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("no encontrado"));
    }

    @Test
    void testUpdateProduct_Error() {
        Producto producto = crearProductoEjemplo();
        when(mockRepositorio.updateProduct(anyLong(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Error interno"));

        Response response = catalogoResource.updateProduct(1L, producto);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Error al actualizar el producto"));
    }

    @Test
    void testDeleteProduct() {
        // Simulate a product in the "database"
        Producto producto = new Producto("Eliminar", "Producto a borrar", new BigDecimal("30.00"), 1, "Categoria", null);
        producto.setId(1L);

        // Mock repository behavior
        when(mockRepositorio.findById(1L)).thenReturn(producto);
        doNothing().when(mockRepositorio).delete(producto);

        // Call the delete method
        Response deleteResponse = catalogoResource.deleteProduct(1L);

        // Validate the response
        assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());
        assertTrue(deleteResponse.getEntity().toString().contains("eliminado"));

        // Verify the delete method was called
        verify(mockRepositorio).delete(producto);
    }

    @Test
    void testDeleteProduct_NotFound() {
        when(mockRepositorio.findById(99L)).thenReturn(null);

        Response response = catalogoResource.deleteProduct(99L);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("no encontrado"));
    }

    @Test
    void testDeleteProduct_Error() {
        when(mockRepositorio.findById(1L)).thenReturn(crearProductoEjemplo());
        doThrow(new RuntimeException("Error")).when(mockRepositorio).delete(any());

        Response response = catalogoResource.deleteProduct(1L);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Error al eliminar el producto"));
    }

}

