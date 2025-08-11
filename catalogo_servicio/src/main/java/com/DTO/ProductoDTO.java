package com.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;

public class ProductoDTO {

    private Long id;

    @NotBlank(message = "El nombre del producto no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(max = 255, message = "La descripción no puede tener más de 255 caracteres")
    private String descripcion;

    @NotNull(message = "El precio del producto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio debe ser mayor que 0")
    @Digits(integer = 10, fraction = 2, message = "El precio debe tener un formato válido (hasta 10 dígitos enteros y 2 decimales)")
    private BigDecimal precio;

    @NotBlank(message = "La categoría no puede estar vacía")
    @Size(max = 50, message = "La categoría no puede tener más de 50 caracteres")
    private String categoria;

    @NotNull(message = "El stock no puede ser nulo")
    @Min(value = 0, message = "El stock no puede ser menor que 0")
    private Integer stock;

    @Type(JsonBinaryType.class)
    private JsonNode detalles;

    public ProductoDTO() {
    }
    public ProductoDTO(Long id,String nombre, String descripcion, BigDecimal precio, int stock, String categoria, JsonNode detalles) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.categoria = categoria;
        this.detalles = detalles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public JsonNode getDetalles() {
        return detalles;
    }

    public void setDetalles(JsonNode detalles) {
        this.detalles = detalles;
    }
}
