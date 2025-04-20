package Servicios;

import Entidades.Pedido;
import Repositorios.PedidoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    public List<Pedido> obtenerPedidosPorUsuario(String usuarioId) {
        return pedidoRepository.buscarPorUsuarioId(usuarioId);
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

                pedido.setTelefono("123456789"); // Ejemplo de valor predeterminado
                pedido.setEstado("PENDIENTE");  // Estado inicial
                pedido.setDireccion("Dirección predeterminada"); // Dirección predeterminada
                pedido.setPedidoId((long) 12.00); // Generar un ID único

                // Guardar el pedido
                pedidoRepository.guardar(pedido);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al procesar el mensaje del carrito", e);
        }
    }
}
