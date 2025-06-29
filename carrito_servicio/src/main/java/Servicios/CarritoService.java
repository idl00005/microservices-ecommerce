package Servicios;

import Cliente.StockClient;
import DTO.*;
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
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public OrdenPago iniciarPago(String userId, String direccion, String telefono) {
        List<CarritoItem> carrito = carritoItemRepository.findByUserId(userId);
        if (carrito.isEmpty()) {
            throw new WebApplicationException("El carrito está vacío", 400);
        }

        // Obtener precios actualizados de los productos
        Map<Long, BigDecimal> precios = carrito.stream()
                .collect(Collectors.toMap(
                        item -> item.productoId,
                        item -> {
                            ProductoDTO producto = stockClient.obtenerProductoPorId(item.productoId);
                            if (producto == null) {
                                throw new WebApplicationException("Producto no encontrado: " + item.productoId, 404);
                            }
                            return producto.precio();
                        }
                ));

        // 2) Construir lista de DTO con precio
        List<CarritoItemDTO> itemsConPrecio = carrito.stream()
                .map(item -> new CarritoItemDTO(
                        item.productoId,
                        item.cantidad,
                        precios.get(item.productoId)
                ))
                .toList();

        Map<Long, Integer> productosAReservar = carrito.stream()
                .collect(Collectors.toMap(
                        item -> item.productoId,
                        item -> item.cantidad
                ));

        // Calcular el total
        BigDecimal total = carrito.stream()
                .map(item -> precios.get(item.productoId).multiply(BigDecimal.valueOf(item.cantidad)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1. Reservar stock de forma SÍNCRONA (esperando respuesta)
        Response reservaExitosa = stockClient.reservarStock(productosAReservar);
        if (reservaExitosa.getStatus() != 200) {
            throw new WebApplicationException(reservaExitosa.toString(), reservaExitosa.getStatus());
        }

        OrdenPago orden = new OrdenPago();
        orden.userId = userId;
        orden.montoTotal = total;
        orden.estado = "PENDIENTE";
        orden.fechaCreacion = LocalDateTime.now();
        orden.direccion = direccion;
        orden.telefono = telefono;
        ordenPagoRepository.persist(orden);

        if(orden.montoTotal.compareTo(BigDecimal.ZERO) == 0) {
            orden.estado = "PAGADO";
            try {
                procesarPagoCompletado(orden.id);
            } catch (WebApplicationException e) {
                throw new WebApplicationException("Error al procesar el pago: " + e.getMessage(), 500);
            }
            return orden;
        } else {
            try {
                PaymentIntent pi = stripeService.crearPago(orden,itemsConPrecio);
                orden.referenciaExterna = pi.getId();
                orden.proveedor         = "Stripe";
                orden.estado            = "CREADO";
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

        if(orden.referenciaExterna != null && !orden.referenciaExterna.isBlank()) {
            // 1) Recuperar el PaymentIntent de Stripe
            PaymentIntent pi;
            try {
                pi = PaymentIntent.retrieve(orden.referenciaExterna);
            } catch (StripeException e) {
                throw new WebApplicationException("No pude recuperar el pago: " + e.getMessage(), 500);
            }

            // 2) Leer la metadata y deserializar la lista de CarritoItemDTO
            Jsonb jsonb = JsonbBuilder.create();

            // 1) Obtenemos el JSON de Stripe
            String itemsJson = pi.getMetadata().get("items");

            // 2) Deserializamos a array
            CarritoItemDTO[] array = jsonb.fromJson(itemsJson, CarritoItemDTO[].class);

            // 3) Convertimos a List
            itemsConPrecio = Arrays.asList(array);

            System.out.println("itemsJson: " + itemsJson);
            System.out.println("itemsConPrecio: " + itemsConPrecio.get(0).productoId() + ", " + itemsConPrecio.get(0).cantidad() + ", " + itemsConPrecio.get(0).precio());

        } else {
            // Si no hay referencia externa significa que el precio es 0 (pago sin Stripe)
            List<CarritoItem> carrito = carritoItemRepository.findByUserId(orden.userId);
            itemsConPrecio = carrito.stream()
                    .map(item -> new CarritoItemDTO(
                            item.productoId,
                            item.cantidad,
                            BigDecimal.ZERO
                    ))
                    .toList();
        }

        // 3) Armar y enviar el evento al servicio de pedidos
        CarritoEventDTO carritoEvent = new CarritoEventDTO();
        carritoEvent.setUserId(orden.userId);
        carritoEvent.setOrdenId(orden.id);
        carritoEvent.setItems(itemsConPrecio);

        String payloadJson = JsonbBuilder.create().toJson(carritoEvent);
        OutboxEvent evt = new OutboxEvent();
        evt.aggregateType = "Carrito";
        evt.aggregateId   = orden.userId;
        evt.eventType     = "Carrito.CompraProcesada";
        evt.payload       = payloadJson;
        outboxRepo.persist(evt);

        // 4) Finalizar estado y vaciar carrito
        orden.estado = "COMPLETADO";
        carritoItemRepository.delete("userId", orden.userId);
    }

    @Transactional
    // Todo: separar esta función en varias para que no se consulte ordenPagoRepository si el coste el 0
    // Todo: el usuario podría agregar objetos al carrito antes de pagar y comprar productos sin pagar, hay que arreglar eso
    public void procesarPagoCompletado(Long ordenId) {
        OrdenPago orden = ordenPagoRepository.findById(ordenId);
        if (orden != null && "PAGADO".equals(orden.estado)) {
            List<CarritoItem> carrito = carritoItemRepository.findByUserId(orden.userId);

            // Preparar el evento
            StockEventDTO evento = StockEventDTO.confirmacionCompra(
                    carrito.stream()
                            .collect(Collectors.toMap(
                                    item -> item.productoId,
                                    item -> item.cantidad
                            )),
                    orden.id
            );

            try {
                String payload = JsonbBuilder.create().toJson(evento);
                OutboxEvent outboxEvent = new OutboxEvent();
                outboxEvent.aggregateType = "Catalogo";
                outboxEvent.aggregateId = String.valueOf(ordenId); // O userId si prefieres
                outboxEvent.eventType = "PAGO_COMPLETADO";
                outboxEvent.payload = payload;
                outboxEvent.status = OutboxEvent.Status.PENDING;
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
            List<CarritoItem> carrito = carritoItemRepository.findByUserId(orden.userId);

            // Enviar evento asíncrono para liberar stock
            StockEventDTO evento = StockEventDTO.liberacionStock(
                    carrito.stream()
                            .collect(Collectors.toMap(
                                    item -> item.productoId,
                                    item -> item.cantidad
                            )),
                    orden.id
            );
            try {
                String payload = JsonbBuilder.create().toJson(evento);
                OutboxEvent outboxEvent = new OutboxEvent();
                outboxEvent.aggregateType = "Catalogo";
                outboxEvent.aggregateId = String.valueOf(orden.id);
                outboxEvent.eventType = "PAGO_CANCELADO";
                outboxEvent.payload = payload;
                outboxEvent.status = OutboxEvent.Status.PENDING;
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
            item.cantidad += cantidad; // Incrementar la cantidad
            if (item.cantidad > producto.stock()) {
                throw new WebApplicationException("Stock insuficiente para el producto: " + producto.nombre(), 400);
            }
            carritoItemRepository.persist(item);
        } else {
            item = new CarritoItem();
            item.userId = userId;
            item.productoId = producto.id();
            item.cantidad = cantidad;
            carritoItemRepository.persist(item);
        }

        return new CarritoItemDetalleDTO(
                item.productoId,
                producto.nombre(),
                item.cantidad,
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
            ProductoDTO producto = stockClient.obtenerProductoPorId(item.productoId);
            if (producto == null) {
                throw new WebApplicationException("Producto no encontrado: " + item.productoId, Response.Status.NOT_FOUND);
            }
            item.cantidad = Math.min(item.cantidad, producto.stock()); // Ajustar cantidad al stock disponible
            CarritoItemDetalleDTO detalle = new CarritoItemDetalleDTO(
                    item.productoId,
                    producto.nombre(),
                    item.cantidad,
                    producto.precio()
            );
            carritoDetalles.add(detalle);
        }
        return carritoDetalles;
    }


    @Incoming("productos-in")
    @Transactional
    public void procesarEventoProducto(String eventJson) throws JsonProcessingException {
        ProductEvent event = parseEvent(eventJson);
        LOGGER.info("Iniciando operación...");

        if ("DELETED".equals(event.getAction())) {
            eliminarProductoDeCarritos(event.getProductId());
        } else if ("UPDATED".equals(event.getAction())) {
            invalidarCacheProducto(event.getProductId());
        } else {
            LOGGER.warn("Acción desconocida: " + event.getAction());
        }
    }

    private ProductEvent parseEvent(String eventJson) throws JsonProcessingException {
        System.out.println("Recibido: " + eventJson);
        return new ObjectMapper().readValue(eventJson, ProductEvent.class);
    }

    //@Transactional
    //public void actualizarProductoEnCarritos(long productId, ProductoDTO producto) {
    //    List<CarritoItem> items = carritoItemRepository.findByProductoId(productId);
    //    for (CarritoItem item : items) {
    //        if(item.cantidad > producto.stock()) {
    //            item.cantidad = producto.stock();
    //        }
    //        carritoItemRepository.persist(item);
    //    }
    //}

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
        if (producto.stock() < nuevaCantidad) {
            throw new WebApplicationException("Stock insuficiente para el producto: " + producto.nombre(), Response.Status.BAD_REQUEST);
        }

        carritoItem.cantidad = nuevaCantidad;
        carritoItemRepository.persist(carritoItem);

        return carritoItem;
    }
}

