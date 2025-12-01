package com.tracker.model;

import com.google.cloud.Timestamp;

public class Movimiento {
    
    private String id;
    private String paqueteId;
    private EstadoPaquete estado;
    private String ubicacion;
    private String empleadoId;
    private String empleadoNombre;
    private Timestamp fechaHora;
    private String observaciones;
    
    // Constructores
    public Movimiento() {
        this.fechaHora = Timestamp.now();
    }
    
    // Getters y Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getPaqueteId() {
        return paqueteId;
    }
    
    public void setPaqueteId(String paqueteId) {
        this.paqueteId = paqueteId;
    }
    
    public EstadoPaquete getEstado() {
        return estado;
    }
    
    public void setEstado(EstadoPaquete estado) {
        this.estado = estado;
    }
    
    public String getUbicacion() {
        return ubicacion;
    }
    
    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
    
    public String getEmpleadoId() {
        return empleadoId;
    }
    
    public void setEmpleadoId(String empleadoId) {
        this.empleadoId = empleadoId;
    }
    
    public String getEmpleadoNombre() {
        return empleadoNombre;
    }
    
    public void setEmpleadoNombre(String empleadoNombre) {
        this.empleadoNombre = empleadoNombre;
    }
    
    public Timestamp getFechaHora() {
        return fechaHora;
    }
    
    public void setFechaHora(Timestamp fechaHora) {
        this.fechaHora = fechaHora;
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

