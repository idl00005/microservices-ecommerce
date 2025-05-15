package DTO;

import java.math.BigDecimal;

public class CarritoItemDTO{

    private String userId;
    private Long productoId;
    private String nombreProducto;
    private BigDecimal precio;
    private Integer cantidad;

    public CarritoItemDTO(String userId, Long productoId, String nombreProducto, BigDecimal precio, Integer cantidad) {
        this.userId = userId;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    public String getUserId() {
        return userId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public Integer getCantidad() {
        return cantidad;
    }
}

