package Unitario;

import Cliente.StockClient;
import DTO.CarritoItemDetalleDTO;
import DTO.ProductoDTO;
import Entidades.CarritoItem;
import Entidades.LineaPago;
import Entidades.OrdenPago;
import Entidades.OutboxEvent;
import DTO.ProductEventDTO;
import Repositorios.CarritoItemRepository;
import Repositorios.OrdenPagoRepository;
import Repositorios.OutboxEventRepository;
import Servicios.CarritoService;
import Servicios.StripeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

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

    @InjectMock
    StripeService stripeService;

    @InjectMock
    OutboxEventRepository outboxEventRepository;

    @BeforeEach
    public void setup() {
        System.setProperty("test.env", "true");
        MockitoAnnotations.openMocks(this);
    }

    private CarritoItem crearItem(Long productoId, int cantidad) {
        CarritoItem item = new CarritoItem();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);
        item.setUserId("user123");
        return item;
    }

    @Test
    void iniciarPago_conProductos() throws StripeException {
        String userId = "user123";
        List<CarritoItem> carrito = List.of(crearItem(1L, 2));
        ProductoDTO productoDTO = new ProductoDTO(1L, "Producto 1", BigDecimal.TEN, 10,"url");
        PaymentIntent mockIntent = new PaymentIntent();
        mockIntent.setId("pi_123");

        Mockito.when(carritoItemRepository.findByUserId(userId)).thenReturn(carrito);
        Mockito.when(stockClient.obtenerProductoPorId(1L)).thenReturn(productoDTO);
        //Mockito.when(stockClient.reservarStock(Map.of(1L, 2))).thenReturn(Response.ok().build());
        Mockito.when(stockClient.obtenerProductoPorId(Mockito.anyLong())).thenReturn(productoDTO);
        Mockito.when(stripeService.crearPago(Mockito.any())).thenReturn(mockIntent);

        OrdenPago orden = carritoService.iniciarPago(userId, "Calle Falsa", "123456","jwt");

        Assertions.assertEquals("CREADO", orden.getEstado());
        Assertions.assertEquals("pi_123", orden.getReferenciaExterna());
    }

    @Test
    void iniciarPago_montoCero() {
        String userId = "user123";
        List<CarritoItem> carrito = List.of(crearItem(1L, 2));
        ProductoDTO productoDTO = new ProductoDTO(1L, "Gratis", BigDecimal.ZERO, 10,"url");

        Mockito.when(carritoItemRepository.findByUserId(userId)).thenReturn(carrito);
        Mockito.when(stockClient.obtenerProductoPorId(1L)).thenReturn(productoDTO);
        //Mockito.when(stockClient.reservarStock(Map.of(1L, 2))).thenReturn(Response.ok().build());

        OrdenPago orden = carritoService.iniciarPago(userId, "Calle Gratis", "000","jwt");

        Assertions.assertEquals("COMPLETADO", orden.getEstado());
        Assertions.assertNull(orden.getReferenciaExterna());
    }

    @Test
    void iniciarPago_carritoVacio() {
        Mockito.when(carritoItemRepository.findByUserId("user123")).thenReturn(Collections.emptyList());

        WebApplicationException ex = Assertions.assertThrows(WebApplicationException.class, () ->
                carritoService.iniciarPago("user123", "Calle", "123","jwt"));

        Assertions.assertEquals(400, ex.getResponse().getStatus());
        Assertions.assertEquals("El carrito está vacío", ex.getMessage());
    }

    @Test
    void iniciarPago_productoNoEncontradoEnStock() {
        List<CarritoItem> carrito = List.of(crearItem(1L, 1));
        Mockito.when(carritoItemRepository.findByUserId("user123")).thenReturn(carrito);
        Mockito.when(stockClient.obtenerProductoPorId(1L)).thenReturn(null);

        WebApplicationException ex = Assertions.assertThrows(WebApplicationException.class, () ->
                carritoService.iniciarPago("user123", "Calle", "123","jwt"));

        Assertions.assertEquals(404, ex.getResponse().getStatus());
        Assertions.assertTrue(ex.getMessage().contains("Producto no encontrado"));
    }

    @Test
    public void testProcesarCompra() {
        // Arrange
        OrdenPago orden = new OrdenPago();
        orden.setId(20L);
        orden.setUserId("userABC");
        orden.setReferenciaExterna(null);

        LineaPago linea = new LineaPago(2L, 3);
        orden.setItemsComprados(List.of(linea));

        // Act
        carritoService.procesarCompra(orden);

        // Assert
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).persist(captor.capture());

        OutboxEvent evento = captor.getValue();
        assertEquals("Carrito", evento.getAggregateType());
        assertEquals("userABC", evento.getAggregateId());
        assertEquals("Carrito.CompraProcesada", evento.getEventType());

        // Confirmamos que el precio sea 0 en el evento generado
        assertTrue(evento.getPayload().contains("\"precio\":0"));
        assertTrue(evento.getPayload().contains("\"productoId\":2"));
    }

    @Test
    public void testProcesarPagoCompletado() {
        // Arrange
        OrdenPago orden = new OrdenPago();
        orden.setId(123L);
        orden.setUserId("usuario-xyz");
        orden.setEstado("PAGADO");
        orden.setItemsComprados(List.of(new LineaPago(1L, 2)));

        CarritoService spyService = spy(carritoService);
        doNothing().when(spyService).procesarCompra(orden);

        // Act
        spyService.procesarPagoCompletado(orden);

        // Assert
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).persist(captor.capture());

        OutboxEvent evento = captor.getValue();
        assertEquals("Carrito", evento.getAggregateType());
        assertEquals("usuario-xyz", evento.getAggregateId());
        assertEquals("Carrito.CompraProcesada", evento.getEventType());
        assertTrue(evento.getPayload().contains("\"ordenId\":123"));
        assertNotNull(evento.getCreatedAt());

        // Verificamos que se haya llamado a procesarCompra()
        verify(spyService, times(1)).procesarPagoCompletado(orden);
    }

    @Test
    void procesarPagoCancelado() {
        // Arrange
        OrdenPago orden = new OrdenPago();
        orden.setId(1L);
        orden.setEstado("PENDIENTE");

        LineaPago linea = new LineaPago();
        linea.setProductoId(1L);
        linea.setCantidad(2);

        orden.setItemsComprados(List.of(linea));

        // Act
        carritoService.procesarPagoCancelado(orden);

        // Assert
        verify(outboxEventRepository, times(1)).persist(any(OutboxEvent.class));
    }

    final String userId = "user123";
    final Long productoId = 1L;

    ProductoDTO productoConStock(int stock) {
        return new ProductoDTO(productoId, "Producto Test", BigDecimal.valueOf(10.0), stock,"url");
    }

    @Test
    void testProductoNoExisteEnStock() {
        Mockito.when(stockClient.obtenerProductoPorId(productoId))
                .thenReturn(null);

        WebApplicationException ex = Assertions.assertThrows(WebApplicationException.class, () ->
                carritoService.agregarProducto(userId, productoId, 1)
        );

        Assertions.assertEquals(404, ex.getResponse().getStatus());
        Assertions.assertTrue(ex.getMessage().contains("no existe"));
    }

    @Test
    void testStockInsuficienteParaAgregar() {
        ProductoDTO producto = productoConStock(1); // Solo 1 en stock

        Mockito.when(stockClient.obtenerProductoPorId(productoId))
                .thenReturn(producto);

        Mockito.when(carritoItemRepository.findByUserAndProducto(userId, productoId))
                .thenReturn(Optional.empty());

        WebApplicationException ex = Assertions.assertThrows(WebApplicationException.class, () ->
                carritoService.agregarProducto(userId, productoId, 2)
        );

        Assertions.assertEquals(400, ex.getResponse().getStatus());
        Assertions.assertTrue(ex.getMessage().contains("Stock insuficiente"));
    }



    @Test
    @TestTransaction
    public void testAgregarProductoNuevo() {
        // Mock del producto
        ProductoDTO productoMock = new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10,"url");
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
        ProductEventDTO event = new ProductEventDTO(1L, "DELETED", null);
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
        when(stockClient.obtenerProductoPorId(1L)).thenReturn(new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10,"url"));

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
        ProductoDTO productoMock = new ProductoDTO(1L, "Producto Test", BigDecimal.valueOf(100), 10,"url");
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
    public void testIniciarPagoCarritoVacio() {
        when(carritoItemRepository.findByUserId("user1")).thenReturn(List.of());

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            carritoService.iniciarPago("user1","Calle de ejemplo","2342233244","jwt");
        });

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("El carrito está vacío", exception.getMessage());
    }
}
