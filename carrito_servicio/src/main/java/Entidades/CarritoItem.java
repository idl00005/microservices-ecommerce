package Entidades;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class CarritoItem extends PanacheEntity {

    public String userId;

    public String productoId;
    public String nombreProducto;
    public BigDecimal precio;

    public Integer cantidad;
}
