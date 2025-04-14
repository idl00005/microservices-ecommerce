package Otros;
import DTO.ProductoDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductEvent {
    private String  productId;
    private String action; // "UPDATED" o "DELETED"
    private ProductoDTO producto;

    @JsonCreator
    public ProductEvent(@JsonProperty("productId") String productId,
                        @JsonProperty("action") String action,
                        @JsonProperty("producto")ProductoDTO producto) {
        this.productId = productId;
        this.action = action;
        this.producto = producto;
    }

    public ProductEvent() {
    }

    // Getters y setters
    public String getProductId() {
        return productId;
    }

    public String getAction() {
        return action;
    }

    public ProductoDTO getProducto() {
        return producto;
    }
}