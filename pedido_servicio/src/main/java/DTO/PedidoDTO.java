package DTO;

public record PedidoDTO(long id, long productoId, int cantidad, String estado, double precioTotal) {}
