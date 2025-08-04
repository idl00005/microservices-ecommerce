package com.Servicios;

import com.DTO.ProductoDTO;
import com.DTO.StockEventDTO;
import com.DTO.ValoracionDTO;
import com.Entidades.Producto;
import com.Entidades.Valoracion;
import com.Excepciones.ValoracionDuplicadaException;
import com.Otros.ProductEvent;
import com.Repositorios.RepositorioProducto;
import com.Repositorios.ValoracionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CatalogoService {

    @Inject
    public RepositorioProducto productoRepository;

    @Inject
    public ValoracionRepository valoracionRepository;

    @Inject
    public ObjectMapper objectMapper;

    @Inject
    @Channel("product-events")
    public Emitter<ProductEvent> productEventEmitter;

    public List<Producto> obtenerProductos(int page, int size, String nombre, String categoria, Double precioMin, Double precioMax) {
        List<Producto> productos = productoRepository.listAll().stream()
                .filter(p -> nombre == null || p.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .filter(p -> categoria == null || (p.getCategoria() != null && p.getCategoria().equalsIgnoreCase(categoria)))
                .filter(p -> precioMin == null || p.getPrecio().compareTo(BigDecimal.valueOf(precioMin)) >= 0)
                .filter(p -> precioMax == null || p.getPrecio().compareTo(BigDecimal.valueOf(precioMax)) <= 0)
                .collect(Collectors.toList());

        if (productos.isEmpty()) {
            return List.of(); // Retorna una lista vacía si no hay productos
        }

        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, productos.size());

        if (startIndex >= productos.size()) {
            throw new IllegalArgumentException("Página fuera de rango.");
        }

        return productos.subList(startIndex, endIndex);
    }

    @Transactional
    public Producto agregarProducto(ProductoDTO producto) {
        Producto nuevoProducto = new Producto(
                producto.getNombre(), producto.getDescripcion(),
                producto.getPrecio(), producto.getStock(),
                producto.getCategoria(), producto.getDetalles()
        );
        productoRepository.add(nuevoProducto);
        return nuevoProducto;
    }

    @Transactional
    public boolean actualizarProducto(Long id, ProductoDTO producto) {
        boolean actualizado = productoRepository.updateProduct(
                id, producto.getNombre(), producto.getDescripcion(),
                producto.getPrecio(), producto.getStock(), producto.getDetalles()
        );
        if (actualizado) {
            ProductEvent event = new ProductEvent(id, "UPDATED", null);
            emitirEventoProducto(event);
            invalidarCacheProducto(id);
            return true;
        } else {
            return false;
        }
    }

    public boolean eliminarProducto(Long id) {
        boolean eliminado = productoRepository.eliminarPorId(id);
        if (eliminado) {
            invalidarCacheProducto(id);
            ProductEvent event = new ProductEvent(id, "DELETED", null);
            emitirEventoProducto(event);
        }
        return eliminado;
    }

    @Transactional
    public boolean reservarStock(Long productoId, int cantidad) {
        Producto producto = productoRepository.findById(productoId);
        if (producto == null) {
            throw new WebApplicationException("Producto no encontrado", 404);
        }

        if (producto.getStock() - producto.getStockReservado() >= cantidad) {
            producto.setStockReservado(producto.getStockReservado() + cantidad);
            productoRepository.persist(producto);
            invalidarCacheProducto(productoId);
            return true;
        }
        return false;
    }

    @Incoming("eventos-stock")
    @Transactional
    public void procesarEventoStock(String mensaje) throws JsonProcessingException {
        StockEventDTO evento = objectMapper.readValue(mensaje, StockEventDTO.class);
        switch (evento.tipo()) {
            case "LIBERAR_STOCK":
                evento.productos().forEach((productoId, cantidad) -> {
                    Producto producto = productoRepository.findById(productoId);
                    if (producto != null) {
                        producto.setStockReservado(
                                producto.getStockReservado() - cantidad
                        );
                        productoRepository.persist(producto);
                    }
                });
                break;

            case "CONFIRMAR_COMPRA":
                evento.productos().forEach((productoId, cantidad) -> {
                    Producto producto = productoRepository.findById(productoId);
                    if (producto != null) {
                        producto.setStock(producto.getStock() - cantidad);
                        producto.setStockReservado(
                                producto.getStockReservado() - cantidad
                        );
                        productoRepository.persist(producto);
                    }
                });
                break;
        }

        // Invalidar el caché de del producto afectado
        evento.productos().keySet().forEach(this::invalidarCacheProducto);
    }

    @Incoming("valoraciones-in")
    @Transactional
    public void procesarEventoValoracion(String mensaje) {
        try {
            // Deserializar el mensaje
            ValoracionDTO valoracionDTO = objectMapper.readValue(mensaje, ValoracionDTO.class);

            // Buscar el producto asociado
            Producto producto = productoRepository.findById(valoracionDTO.idProducto());
            if (producto == null) {
                throw new IllegalArgumentException("Producto no encontrado con ID: " + valoracionDTO.idProducto());
            }

            // Crear y guardar la valoración (asociada al producto)
            Valoracion valoracionEntity = new Valoracion();
            valoracionEntity.setProducto(producto);
            valoracionEntity.setIdUsuario(valoracionDTO.idUsuario());
            valoracionEntity.setPuntuacion(valoracionDTO.puntuacion());
            valoracionEntity.setComentario(valoracionDTO.comentario());
            valoracionEntity.setFechaCreacion(LocalDateTime.now());

            // Agregar la valoración al producto (manejo de la relación unidireccional)
            producto.agregarValoracion(valoracionEntity);

            // Actualizar la puntuación promedio del producto
            actualizarPuntuacionProducto(producto, valoracionDTO.puntuacion());

            // Invalidar el caché de número de valoraciones
            invalidarCacheNumValoraciones(valoracionDTO.idProducto());

            System.out.println("Valoración procesada y guardada: " + valoracionDTO);
        } catch (Exception e) {
            System.err.println("Error al procesar el evento de valoración: " + e.getMessage());
        }
    }

    @Transactional
    public List<Valoracion> obtenerValoracionesPorProducto(Long idProducto, int pagina, int tamanio) {
        return productoRepository.findValoracionesPaginadas(idProducto, pagina, tamanio);
    }

    @Transactional
    public void actualizarPuntuacionProducto(Producto producto, int puntuacion) {
        long totalValoraciones = productoRepository.contarValoraciones(producto.getId());
        producto.actualizarPuntuacion(puntuacion, totalValoraciones);
        productoRepository.persist(producto);
    }

    @Transactional
    @CacheResult(cacheName = "num-valoracion-cache")
    public long contarValoracionesPorProducto(Long idProducto) {
        return productoRepository.contarValoraciones(idProducto);
    }

    @CacheInvalidate(cacheName = "num-valoracion-cache")
    protected void invalidarCacheNumValoraciones(@CacheKey Long idProducto) {}

    @CacheResult(cacheName = "procducto-cache")
    public Producto obtenerProductoPorId(Long id) {
        return productoRepository.findById(id);
    }

    @CacheInvalidate(cacheName = "procducto-cache")
    protected void invalidarCacheProducto(@CacheKey Long id) {}

    public void emitirEventoProducto(ProductEvent event) {
        productEventEmitter.send(event);
    }

    public boolean existeValoracionPorPedido(Long productoId, String usuarioId) {
        return valoracionRepository.count("producto.id = ?1 and usuarioId = ?2", productoId, usuarioId) > 0;
    }
}