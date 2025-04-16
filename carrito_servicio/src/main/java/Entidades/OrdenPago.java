package Entidades;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class OrdenPago extends PanacheEntity {
    public String userId;
    public BigDecimal montoTotal;
    public String estado; // ej: "PENDIENTE", "PAGADO", "FALLIDO"
    public String proveedor; // "Stripe", "MercadoPago", etc.
    public String referenciaExterna; // ID generado por el proveedor
    public LocalDateTime fechaCreacion;
}

