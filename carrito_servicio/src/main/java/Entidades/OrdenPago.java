package Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class OrdenPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El userId no puede estar vacío")
    private String userId;

    @NotNull(message = "El monto total no puede ser nulo")
    @DecimalMin(value = "0.0", message = "El monto total debe ser mayor que 0")
    private BigDecimal montoTotal;

    @NotBlank(message = "El estado no puede estar vacío")
    @Pattern(regexp = "PENDIENTE|CREADO|PAGADO|FALLIDO|CANCELADO|COMPLETADO",
            message = "El estado debe ser PENDIENTE, CREADO, PAGADO, FALLIDO o CANCELADO")
    private String estado;

    private String proveedor;
    private String referenciaExterna;

    @NotNull(message = "La fecha de creación no puede ser nula")
    private LocalDateTime fechaCreacion;

    @NotBlank(message = "La dirección no puede estar vacía")
    private String direccion;

    @NotBlank(message = "El número de teléfono no puede estar vacío")
    @Pattern(regexp = "\\+?[0-9]{9,15}", message = "El número de teléfono debe ser válido")
    private String telefono;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaPago> itemsComprados = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public List<LineaPago> getItemsComprados() {
        return itemsComprados;
    }

    public void setItemsComprados(List<LineaPago> itemsComprados) {
        this.itemsComprados = itemsComprados;
    }
}

