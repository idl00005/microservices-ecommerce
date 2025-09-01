package com.DTO;

import com.Entidades.Producto;

public class ProductEventDTO {
    private Long productId;
    private String action; // "DELETED" or "UPDATED"
    private Producto producto;

    public ProductEventDTO(Long productId, String action, Producto producto) {
        this.productId = productId;
        this.action = action;
        this.producto = producto;
    }

    // Getters y setters
    public Long getProductId() {
        return productId;
    }

    public String getAction() {
        return action;
    }

    public Producto getProducto() {
        return producto;
    }
}
