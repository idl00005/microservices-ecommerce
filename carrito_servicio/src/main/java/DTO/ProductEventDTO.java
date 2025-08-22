package DTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductEventDTO {
    private long  productId;
    private String action; // "UPDATED" o "DELETED"
    private ProductoDTO producto;

    @JsonCreator
    public ProductEventDTO(@JsonProperty("productId") long productId,
                           @JsonProperty("action") String action,
                           @JsonProperty("producto")ProductoDTO producto) {
        this.productId = productId;
        this.action = action;
        this.producto = producto;
    }

    public ProductEventDTO() {
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setProducto(ProductoDTO producto) {
        this.producto = producto;
    }
    
    public long getProductId() {
        return productId;
    }

    public String getAction() {
        return action;
    }

    public ProductoDTO getProducto() {
        return producto;
    }
}