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
        Producto producto = crearProductoEjemplo();
        when(mockRepositorio.listAll()).thenReturn(List.of(producto));

        List<Producto> resultado = catalogoResource.getProducts();

        assertEquals(1, resultado.size());
        assertEquals("Camiseta", resultado.get(0).getNombre());
    }

    @Test
    void testAddProduct_Success() {
        Producto producto = crearProductoEjemplo();

        // No se necesita mockear 'add' si no lanza excepción
        Response response = catalogoResource.addProduct(producto);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Producto añadido con éxito.", response.getEntity());
        verify(mockRepositorio).add(producto);
    }

    @Test
    void testAddProduct_Error() {
        Producto producto = crearProductoEjemplo();
        doThrow(new RuntimeException("Falló")).when(mockRepositorio).add(producto);

        Response response = catalogoResource.addProduct(producto);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Error al añadir el producto"));
    }

    @Test
    void testUpdateProduct() {
        // Crear producto original
        Producto original = new Producto("Original", "Desc", new BigDecimal("10.00"), 5, null);
        original.setId(1L);

        // Guardar en una lista simulando la base de datos
        List<Producto> fakeDB = new ArrayList<>();
        fakeDB.add(original);

        // Simular comportamiento del repositorio
        when(mockRepositorio.findById(1L)).thenReturn(original);
        when(mockRepositorio.updateProduct(eq(1L), anyString(), anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    original.setNombre("Actualizado");
                    original.setDescripcion("Nueva descripción");
                    original.setPrecio(new BigDecimal("20.00"));
                    original.setStock(10);
                    return true;
                });

        when(mockRepositorio.listAll()).thenReturn(fakeDB);

        // Llamar a la actualización
        Producto actualizado = new Producto("Actualizado", "Nueva descripción", new BigDecimal("20.00"), 10, null);
        Response updateResponse = catalogoResource.updateProduct(1L, actualizado);
        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

        // Llamar al get y comprobar los valores cambiados
        List<Producto> productos = catalogoResource.getProducts();
        Producto producto = productos.get(0);

        assertEquals("Actualizado", producto.getNombre());
        assertEquals("Nueva descripción", producto.getDescripcion());
        assertEquals(new BigDecimal("20.00"), producto.getPrecio());
        assertEquals(10, producto.getStock());
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
        // Producto simulado en la "base de datos"
        Producto producto = new Producto("Eliminar", "Producto a borrar", new BigDecimal("30.00"), 1, null);
        producto.setId(1L);
        List<Producto> fakeDB = new ArrayList<>();
        fakeDB.add(producto);

        // Simular findById y delete
        when(mockRepositorio.findById(1L)).thenReturn(producto);
        doAnswer(invocation -> {
            fakeDB.remove(producto);
            return null;
        }).when(mockRepositorio).delete(producto);

        when(mockRepositorio.listAll()).thenAnswer(invocation -> fakeDB);

        // Borrar producto
        Response deleteResponse = catalogoResource.deleteProduct(1L);
        assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());

        // Comprobar que ya no está
        List<Producto> productosRestantes = catalogoResource.getProducts();
        assertTrue(productosRestantes.isEmpty());
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

