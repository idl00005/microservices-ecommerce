package Unitario;

import Cliente.ProductoClient;
import DTO.ProductoDTO;
import Entidades.CarritoItem;
import Entidades.OrdenPago;
import Otros.ProductEvent;
import Repositorios.CarritoItemRepository;
import Servicios.CarritoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        itemMock.cantidad = 5;

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

    @Test
    @TestTransaction
    public void testObtenerCarrito() {
        // Mock del repositorio
        CarritoItem item = new CarritoItem();
        item.userId = "user1";
        item.productoId = 1L;
        item.nombreProducto = "Producto Test";
        item.precio = BigDecimal.valueOf(100);
        item.cantidad = 2;

        when(carritoItemRepository.findByUserId("user1")).thenReturn(List.of(item));

        // Llamada al método
        List<CarritoItem> carrito = carritoService.obtenerCarrito("user1");

        // Verificaciones
        assertNotNull(carrito);
        assertEquals(1, carrito.size());
        assertEquals("Producto Test", carrito.get(0).nombreProducto);
        verify(carritoItemRepository, times(1)).findByUserId("user1");
    }

    @Test
    @TestTransaction
    public void testEliminarProducto() {
        // Mock del repositorio
        CarritoItem item = new CarritoItem();
        item.userId = "user1";
        item.productoId = 1L;

        when(carritoItemRepository.findByUserAndProducto("user1", 1L)).thenReturn(Optional.of(item));
        doNothing().when(carritoItemRepository).delete(item);

        // Llamada al método
        carritoService.eliminarProducto("user1", 1L);

        // Verificaciones
        verify(carritoItemRepository, times(1)).findByUserAndProducto("user1", 1L);
        verify(carritoItemRepository, times(1)).delete(item);
    }

    @Test
    @TestTransaction
    public void testVaciarCarrito() {
        // Mock del repositorio: Simula el método delete
        doAnswer(invocation -> null).when(carritoItemRepository).delete("userId", "user1");

        // Llamada al método
        carritoService.vaciarCarrito("user1");

        // Verificaciones
        verify(carritoItemRepository, times(1)).delete("userId", "user1");
    }

    @Test
    @TestTransaction
    public void testActualizarCantidad() {
        // Mock del producto
        ProductoDTO productoMock = new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10);
        when(productoClient.obtenerProductoPorId(1L)).thenReturn(productoMock);

        // Mock del repositorio
        CarritoItem item = new CarritoItem();
        item.userId = "user1";
        item.productoId = 1L;
        item.nombreProducto = "Producto Test";
        item.precio = BigDecimal.valueOf(100);
        item.cantidad = 2;

        when(carritoItemRepository.findByUserAndProducto("user1", 1L)).thenReturn(Optional.of(item));

        // Llamada al método
        CarritoItem actualizado = carritoService.actualizarCantidadProducto("user1", 1L, 5);

        // Verificaciones
        assertNotNull(actualizado);
        assertEquals(5, actualizado.cantidad);
        verify(carritoItemRepository, times(1)).findByUserAndProducto("user1", 1L);
        verify(carritoItemRepository, times(1)).persist(item);
    }

    @Test
    @TestTransaction
    public void testIniciarPagoExitoso() {
        // Mock del carrito con productos
        CarritoItem item = new CarritoItem();
        item.userId = "user1";
        item.productoId = 1L;
        item.nombreProducto = "Producto Test";
        item.precio = BigDecimal.valueOf(100);
        item.cantidad = 2;

        when(carritoItemRepository.findByUserId("user1")).thenReturn(List.of(item));

        // Mock del servicio de Stripe
        OrdenPago ordenMock = new OrdenPago();
        ordenMock.userId = "user1";
        ordenMock.montoTotal = BigDecimal.valueOf(200);
        ordenMock.estado = "CREADO";

        // Llamada al método
        OrdenPago orden = carritoService.iniciarPago("user1");

        // Verificaciones
        assertNotNull(orden);
        assertEquals("CREADO", orden.estado);
        assertEquals(BigDecimal.valueOf(200), orden.montoTotal);
        verify(carritoItemRepository, times(1)).findByUserId("user1");
    }

    @Test
    @TestTransaction
    public void testIniciarPagoCarritoVacio() {
        when(carritoItemRepository.findByUserId("user1")).thenReturn(List.of());

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            carritoService.iniciarPago("user1");
        });

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("El carrito está vacío", exception.getMessage());
    }

    @Test
    @TestTransaction
    public void testIniciarPagoMontoCero() {
        // Mock del carrito con productos gratuitos
        CarritoItem item = new CarritoItem();
        item.userId = "user1";
        item.productoId = 1L;
        item.nombreProducto = "Producto Gratis";
        item.precio = BigDecimal.ZERO;
        item.cantidad = 1;

        when(carritoItemRepository.findByUserId("user1")).thenReturn(List.of(item));

        // Llamada al método
        OrdenPago orden = carritoService.iniciarPago("user1");

        // Verificaciones
        assertNotNull(orden);
        assertEquals("COMPLETADO", orden.estado);
        assertEquals(BigDecimal.ZERO, orden.montoTotal);
        verify(carritoItemRepository, times(1)).findByUserId("user1");
    }
}
