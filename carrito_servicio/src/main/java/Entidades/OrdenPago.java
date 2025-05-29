package Entidades;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class OrdenPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank(message = "El userId no puede estar vacío")
    public String userId;

    @NotNull(message = "El monto total no puede ser nulo")
    @DecimalMin(value = "0.0", message = "El monto total debe ser mayor que 0")
    public BigDecimal montoTotal;

    @NotBlank(message = "El estado no puede estar vacío")
    @Pattern(regexp = "PENDIENTE|CREADO|PAGADO|FALLIDO|CANCELADO|COMPLETADO",
            message = "El estado debe ser PENDIENTE, CREADO, PAGADO, FALLIDO o CANCELADO")
    public String estado;

    public String proveedor;
    public String referenciaExterna;

    @NotNull(message = "La fecha de creación no puede ser nula")
    public LocalDateTime fechaCreacion;

    @NotBlank(message = "La dirección no puede estar vacía")
    public String direccion;

    @NotBlank(message = "El número de teléfono no puede estar vacío")
    @Pattern(regexp = "\\+?[0-9]{9,15}", message = "El número de teléfono debe ser válido")
    public String telefono;

}

