package com.example.backend.models;

import java.math.BigDecimal;

public class Videojuego {
    private Integer id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private String estado;
    private String empresa;
    private String edad_clasificacion;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }
    public String getEdad_clasificacion() { return edad_clasificacion; }
    public void setEdad_clasificacion(String edad_clasificacion) { this.edad_clasificacion = edad_clasificacion; }
}
