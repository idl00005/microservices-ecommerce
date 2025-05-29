package DTO;

import java.math.BigDecimal;

public class CarritoItemDTO{

    private Long productoId;
    private Integer cantidad;
    private BigDecimal precio;

    public CarritoItemDTO(Long productoId, Integer cantidad, BigDecimal precio) {
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public CarritoItemDTO() {
    }

    public Long getProductoId() {
        return productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecio() { return precio; }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}