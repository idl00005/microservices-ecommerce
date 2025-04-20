package Entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    // Tipo de agregación (p. ej. "Carrito")
    @Column(nullable = false)
    public String aggregateType;

    // ID de la entidad raíz (p. ej. userId)
    @Column(nullable = false)
    public String aggregateId;

    @Column(nullable = false)
    public String eventType;

    // Payload JSON con los datos del evento
    @Column(columnDefinition = "TEXT", nullable = false)
    public String payload;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public Status status = Status.PENDING;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public enum Status {
        PENDING,
        PUBLISHED
    }
}