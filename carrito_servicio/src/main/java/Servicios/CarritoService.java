package Servicios;

import Cliente.ProductoClient;
import DTO.CarritoEventDTO;
import DTO.ProductoDTO;
import DTO.ReducirStockDTO;
import Entidades.CarritoItem;
import Entidades.OrdenPago;
import Entidades.OutboxEvent;
import Otros.ProductEvent;
import Repositorios.CarritoItemRepository;
import Repositorios.OrdenPagoRepository;
import Repositorios.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbBuilder;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class CarritoService {

    @Inject
    ProductoClient productoClient;

    @Inject
    CarritoItemRepository carritoItemRepository;


    private static final Logger LOGGER = LoggerFactory.getLogger(CarritoService.class);

    @Inject
    StripeService stripeService;

    @Inject
    OrdenPagoRepository ordenPagoRepository;

    @Inject
    OutboxEventRepository outboxRepo;

    @Transactional
    public OrdenPago iniciarPago(String userId, String direccion, String telefono) {
        List<CarritoItem> carrito = carritoItemRepository.findByUserId(userId);
        if (carrito.isEmpty()) {
            throw new WebApplicationException("El carrito está vacío", 400);
        }

        BigDecimal total = carrito.stream()
                .map(item -> item.precio.multiply(BigDecimal.valueOf(item.cantidad)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrdenPago orden = new OrdenPago();
        orden.userId = userId;
        orden.montoTotal = total;
        orden.estado = "PENDIENTE";
        orden.fechaCreacion = LocalDateTime.now();
        ordenPagoRepository.persist(orden);

        if(orden.montoTotal.compareTo(BigDecimal.ZERO) == 0) {
            orden.estado = "PAGADO";
            procesarCompra(userId,direccion,telefono);
            return orden;
        } else {
            try {
                // Crear el pago en Stripe
                PaymentIntent paymentIntent = stripeService.crearPago(orden);
                orden.referenciaExterna = paymentIntent.getId();
                orden.proveedor = "Stripe";
                orden.estado = "CREADO";
            } catch (StripeException e) {
                throw new WebApplicationException("Error al procesar el pago: " + e.getMessage(), 500);
            }
        }

        return orden;
    }

    @Transactional
    public void procesarCompra(String userId, String direccion, String telefono) {
        List<CarritoItem> carrito = carritoItemRepository.findByUserId(userId);

        System.out.println("Enviando pedido...");//

        // Crear el evento para el servicio de pedidos
        CarritoEventDTO carritoEvent = new CarritoEventDTO();
        carritoEvent.setUserId(userId);
        carritoEvent.setItems(carrito);
        carritoEvent.setDireccion(direccion);
        carritoEvent.setTelefono(telefono);
        String payloadJson = JsonbBuilder.create().toJson(carritoEvent);
        OutboxEvent evt = new OutboxEvent();
        evt.aggregateType = "Carrito";
        evt.aggregateId = userId;
        evt.eventType = "Carrito.CompraProcesada";
        evt.payload = payloadJson;
        outboxRepo.persist(evt);

        // Crear el evento para el servicio de catálogo
        ReducirStockDTO compraEvent = new ReducirStockDTO();
        compraEvent.setUserId(userId);
        Map<Long, Integer> productosComprados = carrito.stream()
                .collect(Collectors.toMap(item -> item.productoId, item -> item.cantidad));
        compraEvent.setProductosComprados(productosComprados);

        // Enviar el evento al servicio de catálogo
        String catalogoPayload = JsonbBuilder.create().toJson(compraEvent);
        OutboxEvent catalogoEvent = new OutboxEvent();
        catalogoEvent.aggregateType = "Catalogo";
        catalogoEvent.aggregateId = userId;
        catalogoEvent.eventType = "Catalogo.ActualizarStock";
        catalogoEvent.payload = catalogoPayload;
        outboxRepo.persist(catalogoEvent);

        // Vaciar el carrito
        carritoItemRepository.delete("userId", userId);
    }

    @Transactional
    public CarritoItem agregarProducto(String userId, Long productoId, int cantidad) {
        ProductoDTO producto = productoClient.obtenerProductoPorId(productoId);

        // Validar stock disponible
        if (producto.stock() < cantidad) {
            throw new WebApplicationException("Stock insuficiente para el producto: " + producto.nombre(), 400);
        }

        // Verificar si el producto ya está en el carrito del usuario
        Optional<CarritoItem> itemExistente = carritoItemRepository.findByUserAndProducto(userId, productoId);

        if (itemExistente.isPresent()) {
            CarritoItem item = itemExistente.get();
            item.cantidad += cantidad; // Incrementar la cantidad
            if (item.cantidad > producto.stock()) {
                throw new WebApplicationException("Stock insuficiente para el producto: " + producto.nombre(), 400);
            }
            carritoItemRepository.persist(item);
            return item;
        }

        // Si no existe, crear un nuevo registro
        CarritoItem nuevoItem = new CarritoItem();
        nuevoItem.userId = userId;
        nuevoItem.productoId = producto.id();
        nuevoItem.nombreProducto = producto.nombre();
        nuevoItem.precio = producto.precio();
        nuevoItem.cantidad = cantidad;
        carritoItemRepository.persist(nuevoItem);

        return nuevoItem;
    }

    @Transactional
    public List<CarritoItem> obtenerCarrito(String userId) {
        return carritoItemRepository.findByUserId(userId);
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
        return new ObjectMapper().readValue(eventJson, ProductEvent.class);
    }

    @Transactional
    public void actualizarProductoEnCarritos(long productId, ProductoDTO producto) {
        List<CarritoItem> items = carritoItemRepository.findByProductoId(productId);
        for (CarritoItem item : items) {
            item.nombreProducto = producto.nombre();
            item.precio = producto.precio();
            if(item.cantidad > producto.stock()) {
                item.cantidad = producto.stock();
            }
            carritoItemRepository.persist(item);
        }
    }

    @Transactional
    public void eliminarProductoDeCarritos(long productId) {
        carritoItemRepository.delete("productoId", productId);
    }

    @Transactional
    public void eliminarProducto(String userId, Long productoId) {
        Optional<CarritoItem> item = carritoItemRepository.findByUserAndProducto(userId, productoId);
        if (item.isEmpty()) {
            throw new WebApplicationException("Producto no encontrado en el carrito", Response.Status.NOT_FOUND);
        }
        carritoItemRepository.delete(item.get());
    }

    @Transactional
    public void vaciarCarrito(String userId) {
        carritoItemRepository.delete("userId", userId);
    }

    @Transactional
    public CarritoItem actualizarCantidadProducto(String userId, Long productoId, int nuevaCantidad) {
        Optional<CarritoItem> item = carritoItemRepository.findByUserAndProducto(userId, productoId);
        if (item.isEmpty()) {
            throw new WebApplicationException("Producto no encontrado en el carrito", Response.Status.NOT_FOUND);
        }

        CarritoItem carritoItem = item.get();

        // Validar que la nueva cantidad sea mayor a 0
        if (nuevaCantidad <= 0) {
            throw new WebApplicationException("La cantidad debe ser mayor a 0", Response.Status.BAD_REQUEST);
        }

        // Validar stock disponible
        ProductoDTO producto = productoClient.obtenerProductoPorId(productoId);
        if (producto.stock() < nuevaCantidad) {
            throw new WebApplicationException("Stock insuficiente para el producto: " + producto.nombre(), Response.Status.BAD_REQUEST);
        }

        carritoItem.cantidad = nuevaCantidad;
        carritoItemRepository.persist(carritoItem);

        return carritoItem;
    }
}

