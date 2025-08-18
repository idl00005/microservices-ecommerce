package DTO;

import java.math.BigDecimal;

public record CarritoItemDetalleDTO(Long productoId, String nombre, String imagenUrl, int cantidad, BigDecimal precio) {}