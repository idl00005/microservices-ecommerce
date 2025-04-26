package Repositorios;

import Entidades.OutboxEvent;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class OutboxEventRepository implements PanacheRepository<OutboxEvent> {

    public List<OutboxEvent> findPending() {
        return list("status", OutboxEvent.Status.PENDING);
    }

    public void merge(OutboxEvent evt) {
        getEntityManager().merge(evt);
    }
}