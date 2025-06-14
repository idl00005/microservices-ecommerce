package Entidades;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
public class CarritoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank(message = "El userId no puede estar vacío")
    public String userId;

    @NotNull(message = "El productoId no puede ser nulo")
    public Long productoId;

    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 0, message = "La cantidad no puede ser menor que 0")
    public Integer cantidad;

    public String getUserId() {
        return userId;
    }
}
