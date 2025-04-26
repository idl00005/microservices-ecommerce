package Servicios;

import DTO.PedidoDTO;
import Entidades.Pedido;
import Repositorios.PedidoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import DTO.CarritoEventDTO;
import DTO.CarritoItemDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PedidoService {

    @Inject
    PedidoRepository pedidoRepository;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public Pedido crearPedido(Pedido pedido) {
        pedidoRepository.guardar(pedido);
        return pedido;
    }

    @Transactional
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
    public Pedido obtenerPedidoPorId(Long id, String usuarioId) {
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
    public Pedido obtenerPedidoPorIdParaAdmin(Long id) {
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
    public void procesarMensajeCarrito(String mensajeCarrito) {
        try {
            // Parsear el mensaje recibido
            CarritoEventDTO carritoEvent = objectMapper.readValue(mensajeCarrito, CarritoEventDTO.class);

            // Crear un pedido por cada tipo de producto
            for (CarritoItemDTO item : carritoEvent.getItems()) {
                Pedido pedido = new Pedido();
                pedido.setUsuarioId(carritoEvent.getUserId());
                pedido.setProductoId(item.getProductoId());
                pedido.setCantidad(item.getCantidad());
                pedido.setPrecioTotal(item.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));
                pedido.setFechaCreacion(LocalDateTime.now());

                pedido.setTelefono(carritoEvent.getTelefono());
                pedido.setEstado("PENDIENTE");  // Estado inicial
                pedido.setDireccion(carritoEvent.getDireccion());

                // Guardar el pedido
                pedidoRepository.guardar(pedido);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al procesar el mensaje del carrito", e);
        }
    }

    @Transactional
    public void actualizarPedido(Long id, String estado) {
        Pedido pedidoExistente = pedidoRepository.buscarPorId(id);
        if (pedidoExistente == null) {
            throw new WebApplicationException("Pedido no encontrado", 404);
        }
        pedidoExistente.setEstado(estado);
        pedidoRepository.actualizar(pedidoExistente);
    }
}
