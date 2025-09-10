package Entidades;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String aggregateType;

    @Column(nullable = false)
    public String aggregateId;

    @Column(nullable = false)
    public String eventType;

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