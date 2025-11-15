package com.tracker.dto;

import com.tracker.model.EstadoPaquete;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MovimientoRequest {
    
    @NotBlank(message = "El ID del paquete es obligatorio")
    private String paqueteId;
    
    @NotNull(message = "El estado es obligatorio")
    private EstadoPaquete estado;
    
    @NotBlank(message = "La ubicación es obligatoria")
    private String ubicacion;
    
    private String observaciones;
    
    // Constructores
    public MovimientoRequest() {}
    
    // Getters y Setters
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
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

