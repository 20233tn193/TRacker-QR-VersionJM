package com.tracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PaqueteRequest {
    
    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;
    
    @NotBlank(message = "El email del cliente es obligatorio")
    @Email(message = "El email debe ser válido")
    private String clienteEmail;
    
    
    @NotBlank(message = "La dirección de origen es obligatoria")
    private String direccionOrigen;
    
    private String empleadoId;
    private String ubicacion;
    
    // Constructores
    public PaqueteRequest() {}
    
    // Getters y Setters
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getClienteEmail() {
        return clienteEmail;
    }
    
    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }
    
    public String getDireccionOrigen() {
        return direccionOrigen;
    }
    
    public void setDireccionOrigen(String direccionOrigen) {
        this.direccionOrigen = direccionOrigen;
    }
    
    public String getEmpleadoId() {
        return empleadoId;
    }
    
    public void setEmpleadoId(String empleadoId) {
        this.empleadoId = empleadoId;
    }
    
    public String getUbicacion() {
        return ubicacion;
    }
    
    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
}

