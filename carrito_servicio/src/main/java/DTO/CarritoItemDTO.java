package DTO;

import java.math.BigDecimal;

public record CarritoItemDTO(Long productoId, Integer cantidad, BigDecimal precio) {}