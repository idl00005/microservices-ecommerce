package Repositorios;

import Entidades.Pedido;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class PedidoRepository {

    @PersistenceContext
    EntityManager entityManager;

    public void guardar(Pedido pedido) {
        entityManager.persist(pedido);
    }

    public Pedido buscarPorId(Long id) {
        return entityManager.find(Pedido.class, id);
    }

    public void actualizar(Pedido pedido) {
        entityManager.merge(pedido);
    }

    public List<Pedido> buscarPorUsuarioId(String usuarioId) {
        return entityManager.createQuery("SELECT p FROM Pedido p WHERE p.usuarioId = :usuarioId", Pedido.class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
    }

    public List<Pedido> buscarPorEstadoYUsuarioConPaginacion(String estado, String usuarioId, int offset, int limit) {
        String query = "SELECT p FROM Pedido p WHERE (:estado IS NULL OR p.estado = :estado) AND (:usuarioId IS NULL OR p.usuarioId = :usuarioId)";
        return entityManager.createQuery(query, Pedido.class)
                .setParameter("estado", estado)
                .setParameter("usuarioId", usuarioId)
                .setFirstResult(offset)
                .setMaxResults(limit)
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


}
