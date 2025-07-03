package Repositorios;

import Entidades.CarritoItem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CarritoItemRepository implements PanacheRepository<CarritoItem> {

    @Inject
    EntityManager em;

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

    @Transactional
    public boolean checkDatabaseConnection() {
        try {
            Query query = em.createNativeQuery("SELECT 1");
            query.getSingleResult();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

