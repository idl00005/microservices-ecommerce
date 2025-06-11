package DTO;

import java.math.BigDecimal;

public record CarritoItemDetalleDTO(Long productoId, String nombre, int cantidad, BigDecimal precio) {}