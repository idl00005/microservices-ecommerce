package Otros;

import DTO.NuevoPedidoEventDTO;
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
    Emitter<NuevoPedidoEventDTO> carritoEmitter;

    @Channel("eventos-stock")
    Emitter<StockEventDTO> catalogoEmitter;

    @Inject
    ObjectMapper objectMapper;

    @Scheduled(every = "5s")
    public void publishPending() {

        List<OutboxEvent> pendientes = outboxRepo.findPending();
        for(OutboxEvent evt : pendientes) {
            System.out.println("Evento pendiente: " + evt.getId() + ", Tipo: " + evt.getAggregateType() + ", Contenido: " + evt.getPayload());
        }
        for (OutboxEvent evt : pendientes) {
            if ("Carrito".equals(evt.getAggregateType())) {
                NuevoPedidoEventDTO carritoEvent = mapToCarritoEvent(evt);
                carritoEmitter.send(carritoEvent)
                        .whenComplete((r, ex) -> {
                            if (ex == null) {
                                markPublished(evt);
                            } else {
                                ex.printStackTrace(); // Log del error
                            }
                        });
            } else if ("Catalogo".equals(evt.getAggregateType())) {
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
            return objectMapper.readValue(evt.getPayload(), StockEventDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error al deserializar el payload: " + evt.getPayload(), e);
        }
    }

    private NuevoPedidoEventDTO mapToCarritoEvent(OutboxEvent evt) {
        try {
            return objectMapper.readValue(evt.getPayload(), NuevoPedidoEventDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error al deserializar el payload: " + evt.getPayload(), e);
        }
    }

    @Transactional
    protected void markPublished(OutboxEvent evt) {
        evt.setStatus(OutboxEvent.Status.PUBLISHED);
        outboxRepo.merge(evt); // Persistir el cambio de estado
    }
}