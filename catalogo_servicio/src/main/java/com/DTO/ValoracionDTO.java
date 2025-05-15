package com.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValoracionDTO {

    @JsonProperty("idUsuario")
    private String idUsuario;

    @JsonProperty("idProducto")
    private Long idProducto;

    @JsonProperty("puntuacion")
    private int puntuacion;

    @JsonProperty("comentario")
    private String comentario;

    // Constructor sin argumentos (requerido por Jackson)
    public ValoracionDTO() {
    }

    // Constructor con argumentos
    public ValoracionDTO(String idUsuario, Long idProducto, int puntuacion, String comentario) {
        this.idUsuario = idUsuario;
        this.idProducto = idProducto;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
    }

    // Getters y Setters
    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}