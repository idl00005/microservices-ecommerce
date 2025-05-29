package DTO;

import java.util.List;

public class CarritoEventDTO {
    private String userId;
    private Long ordenId;
    private List<CarritoItemDTO> items;

    // Getters y setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CarritoItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CarritoItemDTO> items) {
        this.items = items;
    }
    public Long getOrdenId() {
        return ordenId;
    }
}
