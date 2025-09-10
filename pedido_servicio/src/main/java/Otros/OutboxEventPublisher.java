package Otros;

import DTO.ValoracionDTO;
import Entidades.OutboxEvent;
import Repositorios.OutboxEventRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class OutboxEventPublisher {

    @Inject
    OutboxEventRepository outboxRepo;
    private static final Logger LOG = Logger.getLogger(String.valueOf(OutboxEventPublisher.class));

    @Inject
    @Channel("valoraciones-out")
    Emitter<ValoracionDTO> emitter;

    @Inject
    ObjectMapper objectMapper;

    @Scheduled(every = "5s")
    public void procesarEventos() {
        List<OutboxEvent> eventos = outboxRepo.findPending();
        for (OutboxEvent evento : eventos) {
            try {
                ValoracionDTO dto = objectMapper.readValue(evento.payload, ValoracionDTO.class);
                emitter.send(dto);
                markPublished(evento);
            } catch (Exception e) {
                LOG.warning("Error al deserializar o enviar evento: " + e.getMessage());
            }
        }
    }

    @Transactional
    void markPublished(OutboxEvent evt) {
        evt.status = OutboxEvent.Status.PUBLISHED;
        outboxRepo.merge(evt);
    }
}