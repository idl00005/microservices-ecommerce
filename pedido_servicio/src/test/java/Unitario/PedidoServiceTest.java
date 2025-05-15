package Unitario;

import DTO.CarritoEventDTO;
import DTO.CarritoItemDTO;
import DTO.PedidoDTO;
import Entidades.Pedido;
import Repositorios.PedidoRepository;
import Servicios.PedidoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @InjectMocks
    private PedidoService pedidoService;        // <-- Mockito creará e inyectará aquí

    @Mock
    private PedidoRepository pedidoRepository;  // <-- Este mock será inyectado

    @Mock
    private ObjectMapper objectMapper;          // <-- Y este también

    @Test
    void crearPedido_deberiaGuardarPedido() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);

        Pedido result = pedidoService.crearPedido(pedido);

        verify(pedidoRepository).guardar(pedido);
        assertEquals(1L, result.getId());
    }

    @Test
    void obtenerPedidosPorUsuario_conPedidosExistentes() {
        Pedido p = new Pedido();
        p.setId(1L);
        p.setProductoId(2L);
        p.setCantidad(3);
        p.setEstado("PENDIENTE");
        p.setPrecioTotal(BigDecimal.TEN);

        when(pedidoRepository.buscarPorUsuarioId("usuario123"))
                .thenReturn(List.of(p));

        List<PedidoDTO> dtos = pedidoService.obtenerPedidosPorUsuario("usuario123");

        assertEquals(1, dtos.size());
        assertEquals(2L, dtos.get(0).productoId());
    }

    @Test
    void obtenerPedidosPorUsuario_sinPedidos_deberiaLanzarExcepcion() {
        when(pedidoRepository.buscarPorUsuarioId("usuario123"))
                .thenReturn(Collections.emptyList());

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> pedidoService.obtenerPedidosPorUsuario("usuario123")
        );

        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void obtenerPedidoPorId_conPedidoExistenteYUsuarioCorrecto() {
        Pedido p = new Pedido();
        p.setId(1L);
        p.setUsuarioId("usuario123");

        when(pedidoRepository.buscarPorId(1L)).thenReturn(p);

        Pedido result = pedidoService.obtenerPedidoPorId(1L, "usuario123");

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void obtenerPedidoPorId_noEncontrado() {
        when(pedidoRepository.buscarPorId(1L)).thenReturn(null);

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> pedidoService.obtenerPedidoPorId(1L, "usuario123")
        );
        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void obtenerPedidoPorId_usuarioIncorrecto() {
        Pedido p = new Pedido();
        p.setUsuarioId("otroUsuario");
        when(pedidoRepository.buscarPorId(1L)).thenReturn(p);

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> pedidoService.obtenerPedidoPorId(1L, "usuario123")
        );
        assertEquals(403, ex.getResponse().getStatus());
    }

    @Test
    void obtenerPedidoPorIdParaAdmin_existente() {
        Pedido p = new Pedido();
        p.setId(1L);
        when(pedidoRepository.buscarPorId(1L)).thenReturn(p);

        Pedido result = pedidoService.obtenerPedidoPorIdParaAdmin(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void obtenerPedidoPorIdParaAdmin_noExistente() {
        when(pedidoRepository.buscarPorId(1L)).thenReturn(null);

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> pedidoService.obtenerPedidoPorIdParaAdmin(1L)
        );
        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void listarPedidos_deberiaRetornarLista() {
        Pedido p = new Pedido();
        p.setId(1L);
        p.setProductoId(2L);
        p.setCantidad(3);
        p.setEstado("PENDIENTE");
        p.setPrecioTotal(BigDecimal.TEN);

        when(pedidoRepository.buscarPorEstadoYUsuarioConPaginacion(null, null, 0, 10))
                .thenReturn(List.of(p));

        List<PedidoDTO> dtos = pedidoService.listarPedidos(null, null, 1, 10);
        assertEquals(1, dtos.size());
    }

    @Test
    void procesarMensajeCarrito_deberiaCrearPedidos() throws JsonProcessingException {
        // Crear el objeto CarritoItemDTO
        CarritoItemDTO item = new CarritoItemDTO("usuario123", 2L, "Producto Test", BigDecimal.TEN, 3);

        // Crear el objeto CarritoEventDTO
        CarritoEventDTO carritoEvent = new CarritoEventDTO();
        carritoEvent.setUserId("usuario123");
        carritoEvent.setDireccion("Calle Falsa 123");
        carritoEvent.setTelefono("123456789");
        carritoEvent.setItems(List.of(item));

        // Configurar el mock de ObjectMapper
        String json = "{}"; // JSON de prueba
        when(objectMapper.readValue(json, CarritoEventDTO.class)).thenReturn(carritoEvent);

        // Llamar al método a probar
        pedidoService.procesarMensajeCarrito(json);

        // Capturar y verificar el pedido guardado
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository, times(1)).guardar(captor.capture());

        Pedido pedidoGuardado = captor.getValue();
        assertEquals("usuario123", pedidoGuardado.getUsuarioId());
        assertEquals(2L, pedidoGuardado.getProductoId());
    }

    @Test
    void procesarMensajeCarrito_mensajeInvalido_deberiaLanzarRuntimeException() throws JsonProcessingException {
        String badJson = "{ mal }";
        when(objectMapper.readValue(badJson, CarritoEventDTO.class))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("fail"){});
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> pedidoService.procesarMensajeCarrito(badJson)
        );
        assertTrue(ex.getMessage().contains("Error al procesar"));
    }

    @Test
    void actualizarPedido_existente_deberiaActualizar() {
        Pedido p = new Pedido();
        p.setId(1L);
        when(pedidoRepository.buscarPorId(1L)).thenReturn(p);

        pedidoService.actualizarPedido(1L, "ENVIADO");

        assertEquals("ENVIADO", p.getEstado());
        verify(pedidoRepository).actualizar(p);
    }

    @Test
    void actualizarPedido_noExistente_deberiaLanzarExcepcion() {
        when(pedidoRepository.buscarPorId(1L)).thenReturn(null);

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> pedidoService.actualizarPedido(1L, "ENVIADO")
        );
        assertEquals(404, ex.getResponse().getStatus());
    }
}
