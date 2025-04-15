package Unitario;

import Cliente.ProductoClient;
import DTO.ProductoDTO;
import Entidades.CarritoItem;
import Otros.ProductEvent;
import Repositorios.CarritoItemRepository;
import Servicios.CarritoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CarritoResourceTest {

    @InjectMock
    CarritoItemRepository carritoItemRepository;

    @InjectMock
    ProductoClient productoClient;

    @Inject
    CarritoService carritoService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @TestTransaction
    public void testAgregarProductoNuevo() {
        // Mock del producto
        ProductoDTO productoMock = new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10);
        when(productoClient.obtenerProductoPorId(1L)).thenReturn(productoMock);

        // Mock de CarritoItem
        CarritoItem mockItem = mock(CarritoItem.class);
        doNothing().when(mockItem).persist();

        // Llamada al método
        CarritoItem result = carritoService.agregarProducto("user1", 1L, 2);

        // Verificaciones
        assertNotNull(result);
        assertEquals("Producto Test", result.nombreProducto);
        assertEquals(2, result.cantidad);
        verify(productoClient, times(1)).obtenerProductoPorId(1L);
    }

    @Test
    @TestTransaction
    public void testProcesarEventoProductoUpdated() throws JsonProcessingException {
        // Mock del evento
        ProductoDTO productoMock = new ProductoDTO(1L, "Producto Actualizado", BigDecimal.valueOf(150), 20);
        ProductEvent event = new ProductEvent(1L, "UPDATED", productoMock);
        String eventJson = new ObjectMapper().writeValueAsString(event);

        // Mock del CarritoItem
        CarritoItem itemMock = new CarritoItem();
        itemMock.productoId = 1L;
        itemMock.nombreProducto = "Producto Viejo";
        itemMock.precio = BigDecimal.valueOf(100);

        // Mock del repositorio
        when(carritoItemRepository.findByProductoId(1L)).thenReturn(List.of(itemMock));

        // Llamada al método
        carritoService.procesarEventoProducto(eventJson);

        // Verificaciones
        assertEquals("Producto Actualizado", itemMock.nombreProducto);
        assertEquals(BigDecimal.valueOf(150), itemMock.precio);
        verify(carritoItemRepository).persist(itemMock);
    }


    @Test
    @TestTransaction
    public void testProcesarEventoProductoDeleted() throws JsonProcessingException {
        // Mock del evento
        ProductEvent event = new ProductEvent(1L, "DELETED", null);
        String eventJson = new ObjectMapper().writeValueAsString(event);

        // Llamada al método
        carritoService.procesarEventoProducto(eventJson);

        // Verificación: se llama al repositorio para eliminar los ítems
        verify(carritoItemRepository).delete("productoId",1L);
    }
}
