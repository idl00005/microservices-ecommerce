package Servicios;

import Cliente.ProductoClient;
import DTO.ProductoDTO;
import Entidades.CarritoItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.orm.panache.Panache;

@ApplicationScoped
public class CarritoService {

    @Inject
    ProductoClient productoClient;

    public CarritoItem agregarProducto(String userId, String productoId, int cantidad) {
        ProductoDTO producto = productoClient.obtenerProductoPorId(productoId);

        CarritoItem item = new CarritoItem();
        item.userId = userId;
        item.productoId = producto.id;
        item.nombreProducto = producto.nombre;
        item.precio = producto.precio;
        item.cantidad = cantidad;
        item.persist();

        return item;
    }
}

