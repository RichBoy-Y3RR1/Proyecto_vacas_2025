package com.example.backend.models;

import java.time.Instant;

public class Comentario {
    private Integer id;
    private Integer usuario_id;
    private Integer videojuego_id;
    private String texto;
    private Integer puntuacion;
    private Instant fecha;
    private Boolean visible;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUsuario_id() { return usuario_id; }
    public void setUsuario_id(Integer usuario_id) { this.usuario_id = usuario_id; }
    public Integer getVideojuego_id() { return videojuego_id; }
    public void setVideojuego_id(Integer videojuego_id) { this.videojuego_id = videojuego_id; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public Integer getPuntuacion() { return puntuacion; }
    public void setPuntuacion(Integer puntuacion) { this.puntuacion = puntuacion; }
    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
}
