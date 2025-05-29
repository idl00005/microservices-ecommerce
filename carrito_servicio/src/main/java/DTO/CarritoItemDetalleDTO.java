// carrito_servicio/src/main/java/DTO/CarritoItemDetalleDTO.java
package DTO;

import java.math.BigDecimal;

public class CarritoItemDetalleDTO {
    public Long productoId;
    public String nombre;
    public int cantidad;
    public BigDecimal precio;

    public CarritoItemDetalleDTO(Long productoId, String nombre, int cantidad, BigDecimal precio) {
        this.productoId = productoId;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precio = precio;
    }
}