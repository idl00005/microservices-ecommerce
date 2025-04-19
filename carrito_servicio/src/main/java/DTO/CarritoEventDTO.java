package DTO;

import Entidades.CarritoItem;

import java.util.List;

public class CarritoEventDTO {
    private String userId;
    private List<CarritoItem> items;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CarritoItem> getItems() {
        return items;
    }

    public void setItems(List<CarritoItem> items) {
        this.items = items;
    }
}