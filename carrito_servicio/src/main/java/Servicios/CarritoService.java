package Servicios;

import Cliente.StockClient;
import DTO.*;
import Entidades.CarritoItem;
import Entidades.LineaPago;
import Entidades.OrdenPago;
import Entidades.OutboxEvent;
import DTO.ProductEventDTO;
import Repositorios.CarritoItemRepository;
import Repositorios.OrdenPagoRepository;
import Repositorios.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbBuilder;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class CarritoService {

    @Inject
    CarritoItemRepository carritoItemRepository;


    private static final Logger LOGGER = LoggerFactory.getLogger(CarritoService.class);

    @Inject
    StripeService stripeService;

    @Inject
    OrdenPagoRepository ordenPagoRepository;

    @Inject
    OutboxEventRepository outboxRepo;

    @Inject
    public StockClient stockClient;

    @Transactional
    // Todo: Reservar stock y obtener productos en una sola llamada
    public OrdenPago iniciarPago(String userId, String direccion, String telefono, String jwt) {
        List<CarritoItem> carrito = carritoItemRepository.findByUserId(userId);
        if (carrito.isEmpty()) {
            throw new WebApplicationException("El carrito está vacío", 400);
        }

        // Obtener precios actualizados de los productos
        Map<Long, BigDecimal> precios = carrito.stream()
                .collect(Collectors.toMap(
                        item -> item.getProductoId(),
                        item -> {
                            ProductoDTO producto = stockClient.obtenerProductoPorId(item.getProductoId());
                            if (producto == null) {
                                throw new WebApplicationException("Producto no encontrado: " + item.getProductoId(), 404);
                            }
                            return producto.precio();
                        }
                ));

        // 2) Construir lista de DTO con precio
        List<CarritoItemDTO> itemsConPrecio = carrito.stream()
                .map(item -> new CarritoItemDTO(
                        item.getProductoId(),
                        item.getCantidad(),
                        precios.get(item.getProductoId())
                ))
                .toList();

        Map<Long, Integer> productosAReservar = carrito.stream()
                .collect(Collectors.toMap(
                        item -> item.getProductoId(),
                        item -> item.getCantidad()
                ));

        // Calcular el total
        BigDecimal total = carrito.stream()
                .map(item -> precios.get(item.getProductoId()).multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1. Reservar stock de forma SÍNCRONA (esperando respuesta)
        stockClient.reservarStock(productosAReservar, jwt);

        List<LineaPago> lineas = carrito.stream()
                .map(item -> new LineaPago(
                        item.getProductoId(),
                        item.getCantidad()
                ))
                .toList();

        OrdenPago orden = new OrdenPago();
        orden.setUserId(userId);
        orden.setMontoTotal(total);
        orden.setEstado("PENDIENTE");
        orden.setFechaCreacion(LocalDateTime.now());
        orden.setDireccion(direccion);
        orden.setTelefono(telefono);
        orden.setItemsComprados(lineas);
        ordenPagoRepository.persist(orden);

        if(orden.getMontoTotal().compareTo(BigDecimal.ZERO) == 0) {
            orden.setEstado("PAGADO");
            try {
                procesarPagoCompletado(orden);
            } catch (WebApplicationException e) {
                throw new WebApplicationException("Error al procesar el pago: " + e.getMessage(), 500);
            }
            return orden;
        } else {
            try {
                PaymentIntent pi = stripeService.crearPago(orden);
                orden.setReferenciaExterna(pi.getId());
                orden.setProveedor("Stripe");
                orden.setEstado("CREADO");
            } catch (StripeException e) {
                throw new WebApplicationException("Error al procesar el pago: " + e.getMessage(), 500);
            }
        }

        return orden;
    }

    @Transactional
    public void procesarCompra(OrdenPago orden) {
        List<CarritoItemDTO> itemsConPrecio;

        System.out.println("Enviando pedido...");//

        Hibernate.initialize(orden.getItemsComprados());
        List<LineaPago> carrito = orden.getItemsComprados();
        itemsConPrecio = carrito.stream()
                .map(item -> new CarritoItemDTO(
                        item.getProductoId(),
                        item.getCantidad(),
                        BigDecimal.ZERO
                ))
                .toList();

        // 3) Armar y enviar el evento al servicio de pedidos
        NuevoPedidoEventDTO nuevoPedidoEvent = new NuevoPedidoEventDTO();
        nuevoPedidoEvent.setUserId(orden.getUserId());
        nuevoPedidoEvent.setOrdenId(orden.getId());
        nuevoPedidoEvent.setItems(itemsConPrecio);

        String payloadJson = JsonbBuilder.create().toJson(nuevoPedidoEvent);
        OutboxEvent evt = new OutboxEvent();
        evt.setAggregateType("Carrito");
        evt.setAggregateId(orden.getUserId());
        evt.setEventType("Carrito.CompraProcesada");
        evt.setPayload(payloadJson);
        outboxRepo.persist(evt);

        // 4) Finalizar estado y vaciar carrito
        orden.setEstado("COMPLETADO");
        carritoItemRepository.delete("userId", orden.getUserId());
    }

    @Transactional
    public void procesarPagoCompletado(OrdenPago orden) {
        if (orden != null && "PAGADO".equals(orden.getEstado())) {

            // Preparar el evento
            StockEventDTO evento = StockEventDTO.confirmacionCompra(
                    orden.getItemsComprados().stream()
                            .collect(Collectors.toMap(
                                    item -> item.getProductoId(),
                                    item -> item.getCantidad()
                            )),
                    orden.getId()
            );

            try {
                String payload = JsonbBuilder.create().toJson(evento);
                OutboxEvent outboxEvent = new OutboxEvent();
                outboxEvent.setAggregateType("Catalogo");
                outboxEvent.setAggregateId(String.valueOf(orden.getId())); // O userId si prefieres
                outboxEvent.setEventType("PAGO_COMPLETADO");
                outboxEvent.setPayload(payload);
                outboxEvent.setStatus(OutboxEvent.Status.PENDING);
                outboxRepo.persist(outboxEvent);
            } catch (Exception e) {
                throw new RuntimeException("Error serializando evento de pago completado", e);
            }
            procesarCompra(orden);
        }
    }



    @Transactional
    public void procesarPagoCancelado(OrdenPago orden) {
        if (orden != null) {
            // Enviar evento asíncrono para liberar stock
            StockEventDTO evento = StockEventDTO.liberacionStock(
                    orden.getItemsComprados().stream()
                            .collect(Collectors.toMap(
                                    item -> item.getProductoId(),
                                    item -> item.getCantidad()
                            )),
                    orden.getId()
            );
            try {
                String payload = JsonbBuilder.create().toJson(evento);
                OutboxEvent outboxEvent = new OutboxEvent();
                outboxEvent.setAggregateType("Catalogo");
                outboxEvent.setAggregateId(String.valueOf(orden.getId()));
                outboxEvent.setEventType("PAGO_CANCELADO");
                outboxEvent.setPayload(payload);
                outboxEvent.setStatus(OutboxEvent.Status.PENDING);
                outboxRepo.persist(outboxEvent);
            } catch (Exception e) {
                throw new RuntimeException("Error serializando evento de pago cancelado", e);
            }
        }
    }


    @Transactional
    @CacheInvalidate(cacheName = "carrito-cache")
    public CarritoItemDetalleDTO agregarProducto(@CacheKey String userId, Long productoId, int cantidad) {
        ProductoDTO producto = stockClient.obtenerProductoPorId(productoId);

        if (producto == null) {
            throw new WebApplicationException("El producto no existe", Response.Status.NOT_FOUND);
        }

        // Validar stock disponible
        if (producto.stock() < cantidad) {
            throw new WebApplicationException("Stock insuficiente para el producto: " + producto.nombre(), 400);
        }

        // Verificar si el producto ya está en el carrito del usuario
        Optional<CarritoItem> itemExistente = carritoItemRepository.findByUserAndProducto(userId, productoId);

        CarritoItem item;
        if (itemExistente.isPresent()) {
            item = itemExistente.get();
            item.setCantidad(item.getCantidad()+cantidad);
            if (item.getCantidad() > producto.stock()) {
                throw new WebApplicationException("Stock insuficiente para el producto: " + producto.nombre(), 400);
            }
            carritoItemRepository.persist(item);
        } else {
            item = new CarritoItem();
            item.setUserId(userId);
            item.setProductoId(productoId);
            item.setCantidad(cantidad);
            carritoItemRepository.persist(item);
        }

        return new CarritoItemDetalleDTO(
                item.getProductoId(),
                producto.nombre(),
                producto.imagenUrl(),
                item.getCantidad(),
                producto.precio()
        );
    }

    @Transactional
    @CacheResult(cacheName = "carrito-cache")
    public List<CarritoItemDetalleDTO> obtenerCarrito(String userId) {
        LOGGER.info("Obteniendo carrito para el usuario: {}", userId);
        List<CarritoItem> carrito =  carritoItemRepository.findByUserId(userId);
        List<CarritoItemDetalleDTO> carritoDetalles = new ArrayList<>();
        for (CarritoItem item : carrito) {
            ProductoDTO producto = stockClient.obtenerProductoPorId(item.getProductoId());
            if (producto == null) {
                throw new WebApplicationException("Producto no encontrado: " + item.getProductoId(), Response.Status.NOT_FOUND);
            }
            item.setCantidad(Math.min(item.getCantidad(), producto.stock())); // Ajustar cantidad al stock disponible
            CarritoItemDetalleDTO detalle = new CarritoItemDetalleDTO(
                    item.getProductoId(),
                    producto.nombre(),
                    producto.imagenUrl(),
                    item.getCantidad(),
                    producto.precio()
            );
            carritoDetalles.add(detalle);
        }
        return carritoDetalles;
    }


    @Incoming("productos-in")
    @Transactional
    public void procesarEventoProducto(String eventJson) throws JsonProcessingException {
        ProductEventDTO event = parseEvent(eventJson);
        LOGGER.info("Iniciando operación...");

        if ("DELETED".equals(event.getAction())) {
            eliminarProductoDeCarritos(event.getProductId());
        } else if ("UPDATED".equals(event.getAction())) {
            invalidarCacheProducto(event.getProductId());
        } else {
            LOGGER.warn("Acción desconocida: " + event.getAction());
        }
    }

    private ProductEventDTO parseEvent(String eventJson) throws JsonProcessingException {
        System.out.println("Recibido: " + eventJson);
        return new ObjectMapper().readValue(eventJson, ProductEventDTO.class);
    }

    @Transactional
    public void eliminarProductoDeCarritos(long productId) {
        List<String> userIds = carritoItemRepository.findUserIdsByProductoId(productId);

        carritoItemRepository.delete("productoId", productId);

        for (String userId : userIds) {
            invalidarCarritoUsuario(userId);
        }
    }

    @CacheInvalidate(cacheName = "carrito-cache")
    public void invalidarCarritoUsuario(@CacheKey String userId) {}

    @CacheInvalidate(cacheName = "producto-cache")
    public void invalidarCacheProducto(@CacheKey Long id) {
        List<String> userIds = carritoItemRepository.findUserIdsByProductoId(id);
        for (String userId : userIds) {
            invalidarCarritoUsuario(userId);
        }
    }

    @Transactional
    @CacheInvalidate(cacheName = "carrito-cache")
    public void eliminarProducto(@CacheKey String userId, Long productoId) {
        Optional<CarritoItem> item = carritoItemRepository.findByUserAndProducto(userId, productoId);
        if (item.isEmpty()) {
            throw new WebApplicationException("Producto no encontrado en el carrito", Response.Status.NOT_FOUND);
        }
        carritoItemRepository.delete(item.get());
    }

    @Transactional
    @CacheInvalidate(cacheName = "carrito-cache")
    public void vaciarCarrito(@CacheKey String userId) {
        carritoItemRepository.delete("userId", userId);
    }

    @Transactional
    @CacheInvalidate(cacheName = "carrito-cache")
    public CarritoItem actualizarCantidadProducto(@CacheKey String userId, Long productoId, int nuevaCantidad) {
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
        ProductoDTO producto = stockClient.obtenerProductoPorId(productoId);
        if (producto == null) {
            throw new WebApplicationException("El producto no existe", Response.Status.NOT_FOUND);
        }

        if (producto.stock() < nuevaCantidad) {
            throw new WebApplicationException("Stock insuficiente para el producto: " + producto.nombre(), Response.Status.BAD_REQUEST);
        }

        carritoItem.setCantidad(nuevaCantidad);
        carritoItemRepository.persist(carritoItem);

        return carritoItem;
    }
}

