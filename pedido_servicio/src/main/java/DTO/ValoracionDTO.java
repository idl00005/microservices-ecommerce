package DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValoracionDTO {
    private String idUsuario;
    private Long idProducto;
    private int puntuacion;
    private String comentario;

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

    @JsonCreator
    public ValoracionDTO(
            @JsonProperty("idUsuario")   String idUsuario,
            @JsonProperty("idProducto")  Long idProducto,
            @JsonProperty("puntuacion")  int puntuacion,
            @JsonProperty("comentario")  String comentario) {
        this.idUsuario  = idUsuario;
        this.idProducto = idProducto;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
    }

    public ValoracionDTO() {
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}