package com.Repositorios;

import com.Entidades.Producto;
import com.Entidades.Valoracion;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.jboss.logging.annotations.Param;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RepositorioProducto implements PanacheRepository<Producto> {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public void add(Producto producto) {
        persist(producto);
    }

    @Transactional
    public boolean updateProduct(Long id, String nombre, String descripcion,
                                 @NotNull(message = "El precio del producto es obligatorio")
                                 @DecimalMin(value = "0.00", inclusive = true, message = "El precio debe ser mayor que 0")
                                 @Digits(integer = 10, fraction = 2, message = "El precio debe tener un formato válido (hasta 10 dígitos enteros y 2 decimales)")
                                 BigDecimal precio, Integer stock, JsonNode detalles) {
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

    @Transactional
    public boolean eliminarPorId(Long id) {
        long filas = delete("id = ?1", id);
        return filas > 0;
    }

    public Integer contarValoraciones(Long productoId) {
        return entityManager.createQuery(
                        "SELECT SIZE(p.valoraciones) FROM Producto p WHERE p.id = :id",
                        Integer.class
                ).setParameter("id", productoId)
                .getSingleResult();
    }

    public List<Valoracion> findValoracionesPaginadas(Long idProducto, int page, int size) {
        return getEntityManager()
                .createQuery(
                        "SELECT v FROM Producto p JOIN p.valoraciones v WHERE p.id = :idProducto ORDER BY v.fechaCreacion DESC",
                        Valoracion.class
                )
                .setParameter("idProducto", idProducto)
                .setFirstResult((page - 1) * size)  // Cálculo del offset
                .setMaxResults(size)
                .getResultList();
    }

    @Transactional
    public boolean checkDatabaseConnection() {
        try {
            Query query = entityManager.createNativeQuery("SELECT 1");
            query.getSingleResult();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Producto> buscarProductos(int page, int size, String nombre, String categoria, Double precioMin, Double precioMax) {
        StringBuilder query = new StringBuilder("FROM Producto p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (nombre != null && !nombre.isEmpty()) {
            query.append(" AND LOWER(p.nombre) LIKE :nombre");
            params.put("nombre", "%" + nombre.toLowerCase() + "%");
        }
        if (categoria != null && !categoria.isEmpty()) {
            query.append(" AND LOWER(p.categoria) = :categoria");
            params.put("categoria", categoria.toLowerCase());
        }
        if (precioMin != null) {
            query.append(" AND p.precio >= :precioMin");
            params.put("precioMin", BigDecimal.valueOf(precioMin));
        }
        if (precioMax != null) {
            query.append(" AND p.precio <= :precioMax");
            params.put("precioMax", BigDecimal.valueOf(precioMax));
        }

        PanacheQuery<Producto> panacheQuery = find(query.toString(), params)
                .page(Page.of(page - 1, size));  // Ajustamos porque Panache usa índice 0 para página

        return panacheQuery.list();
    }
}
