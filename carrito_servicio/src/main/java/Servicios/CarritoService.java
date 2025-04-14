package Servicios;

import Cliente.ProductoClient;
import DTO.ProductoDTO;
import Entidades.CarritoItem;
import Otros.ProductEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CarritoService {

    @Inject
    ProductoClient productoClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(CarritoService.class);

    @Transactional
    public CarritoItem agregarProducto(String userId, Integer productoId, int cantidad) {
        ProductoDTO producto = productoClient.obtenerProductoPorId(productoId);

        // Validar stock disponible
        if (producto.stock() < cantidad) {
            throw new WebApplicationException("Stock insuficiente para el producto: " + producto.nombre(), 400);
        }

        // Verificar si el producto ya está en el carrito del usuario
        Optional<CarritoItem> itemExistente = CarritoItem.find("userId = ?1 and productoId = ?2", userId, productoId).firstResultOptional();

        if (itemExistente.isPresent()) {
            CarritoItem item = itemExistente.get();
            item.cantidad += cantidad; // Incrementar la cantidad
            item.persist();
            return item;
        }

        // Si no existe, crear un nuevo registro
        CarritoItem nuevoItem = new CarritoItem();
        nuevoItem.userId = userId;
        nuevoItem.productoId = producto.id();
        nuevoItem.nombreProducto = producto.nombre();
        nuevoItem.precio = producto.precio();
        nuevoItem.cantidad = cantidad;
        nuevoItem.persist();

        return nuevoItem;
    }

    @Transactional
    public List<CarritoItem> obtenerCarrito(String userId) {
        List<CarritoItem> carrito = CarritoItem.list("userId", userId);

        for (CarritoItem item : carrito) {
            ProductoDTO producto = productoClient.obtenerProductoPorId(Integer.parseInt(item.productoId));

            if (item.cantidad > producto.stock()) {
                item.cantidad = producto.stock();
            }

            item.persist();
        }

        return carrito;
    }

    @Incoming("productos-in")
    @Transactional
    public void procesarEventoProducto(String eventJson) throws JsonProcessingException {
        ProductEvent event = parseEvent(eventJson);
        LOGGER.info("Iniciando operación...");

        if ("UPDATED".equals(event.getAction())) {
            actualizarProductoEnCarritos(event.getProductId(), event.getProducto());
        } else if ("DELETED".equals(event.getAction())) {
            eliminarProductoDeCarritos(event.getProductId());
        }
    }

    private ProductEvent parseEvent(String eventJson) throws JsonProcessingException {
        System.out.println("Recibido: " + eventJson);
        // Aquí deberías usar un deserializador como Jackson
        return new ObjectMapper().readValue(eventJson, ProductEvent.class);
    }

    @Transactional
    public void actualizarProductoEnCarritos(String productId, ProductoDTO producto) {
        List<CarritoItem> items = CarritoItem.list("productoId", productId);
        for (CarritoItem item : items) {
            item.nombreProducto = producto.nombre();
            item.precio = producto.precio();
            item.persist();
        }
    }

    @Transactional
    public void eliminarProductoDeCarritos(String productId) {
        CarritoItem.delete("productoId", productId);
    }
}

