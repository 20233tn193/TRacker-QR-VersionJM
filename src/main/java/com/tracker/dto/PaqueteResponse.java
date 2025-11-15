package com.tracker.dto;

import com.tracker.model.EstadoPaquete;
import java.time.Instant;
import java.util.List;

public class PaqueteResponse {
    
    private String id;
    private String codigoQR;
    private String descripcion;
    private EstadoPaquete estado;
    private String clienteEmail;
    private String direccionOrigen;
    private String direccionDestino;
    private Instant fechaCreacion;
    private Instant fechaUltimaActualizacion;
    private boolean confirmadoRecepcion;
    private Instant fechaConfirmacionRecepcion;
    private List<MovimientoResponse> historialMovimientos;
    
    // Constructores
    public PaqueteResponse() {}
    
    // Getters y Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCodigoQR() {
        return codigoQR;
    }
    
    public void setCodigoQR(String codigoQR) {
        this.codigoQR = codigoQR;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public EstadoPaquete getEstado() {
        return estado;
    }
    
    public void setEstado(EstadoPaquete estado) {
        this.estado = estado;
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
    
    public String getDireccionDestino() {
        return direccionDestino;
    }
    
    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }
    
    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public Instant getFechaUltimaActualizacion() {
        return fechaUltimaActualizacion;
    }
    
    public void setFechaUltimaActualizacion(Instant fechaUltimaActualizacion) {
        this.fechaUltimaActualizacion = fechaUltimaActualizacion;
    }
    
    public boolean isConfirmadoRecepcion() {
        return confirmadoRecepcion;
    }
    
    public void setConfirmadoRecepcion(boolean confirmadoRecepcion) {
        this.confirmadoRecepcion = confirmadoRecepcion;
    }
    
    public Instant getFechaConfirmacionRecepcion() {
        return fechaConfirmacionRecepcion;
    }
    
    public void setFechaConfirmacionRecepcion(Instant fechaConfirmacionRecepcion) {
        this.fechaConfirmacionRecepcion = fechaConfirmacionRecepcion;
    }
    
    public List<MovimientoResponse> getHistorialMovimientos() {
        return historialMovimientos;
    }
    
    public void setHistorialMovimientos(List<MovimientoResponse> historialMovimientos) {
        this.historialMovimientos = historialMovimientos;
    }
}

