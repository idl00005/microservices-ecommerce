package com.Repositorios;

import com.Entidades.Producto;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@ApplicationScoped
public class RepositorioProducto implements PanacheRepository<Producto> {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public void add(Producto producto) {
        persist(producto);
    }

    @Transactional
    public boolean updateProduct(Long id, String nombre, String descripcion, @NotNull(message = "El precio del producto es obligatorio") @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que 0") @Digits(integer = 10, fraction = 2, message = "El precio debe tener un formato válido (hasta 10 dígitos enteros y 2 decimales)") BigDecimal precio, Integer stock, JsonNode detalles) {
        // Buscar el producto por ID
        Producto producto = entityManager.find(Producto.class, id);
        if (producto == null) {
            // Retorna false si el producto no existe
            return false;
        }

        // Si el producto existe, actualiza sus valores
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setPrecio(precio);
        producto.setStock(stock);
        producto.setDetalles(detalles);

        // Persistir los cambios
        entityManager.merge(producto);

        // Retorna true si la operación tuvo éxito
        return true;
    }



}
