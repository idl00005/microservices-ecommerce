package com.DTO;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record StockEventDTO(
        String tipo,                            // LIBERAR_STOCK o CONFIRMAR_COMPRA
        Map<Long, Integer> productos,           // Map de productoId -> cantidad
        Long ordenId,                          // ID de la orden relacionada
        LocalDateTime timestamp,                // Momento del evento
        String correlationId                   // ID para rastrear el flujo de eventos
) {
    // Métodos de fábrica
    public static StockEventDTO liberacionStock(Map<Long, Integer> productos, Long ordenId) {
        return new StockEventDTO(
                "LIBERAR_STOCK",
                productos,
                ordenId,
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );
    }

    public static StockEventDTO confirmacionCompra(Map<Long, Integer> productos, Long ordenId) {
        return new StockEventDTO(
                "CONFIRMAR_COMPRA",
                productos,
                ordenId,
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );
    }
}

