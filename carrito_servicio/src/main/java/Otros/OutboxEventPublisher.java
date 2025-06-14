package Otros;

import DTO.CarritoEventDTO;
import DTO.StockEventDTO;
import Entidades.OutboxEvent;
import Repositorios.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class OutboxEventPublisher {

    @Inject
    OutboxEventRepository outboxRepo;

    @Channel("carrito-a-pedidos-out")
    Emitter<CarritoEventDTO> emitter;

    @Channel("eventos-stock")
    Emitter<StockEventDTO> catalogoEmitter;

    @Inject
    ObjectMapper objectMapper;

    @Scheduled(every = "5s")
    public void publishPending() {

        List<OutboxEvent> pendientes = outboxRepo.findPending();
        for(OutboxEvent evt : pendientes) {
            System.out.println("Evento pendiente: " + evt.id + ", Tipo: " + evt.aggregateType + ", Contenido: " + evt.payload);
        }
        for (OutboxEvent evt : pendientes) {
            if ("Carrito".equals(evt.aggregateType)) {
                CarritoEventDTO carritoEvent = mapToCarritoEvent(evt);
                emitter.send(carritoEvent)
                        .whenComplete((r, ex) -> {
                            if (ex == null) {
                                markPublished(evt);
                            } else {
                                ex.printStackTrace(); // Log del error
                            }
                        });
            } else if ("Catalogo".equals(evt.aggregateType)) {
                StockEventDTO reducirStockEvent = mapToReducirStockEvent(evt);
                catalogoEmitter.send(reducirStockEvent)
                        .whenComplete((r, ex) -> {
                            if (ex == null) {
                                markPublished(evt);
                            } else {
                                ex.printStackTrace(); // Log del error
                            }
                        });
            }
        }
    }

    private StockEventDTO mapToReducirStockEvent(OutboxEvent evt) {
        try {
            return objectMapper.readValue(evt.payload, StockEventDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error al deserializar el payload: " + evt.payload, e);
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