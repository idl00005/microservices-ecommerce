package Unitario;

import DTO.NuevoPedidoEventDTO;
import DTO.CarritoItemDTO;
import DTO.PedidoDTO;
import Entidades.OutboxEvent;
import Entidades.Pedido;
import Repositorios.OutboxEventRepository;
import Repositorios.PedidoRepository;
import Servicios.PedidoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @InjectMocks
    private PedidoService pedidoService;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        pedidoService.objectMapper = objectMapper;  // asignar ObjectMapper real
    }

    @Test
    void crearPedido() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);

        Pedido result = pedidoService.crearPedido(pedido);

        verify(pedidoRepository).guardar(pedido);
        assertEquals(1L, result.getId());
    }

    @Test
    public void obtenerPedidoPorIdUsuario() {
        // Arrange
        Long pedidoId = 1L;
        String usuarioId = "user123";

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setUsuarioId(usuarioId);

        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(pedido);

        // Act
        Pedido resultado = pedidoService.obtenerPedidoPorIdParaUsuario(pedidoId, usuarioId);

        // Assert
        assertNotNull(resultado);
        assertEquals(pedidoId, resultado.getId());
        assertEquals(usuarioId, resultado.getUsuarioId());
        verify(pedidoRepository).buscarPorId(pedidoId);
    }

    @Test
    public void obtenerPedidoPorIdUsuario_PedidoNoExiste() {
        // Arrange
        Long pedidoId = 2L;
        String usuarioId = "user123";

        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(null);

        // Act & Assert
        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> pedidoService.obtenerPedidoPorIdParaUsuario(pedidoId, usuarioId));
        assertEquals(404, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("Pedido no encontrado"));

        verify(pedidoRepository).buscarPorId(pedidoId);
    }

    @Test
    public void obtenerPedidoPorIdUsuario_PedidoNoEsDelUsuario() {
        // Arrange
        Long pedidoId = 3L;
        String usuarioId = "user123";

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setUsuarioId("otroUsuario");

        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(pedido);

        // Act & Assert
        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> pedidoService.obtenerPedidoPorIdParaUsuario(pedidoId, usuarioId));
        assertEquals(403, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("No tienes permiso"));

        verify(pedidoRepository).buscarPorId(pedidoId);
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
    public void listarPedidos_deberiaRetornarListaPaginadaFiltrada() {
        // Arrange
        String estado = "COMPLETADO";
        String usuarioId = "user123";
        int pagina = 2;
        int tamanio = 3;

        Pedido pedido1 = new Pedido();
        pedido1.setId(10L);
        pedido1.setUsuarioId(usuarioId);
        pedido1.setEstado(estado);
        pedido1.setProductoId(1L);
        pedido1.setCantidad(2);
        pedido1.setPrecioTotal(BigDecimal.valueOf(100));
        Pedido pedido2 = new Pedido();
        pedido2.setId(11L);
        pedido2.setUsuarioId(usuarioId);
        pedido2.setEstado(estado);
        pedido2.setProductoId(2L);
        pedido2.setCantidad(1);
        pedido2.setPrecioTotal(BigDecimal.valueOf(50));

        List<Pedido> pedidos = List.of(
                pedido1,
                pedido2
        );

        int offsetEsperado = (pagina - 1) * tamanio;

        when(pedidoRepository.buscarPorEstadoYUsuarioConPaginacion(estado, usuarioId, offsetEsperado, tamanio))
                .thenReturn(pedidos);

        // Act
        List<PedidoDTO> resultado = pedidoService.listarPedidos(estado, usuarioId, pagina, tamanio);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(10L, resultado.get(0).id());
        assertEquals(11L, resultado.get(1).id());

        verify(pedidoRepository).buscarPorEstadoYUsuarioConPaginacion(estado, usuarioId, offsetEsperado, tamanio);
    }

    @Test
    void procesarMensajeCarrito() throws JsonProcessingException {
        // Crear el objeto CarritoItemDTO
        CarritoItemDTO item = new CarritoItemDTO(2L, 3, BigDecimal.valueOf(10.0));

        // Crear el objeto CarritoEventDTO con ítems válidos
        NuevoPedidoEventDTO carritoEvent = new NuevoPedidoEventDTO();
        carritoEvent.setUserId("usuario123");
        carritoEvent.setItems(List.of(item));

        // Configurar el mock de ObjectMapper para devolver el objeto simulado
        String json = "{\"userId\":\"usuario123\",\"items\":[{\"productoId\":2,\"cantidad\":3,\"precio\":10.0}]}";
        Message<String> msg = Message.of(json);
        pedidoService.procesarMensajeCarrito(msg).await().indefinitely();

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);

        // Capturar y verificar el pedido guardado
        verify(pedidoRepository, times(1)).guardar(captor.capture());

        Pedido pedidoGuardado = captor.getValue();
        assertEquals("usuario123", pedidoGuardado.getUsuarioId());
        assertEquals(2L, pedidoGuardado.getProductoId());
        assertEquals(BigDecimal.valueOf(30.0), pedidoGuardado.getPrecioTotal()); // 10.0 * 3
    }

    @Test
    public void actualizarPedido() {
        // Arrange
        Long pedidoId = 1L;
        String nuevoEstado = "COMPLETADO";

        Pedido pedidoExistente = new Pedido();
        pedidoExistente.setId(pedidoId);
        pedidoExistente.setEstado("PENDIENTE");

        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(pedidoExistente);

        // Para el método invalidarCachePedidoPorId, usamos spy para verificar llamada
        PedidoService spyService = Mockito.spy(pedidoService);
        spyService.pedidoRepository = pedidoRepository;  // inyectar mock

        // Act
        spyService.actualizarPedido(pedidoId, nuevoEstado);

        // Assert
        assertEquals(nuevoEstado, pedidoExistente.getEstado());
        verify(pedidoRepository).actualizar(pedidoExistente);
        verify(spyService).invalidarCachePedidoPorId(pedidoId);
    }

    @Test
    public void actualizarPedido_PedidoNoExiste() {
        // Arrange
        Long pedidoId = 999L;
        String nuevoEstado = "COMPLETADO";

        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(null);

        // Act & Assert
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> {
            pedidoService.actualizarPedido(pedidoId, nuevoEstado);
        });

        assertEquals(404, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("Pedido no encontrado"));
    }

    @Test
    public void crearValoracion_deberiaCrearValoracionCorrectamente() throws Exception {
        Long pedidoId = 1L;
        String usuarioId = "user1";
        int puntuacion = 5;
        String comentario = "Muy bueno";
        String jwtToken = "token";

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setUsuarioId(usuarioId);
        pedido.setEstado("COMPLETADO");
        pedido.setProductoId(10L);

        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(pedido);

        // Llamar al método a testear
        pedidoService.crearValoracion(pedidoId, usuarioId, puntuacion, comentario, jwtToken);

        // Verificar que se persiste un evento outbox con la info correcta
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).persist(eventCaptor.capture());

        OutboxEvent evento = eventCaptor.getValue();
        assertEquals(pedidoId.toString(), evento.aggregateId);
        assertEquals("Pedido", evento.aggregateType);
        assertEquals("ValoracionCreada", evento.eventType);
        assertEquals(OutboxEvent.Status.PENDING, evento.status);

        // Comprobar que el payload contiene los datos esperados (usuarioId, productoId, puntuacion, comentario)
        String payload = evento.payload;
        assertTrue(payload.contains(usuarioId));
        assertTrue(payload.contains(pedido.getProductoId().toString()));
        assertTrue(payload.contains(String.valueOf(puntuacion)));
        assertTrue(payload.contains(comentario));
    }

    @Test
    public void crearValoracion_PedidoNoExiste() {
        Long pedidoId = 1L;
        String usuarioId = "user1";

        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(null);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> {
            pedidoService.crearValoracion(pedidoId, usuarioId, 5, "Comentario", "token");
        });

        assertEquals(404, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("Pedido no encontrado"));
    }

    @Test
    public void crearValoracion_UsuarioNoEsPropietario() {
        Long pedidoId = 1L;
        String usuarioId = "user1";

        Pedido pedido = new Pedido();
        pedido.setUsuarioId("otroUsuario");
        pedido.setEstado("COMPLETADO");

        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(pedido);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> {
            pedidoService.crearValoracion(pedidoId, usuarioId, 5, "Comentario", "token");
        });

        assertEquals(403, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("No tienes permiso para valorar este pedido"));
    }

    @Test
    public void crearValoracion_EstadoNoEsCompletado() {
        Long pedidoId = 1L;
        String usuarioId = "user1";

        Pedido pedido = new Pedido();
        pedido.setUsuarioId(usuarioId);
        pedido.setEstado("PENDIENTE"); // No completado

        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(pedido);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> {
            pedidoService.crearValoracion(pedidoId, usuarioId, 5, "Comentario", "token");
        });

        assertEquals(400, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("Solo se pueden valorar pedidos completados"));
    }

}
