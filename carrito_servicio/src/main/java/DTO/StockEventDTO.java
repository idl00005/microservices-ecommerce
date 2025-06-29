package DTO;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record StockEventDTO(
        String tipo,
        Map<Long, Integer> productos,
        Long ordenId,
        LocalDateTime timestamp,
        String correlationId
) {

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

