package Repositorios;

import Entidades.CarritoItem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CarritoItemRepository implements PanacheRepository<CarritoItem> {

    public List<CarritoItem> findByProductoId(Long productoId) {
        return list("productoId", productoId);
    }

    public Optional<CarritoItem> findByUserAndProducto(String userId, Long productoId) {
        return find("userId = ?1 and productoId = ?2", userId, productoId).firstResultOptional();
    }

    public List<CarritoItem> findByUserId(String userId) {
        return list("userId", userId);
    }

    public List<String> findUserIdsByProductoId(Long productoId) {
        return find("SELECT DISTINCT c.userId FROM CarritoItem c WHERE c.productoId = ?1", productoId)
                .project(String.class)
                .list();
    }
}

