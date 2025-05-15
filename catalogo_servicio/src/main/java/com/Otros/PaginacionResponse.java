package com.Otros;

import java.util.List;

public class PaginacionResponse<T> {
    private List<T> datos;
    private int pagina;
    private int tamanio;
    private long total;

    public PaginacionResponse(List<T> datos, int pagina, int tamanio, long total) {
        this.datos = datos;
        this.pagina = pagina;
        this.tamanio = tamanio;
        this.total = total;
    }

    public List<T> getDatos() {
        return datos;
    }

    public int getPagina() {
        return pagina;
    }

    public int getTamanio() {
        return tamanio;
    }

    public long getTotal() {
        return total;
    }
}