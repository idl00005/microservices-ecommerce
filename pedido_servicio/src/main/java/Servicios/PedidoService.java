package Servicios;

import DTO.PedidoDTO;
import DTO.ValoracionDTO;
import Entidades.OutboxEvent;
import Entidades.Pedido;
import Repositorios.OutboxEventRepository;
import Repositorios.PedidoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import DTO.CarritoEventDTO;
import DTO.CarritoItemDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PedidoService {

    @Inject
    public PedidoRepository pedidoRepository;

    @Inject
    OutboxEventRepository outboxEventRepository;

    @Inject
    public ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    @Transactional
    public Pedido crearPedido(Pedido pedido) {
        pedidoRepository.guardar(pedido);
        return pedido;
    }

    @Transactional
    @CacheResult(cacheName = "pedidos-por-usuario-cache")
    public List<PedidoDTO> obtenerPedidosPorUsuario(String usuarioId) {
        List<Pedido> pedidos = pedidoRepository.buscarPorUsuarioId(usuarioId);
        List<PedidoDTO> pedidosDTO = pedidos.stream()
                .map(p -> new PedidoDTO(p.getId(), p.getProductoId(), p.getCantidad(),p.getEstado(),p.getPrecioTotal().doubleValue()))
                .toList();
        if (pedidosDTO.isEmpty()) {
            throw new WebApplicationException("No se encontraron pedidos para el usuario", 404);
        }
        return pedidosDTO;
    }

    @Transactional
    @CacheResult(cacheName = "pedidos-por-id-cache")
    public Pedido obtenerPedidoPorId(@CacheKey Long id, String usuarioId) {
        Pedido pedido = pedidoRepository.buscarPorId(id);
        if(pedido != null && pedido.getUsuarioId().equals(usuarioId)) {
            return pedido;
        } else if (pedido == null) {
            throw new WebApplicationException("Pedido no encontrado", 404);
        } else {
            throw new WebApplicationException("No tienes permiso para acceder a este pedido", 403);
        }
    }

    @Transactional
    @CacheResult(cacheName = "pedidos-por-id-cache")
    public Pedido obtenerPedidoPorIdParaAdmin(@CacheKey Long id) {
        Pedido pedido = pedidoRepository.buscarPorId(id);
        if (pedido == null) {
            throw new WebApplicationException("Pedido no encontrado", 404);
        }
        return pedido;
    }

    @Transactional
    public List<PedidoDTO> listarPedidos(String estado, String usuarioId, Integer pagina, Integer tamanio) {
        // Valores predeterminados
        int paginaDefecto = (pagina == null || pagina < 1) ? 1 : pagina;
        int tamanioDefecto = (tamanio == null || tamanio < 1) ? 10 : tamanio;

        int offset = (paginaDefecto - 1) * tamanioDefecto;

        // Si estado o usuarioId son null, se ignoran en el filtro
        List<Pedido> pedidos = pedidoRepository.buscarPorEstadoYUsuarioConPaginacion(estado, usuarioId, offset, tamanioDefecto);
        return pedidos.stream()
                .map(p -> new PedidoDTO(p.getId(), p.getProductoId(), p.getCantidad(), p.getEstado(), p.getPrecioTotal().doubleValue()))
                .toList();
    }

    @Incoming("productos-in")
    @Transactional
    public Uni<Void> procesarMensajeCarrito(Message<String> msg) {
        String mensaje = msg.getPayload();
        log.info("Mensaje recibido: {}", mensaje);

        CarritoEventDTO carritoEvent;
        try {
            carritoEvent = objectMapper.readValue(mensaje, CarritoEventDTO.class);
        } catch (JsonProcessingException e) {
            log.error("JSON inválido, descartando mensaje", e);
            return Uni.createFrom().completionStage(msg.ack());
        }

        // Validaciones de evento
        if (carritoEvent.getItems() == null || carritoEvent.getItems().isEmpty()) {
            log.warn("Carrito vacío. Orden descartada. ordenId={}", carritoEvent.getOrdenId());
            return Uni.createFrom().completionStage(msg.ack());
        }
        for (CarritoItemDTO item : carritoEvent.getItems()) {
            if (item.productoId() == null || item.cantidad() == null || item.precio() == null) {
                log.error("Ítem inválido en ordenId={}, item={}", carritoEvent.getOrdenId(), item);
                return Uni.createFrom().completionStage(msg.ack());
            }
        }

        // Procesamiento normal
        return Uni.createFrom().item(carritoEvent)
                .onItem().invoke(event -> {
                    for (CarritoItemDTO item : event.getItems()) {
                        Pedido pedido = new Pedido();
                        pedido.setUsuarioId(event.getUserId());
                        pedido.setProductoId(item.productoId());
                        pedido.setCantidad(item.cantidad());
                        pedido.setPrecioTotal(item.precio().multiply(
                                BigDecimal.valueOf(item.cantidad())));
                        pedido.setFechaCreacion(LocalDateTime.now());
                        pedido.setOrdenId(event.getOrdenId());
                        pedido.setEstado("PENDIENTE");
                        pedidoRepository.guardar(pedido);
                    }
                })
                // Siempre ack al final, tanto si todo fue bien como si Mutiny captura una excepción
                .onItem().transformToUni(x -> Uni.createFrom().completionStage(msg.ack()))
                .onFailure().recoverWithUni(err -> {
                    // Loguea el error, pero como failure-strategy=ignore, lo descartas
                    log.error("Error procesando mensaje, lo descarto", err);
                    return Uni.createFrom().completionStage(msg.ack());
                });
    }


    @Transactional
    public void actualizarPedido(Long id, String estado) {
        Pedido pedidoExistente = pedidoRepository.buscarPorId(id);
        invalidarCachePedidoPorId(id);
        invalidarCachePedidoPorUsuario(pedidoExistente.getUsuarioId());
        if (pedidoExistente == null) {
            throw new WebApplicationException("Pedido no encontrado", 404);
        }
        pedidoExistente.setEstado(estado);
        pedidoRepository.actualizar(pedidoExistente);
    }

    @CacheInvalidate(cacheName = "pedidos-por-id-cache")
    public void invalidarCachePedidoPorId(Long id) {}

    @CacheInvalidate(cacheName = "pedidos-por-usuario-cache")
    public void invalidarCachePedidoPorUsuario(@CacheKey String usuarioId) {}

    @Transactional
    public void crearValoracion(Long pedidoId, String usuarioId, int puntuacion, String comentario) throws JsonProcessingException {
        Pedido pedido = pedidoRepository.buscarPorId(pedidoId);

        if (pedido == null) {
            throw new WebApplicationException("Pedido no encontrado", 404);
        }

        if (!pedido.getUsuarioId().equals(usuarioId)) {
            throw new WebApplicationException("No tienes permiso para valorar este pedido", 403);
        }

        if (!"COMPLETADO".equals(pedido.getEstado())) {
            throw new WebApplicationException("Solo se pueden valorar pedidos completados", 400);
        }

        // Crear la valoración
        ValoracionDTO valoracionDTO = new ValoracionDTO(usuarioId, pedido.getProductoId(), puntuacion, comentario);

        // Crear el evento de valoración
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.aggregateId = pedidoId.toString();
        outboxEvent.aggregateType = "Pedido";
        outboxEvent.eventType =  "ValoracionCreada";
        outboxEvent.payload =  objectMapper.writeValueAsString(valoracionDTO);
        outboxEvent.status = OutboxEvent.Status.PENDING;

        // Persistir el evento en la tabla outbox
        outboxEventRepository.persist(outboxEvent);
    }
}
