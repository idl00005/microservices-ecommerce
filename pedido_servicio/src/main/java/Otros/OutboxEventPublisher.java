package Otros;

import Entidades.OutboxEvent;
import Repositorios.OutboxEventRepository;
import Repositorios.PedidoRepository;
import Servicios.PedidoService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class OutboxEventPublisher {

    @Inject
    OutboxEventRepository outboxRepo;
    private static final Logger LOG = Logger.getLogger(String.valueOf(OutboxEventPublisher.class));

    @Inject
    @Channel("valoraciones-out")
    Emitter<String> emitter;

    @Scheduled(every = "5s")
    public void procesarEventos() {
        //System.out.println("Publicando eventos pendientes...");
        //LOG.info("Iniciando la acción...");
        List<OutboxEvent> eventos = outboxRepo.findPending();
        //System.out.println("Eventos pendientes: " + eventos.size());
        for (OutboxEvent evento : eventos) {
            emitter.send(evento.payload);
            markPublished(evento);
        }
    }

    @Transactional
    void markPublished(OutboxEvent evt) {
        evt.status = OutboxEvent.Status.PUBLISHED;
        outboxRepo.merge(evt); // Persistir el cambio de estado
    }
}