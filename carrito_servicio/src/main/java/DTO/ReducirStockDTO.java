package DTO;

import java.util.Map;

public class ReducirStockDTO {
    private String userId;
    private Map<Long, Integer> productosComprados; // ID del producto y cantidad comprada

    // Getters y setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<Long, Integer> getProductosComprados() {
        return productosComprados;
    }

    public void setProductosComprados(Map<Long, Integer> productosComprados) {
        this.productosComprados = productosComprados;
    }
}