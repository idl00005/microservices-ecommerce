package DTO;

import java.util.List;

public class CarritoEventDTO {
    private String userId;
    private List<CarritoItemDTO> items;
    private String direccion;
    private String telefono;

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
}
