package com.DTO;

public class ValoracionDTO {
    private String idUsuario;
    private Long idProducto;
    private int puntuacion;
    private String comentario;


    public ValoracionDTO(String usuarioId, Long idProducto, int puntuacion, String comentario) {
        this.idUsuario = usuarioId;
        this.idProducto = idProducto;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
    }

    public ValoracionDTO() {
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public String getComentario() {
        return comentario;
    }
}