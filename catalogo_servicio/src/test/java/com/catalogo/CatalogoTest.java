package com.catalogo;

import com.Entidades.Producto;
import com.Repositorios.RepositorioProducto;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class CatalogoTest {

    @InjectMocks
    CatalogoResource catalogoResource; // Clase que vamos a probar

    @Mock
    RepositorioProducto productoRepository; // Mock del repositorio

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Inicializar los mocks
    }

    @Test
    void testGetProducts() {
        // Datos de prueba
        Producto producto1 = new Producto();
        producto1.setId(1L);
        producto1.setNombre("Producto 1");
        producto1.setDescripcion("Descripción 1");

        Producto producto2 = new Producto();
        producto2.setId(2L);
        producto2.setNombre("Producto 2");
        producto2.setDescripcion("Descripción 2");

        List<Producto> productosEsperados = Arrays.asList(producto1, producto2);

        // Simular el comportamiento del repositorio
        when(productoRepository.listAll()).thenReturn(productosEsperados);

        // Llamar al método
        List<Producto> resultado = catalogoResource.getProducts();

        // Verificar el resultado
        assertEquals(2, resultado.size());
        assertEquals("Producto 1", resultado.get(0).getNombre());
        assertEquals("Producto 2", resultado.get(1).getNombre());
    }

    @Test
    void testAddProduct() {
        // Simular producto de entrada
        Producto productoNuevo = new Producto();
        productoNuevo.setNombre("Nuevo Producto");
        productoNuevo.setDescripcion("Descripción del nuevo producto");

        // No necesitamos simular nada en el repositorio, ya que no devuelve resultado

        // Llamar al método
        Response response = catalogoResource.addProduct(productoNuevo);

        // Verificar el resultado
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Producto añadido con éxito.", response.getEntity());
    }

    @Test
    void testUpdateProduct_Success() {
        // Producto actualizado
        Producto productoActualizado = new Producto();
        productoActualizado.setNombre("Producto Actualizado");
        productoActualizado.setDescripcion("Descripción Actualizada");

        // Simular que el producto existe y se actualiza correctamente
        when(productoRepository.updateProduct(1L, "Producto Actualizado", "Descripción Actualizada", null, null, null))
                .thenReturn(true);

        // Llamar al método
        Response response = catalogoResource.updateProduct(1L, productoActualizado);

        // Verificar el resultado
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Producto actualizado con éxito.", response.getEntity());
    }

    @Test
    void testUpdateProduct_NotFound() {
        // Producto actualizado
        Producto productoActualizado = new Producto();
        productoActualizado.setNombre("Producto Actualizado");
        productoActualizado.setDescripcion("Descripción Actualizada");

        // Simular que el producto no existe
        when(productoRepository.updateProduct(1L, "Producto Actualizado", "Descripción Actualizada", null, null, null))
                .thenReturn(false);

        // Llamar al método
        Response response = catalogoResource.updateProduct(1L, productoActualizado);

        // Verificar el resultado
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals("Producto con ID 1 no encontrado.", response.getEntity());
    }

    @Test
    void testDeleteProduct_Success() {
        // Simular que el producto existe y es eliminado
        when(productoRepository.deleteById(1L)).thenReturn(true);

        // Llamar al método
        Response response = catalogoResource.deleteProduct(1L);

        // Verificar el resultado
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Producto eliminado con éxito.", response.getEntity());
    }

    @Test
    void testDeleteProduct_NotFound() {
        // Simular que el producto no existe
        when(productoRepository.deleteById(1L)).thenReturn(false);

        // Llamar al método
        Response response = catalogoResource.deleteProduct(1L);

        // Verificar el resultado
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals("Producto con ID 1 no encontrado.", response.getEntity());
    }
}