package Entidades;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
public class CarritoItem extends PanacheEntity {

    @NotBlank(message = "El userId no puede estar vacío")
    public String userId;

    @NotNull(message = "El productoId no puede ser nulo")
    public Long productoId;

    @NotBlank(message = "El nombre del producto no puede estar vacío")
    public String nombreProducto;

    @NotNull(message = "El precio no puede ser nulo")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que 0")
    public BigDecimal precio;

    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 0, message = "La cantidad no puede ser menor que 0")
    public Integer cantidad;
}
