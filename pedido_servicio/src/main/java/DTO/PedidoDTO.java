package DTO;

public record PedidoDTO(long id, long productoId, int cantidad, String usuarioId, String estado, double precioTotal) {}
