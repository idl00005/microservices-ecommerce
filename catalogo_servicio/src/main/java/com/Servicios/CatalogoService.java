package com.Servicios;

import com.Entidades.Producto;
import com.Otros.ProductEvent;
import com.Repositorios.RepositorioProducto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CatalogoService {

    @Inject
    public RepositorioProducto productoRepository;

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
    public Producto agregarProducto(Producto producto) {
        productoRepository.add(producto);
        return producto;
    }

    @Transactional
    public boolean actualizarProducto(Long id, Producto producto) {
        return productoRepository.updateProduct(
                id, producto.getNombre(), producto.getDescripcion(),
                producto.getPrecio(), producto.getStock(), producto.getDetalles()
        );
    }

    @Transactional
    public boolean eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id);
        if (producto != null) {
            productoRepository.delete(producto);
            return true;
        }
        return false;
    }

    public Producto obtenerProductoPorId(Long id) {
        return productoRepository.findById(id);
    }

    public void emitirEventoProducto(ProductEvent event) {
        productEventEmitter.send(event);
    }
}