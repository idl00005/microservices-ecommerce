package Otros;

import DTO.CarritoEventDTO;
import Entidades.CarritoItem;
import Entidades.OutboxEvent;
import Repositorios.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class OutboxEventPublisher {

    @Inject
    OutboxEventRepository outboxRepo;

    @Channel("carrito-a-pedidos-out")
    Emitter<CarritoEventDTO> emitter;

    @Inject
    ObjectMapper objectMapper;

    // Se ejecuta cada 5 segundos
    @Scheduled(every = "5s")
    public void publishPending() {
        List<OutboxEvent> pendientes = outboxRepo.findPending();
        for (OutboxEvent evt : pendientes) {
            CarritoEventDTO carritoEvent= mapToCarritoEvent(evt);
            emitter.send(carritoEvent)
                    .whenComplete((r, ex) -> {
                        if (ex == null) {
                            markPublished(evt);
                        } else {
                            ex.printStackTrace(); // Log del error
                        }
                    });
        }
    }

    private CarritoEventDTO mapToCarritoEvent(OutboxEvent evt) {
        try {
            return objectMapper.readValue(evt.payload, CarritoEventDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error al deserializar el payload: " + evt.payload, e);
        }
    }

    @Transactional
    void markPublished(OutboxEvent evt) {
        evt.status = OutboxEvent.Status.PUBLISHED;
        outboxRepo.merge(evt); // Persistir el cambio de estado
    }
}