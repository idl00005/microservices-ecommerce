package Unitario;

import Cliente.StockClient;
import DTO.CarritoItemDetalleDTO;
import DTO.ProductoDTO;
import Entidades.CarritoItem;
import Entidades.OrdenPago;
import Otros.ProductEvent;
import Repositorios.CarritoItemRepository;
import Repositorios.OrdenPagoRepository;
import Servicios.CarritoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CarritoServiceTest {

    @InjectMock
    CarritoItemRepository carritoItemRepository;

    @Inject
    CarritoService carritoService;

    @InjectMock
    OrdenPagoRepository ordenPagoRepository;

    @InjectMock
    StockClient stockClient;

    @BeforeEach
    public void setup() {
        System.setProperty("test.env", "true");
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @TestTransaction
    public void testAgregarProductoNuevo() {
        // Mock del producto
        ProductoDTO productoMock = new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10);
        when(stockClient.obtenerProductoPorId(1L)).thenReturn(productoMock);

        // Mock del repositorio
        doNothing().when(carritoItemRepository).persist(any(CarritoItem.class));

        // Llamada al método
        CarritoItemDetalleDTO result = carritoService.agregarProducto("user1", 1L, 2);

        // Verificaciones
        assertNotNull(result);
        assertEquals(2, result.cantidad());
        verify(stockClient, times(1)).obtenerProductoPorId(1L);
        verify(carritoItemRepository, times(1)).persist(any(CarritoItem.class));
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
        item.setUserId("user1");
        item.setProductoId(1L);
        item.setCantidad(2);

        when(carritoItemRepository.findByUserId("user1")).thenReturn(List.of(item));
        when(stockClient.obtenerProductoPorId(1L)).thenReturn(new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10));

        // Llamada al método
        List<CarritoItemDetalleDTO> carrito = carritoService.obtenerCarrito("user1");

        // Verificaciones
        assertNotNull(carrito);
        assertEquals(1, carrito.size());
        verify(carritoItemRepository, times(1)).findByUserId("user1");
    }

    @Test
    @TestTransaction
    public void testEliminarProducto() {
        // Mock del repositorio
        CarritoItem item = new CarritoItem();
        item.setUserId("user1");
        item.setProductoId(1L);
        item.setCantidad(2);

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
        when(stockClient.obtenerProductoPorId(1L)).thenReturn(productoMock);

        // Mock del repositorio
        CarritoItem item = new CarritoItem();
        item.setUserId("user1");
        item.setProductoId(1L);
        item.setCantidad(2);

        when(carritoItemRepository.findByUserAndProducto("user1", 1L)).thenReturn(Optional.of(item));

        // Llamada al método
        CarritoItem actualizado = carritoService.actualizarCantidadProducto("user1", 1L, 5);

        // Verificaciones
        assertNotNull(actualizado);
        assertEquals(5, actualizado.getCantidad());
        verify(carritoItemRepository, times(1)).findByUserAndProducto("user1", 1L);
        verify(carritoItemRepository, times(1)).persist(item);
    }

    @Test
    @TestTransaction
    public void testIniciarPagoExitoso() {
        // Mock del carrito con productos
        CarritoItem item = new CarritoItem();
        item.setUserId("user1");
        item.setProductoId(1L);
        item.setCantidad(2);

        when(carritoItemRepository.findByUserId("user1")).thenReturn(List.of(item));

        // Mock del cliente de stock
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(200);
        when(stockClient.reservarStock(any())).thenReturn(mockResponse);
        when(stockClient.obtenerProductoPorId(1L)).thenReturn(new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10));

        // Mock del servicio de Stripe
        OrdenPago ordenMock = new OrdenPago();
        ordenMock.setUserId("user1");
        ordenMock.setMontoTotal(BigDecimal.valueOf(200));
        ordenMock.setEstado("CREADO");

        // Llamada al método
        OrdenPago orden = carritoService.iniciarPago("user1", "Calle tralala", "6834345454");

        // Verificaciones
        assertNotNull(orden);
        assertEquals("CREADO", orden.getEstado());
        assertEquals(BigDecimal.valueOf(200), orden.getMontoTotal());
        verify(carritoItemRepository, times(1)).findByUserId("user1");
        verify(stockClient, times(1)).reservarStock(any());
    }

    @Test
    @TestTransaction
    public void testIniciarPagoCarritoVacio() {
        when(carritoItemRepository.findByUserId("user1")).thenReturn(List.of());

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            carritoService.iniciarPago("user1","Calle de ejemplo","2342233244");
        });

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("El carrito está vacío", exception.getMessage());
    }

    @Test
    @TestTransaction
    public void testProcesarPagoCompletado_CambiaEstadoCorrectamente() {
        Long ordenId = 99L;
        OrdenPago orden = new OrdenPago();
        orden.setId(ordenId);
        orden.setEstado("PAGADO");
        orden.setUserId("user1");
        CarritoItem item = new CarritoItem();
        item.setProductoId(1L);
        item.setCantidad(2);
        List<CarritoItem> items = List.of(item);

        // Mock del repositorio
        when(ordenPagoRepository.findById(ordenId)).thenReturn(orden);
        when(carritoItemRepository.findByUserId(orden.getUserId())).thenReturn(items);

        // Llamada al método
        carritoService.pagoCostoMayorQue0(orden.getId());

        // Verificaciones
        assertEquals("COMPLETADO", orden.getEstado());
    }

    @Test
    @TestTransaction
    public void testProcesarPagoCompletado_OrdenNoExiste() {
        when(ordenPagoRepository.findById(100L)).thenReturn(null);

        // No lanza excepción, simplemente ignora
        carritoService.pagoCostoMayorQue0(100L);

        verify(ordenPagoRepository, never()).persist((OrdenPago) any());
    }
}
