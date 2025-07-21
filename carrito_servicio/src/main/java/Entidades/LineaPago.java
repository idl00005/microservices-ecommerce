package Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
public class LineaPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El productoId no puede ser nulo")
    private Long productoId;

    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 0, message = "La cantidad no puede ser menor que 0")
    private Integer cantidad;

    public LineaPago(Long productoId, Integer cantidad) {
        this.productoId = productoId;
        this.cantidad = cantidad;
    }

    public LineaPago() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}