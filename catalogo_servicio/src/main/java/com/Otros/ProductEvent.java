package com.Otros;

import com.Entidades.Producto;

public class ProductEvent {
    private Long productId;
    private String action; // "UPDATED" o "DELETED"
    private Producto producto;

    public ProductEvent(Long productId, String action, Producto producto) {
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
