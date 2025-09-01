package com.Servicios;

import com.DTO.ProductoDTO;
import com.DTO.StockEventDTO;
import com.DTO.ValoracionDTO;
import com.Entidades.Producto;
import com.Entidades.Valoracion;
import com.DTO.ProductEventDTO;
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
    public Emitter<ProductEventDTO> productEventEmitter;

    public List<ProductoDTO> obtenerProductos(int page, int size, String nombre, String categoria, Double precioMin, Double precioMax) {
        List<Producto> productos = productoRepository.buscarProductos(page, size, nombre, categoria, precioMin, precioMax);

        // Mapeo a DTO
        return productos.stream()
                .map(p -> new ProductoDTO(
                        p.getId(),
                        p.getNombre(),
                        p.getDescripcion(),
                        p.getPrecio(),
                        p.getStock(),
                        p.getCategoria(),
                        p.getImagenURL(),
                        p.getDetalles()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductoDTO agregarProducto(ProductoDTO producto) {
        Producto nuevoProducto = new Producto(
                producto.getNombre(), producto.getDescripcion(),
                producto.getPrecio(), producto.getStock(),
                producto.getCategoria(), producto.getImagenUrl(), producto.getDetalles()
        );
        productoRepository.persist(nuevoProducto);

        return new ProductoDTO(nuevoProducto.getId(),nuevoProducto.getNombre(), nuevoProducto.getDescripcion(),
                nuevoProducto.getPrecio(), nuevoProducto.getStock(),
                nuevoProducto.getCategoria(), nuevoProducto.getImagenURL(), nuevoProducto.getDetalles());
    }

    public boolean actualizarProducto(Long id, ProductoDTO producto) {
        boolean actualizado = productoRepository.updateProduct(
                id, producto.getNombre(), producto.getDescripcion(),
                producto.getPrecio(), producto.getStock(), producto.getDetalles()
        );
        if (actualizado) {
            ProductEventDTO event = new ProductEventDTO(id, "UPDATED", null);
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
            ProductEventDTO event = new ProductEventDTO(id, "DELETED", null);
            emitirEventoProducto(event);
        }
        return eliminado;
    }

    @Transactional
    public boolean reservarStock(Long productoId, int cantidad) {
        Producto producto = productoRepository.findById(productoId);
        if (producto == null) {
            throw new WebApplicationException("Producto no encontrado con id "+productoId, 404);
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

            System.out.println("Obtenida la siguiente valoración: " + valoracionDTO);
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
            invalidarCacheProducto(valoracionDTO.idProducto());
        } catch (Exception e) {
            System.err.println("Error al procesar el evento de valoración: " + e.getMessage());
        }
    }

    @Transactional
    public List<ValoracionDTO> obtenerValoracionesPorProducto(Long idProducto, int pagina, int tamanio) {
        List<Valoracion> valoraciones = productoRepository.findValoracionesPaginadas(idProducto, pagina, tamanio);
        return valoraciones.stream()
                .map(v -> new ValoracionDTO(
                        v.getIdUsuario(),
                        v.getProducto() != null ? v.getProducto().getId() : null,
                        v.getPuntuacion(),
                        v.getComentario()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void actualizarPuntuacionProducto(Producto producto, int puntuacion) {
        Integer totalValoraciones = productoRepository.contarValoraciones(producto.getId());
        System.out.println("Total de valoraciones para el producto " + producto.getId() + ": " + totalValoraciones);
        System.out.println("Puntuación actual: " + producto.getPuntuacion());
        System.out.println("Nueva puntuación a insertar: " + puntuacion);
        producto.actualizarPuntuacion(puntuacion, totalValoraciones);
        System.out.println("Puntuación actualizada: " + producto.getPuntuacion());
    }

    @Transactional
    @CacheResult(cacheName = "num-valoracion-cache")
    public Integer contarValoracionesPorProducto(Long idProducto) {
        return productoRepository.contarValoraciones(idProducto);
    }

    @CacheInvalidate(cacheName = "num-valoracion-cache")
    protected void invalidarCacheNumValoraciones(@CacheKey Long idProducto) {}

    @CacheResult(cacheName = "procducto-cache")
    public ProductoDTO obtenerProductoPorId(Long id) {
        Producto producto = productoRepository.findById(id);
        if(producto == null) {
            return null;
        } else {
            ProductoDTO productoDTO = new ProductoDTO(
                    producto.getId(),producto.getNombre(), producto.getDescripcion(),
                    producto.getPrecio(), producto.getStock(), producto.getCategoria(),
                    producto.getImagenURL(), producto.getDetalles()
            );
            productoDTO.setPuntuacion(producto.getPuntuacion());
            return productoDTO;
        }
    }

    @CacheInvalidate(cacheName = "procducto-cache")
    protected void invalidarCacheProducto(@CacheKey Long id) {}

    public void emitirEventoProducto(ProductEventDTO event) {
        productEventEmitter.send(event);
    }

    public boolean existeValoracionPorPedido(Long productoId, String usuarioId) {
        return valoracionRepository.count("producto.id = ?1 and usuarioId = ?2", productoId, usuarioId) > 0;
    }
}