package com.Otros; // Cambia el paquete si lo necesitas

import com.Entidades.Producto; // Asegúrate de importar tu clase Producto

public class ResponseMessage {
    private String message;
    private Producto producto;

    // Constructor por defecto (requerido por algunas bibliotecas de serialización JSON)
    public ResponseMessage() {}

    // Constructor con parámetros
    public ResponseMessage(String message, Producto producto) {
        this.message = message;
        this.producto = producto;
    }

    // Getters y setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}
