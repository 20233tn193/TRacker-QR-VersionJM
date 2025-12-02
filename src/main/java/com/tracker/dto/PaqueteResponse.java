package com.tracker.dto;

import com.google.cloud.Timestamp;
import com.tracker.model.EstadoPaquete;
import java.util.List;

public class PaqueteResponse {
    
    private String id;
    private String codigoQR;
    private String descripcion;
    private EstadoPaquete estado;
    private String clienteEmail;
    private String direccionOrigen;
    private String direccionDestino;
    private String ubicacion;
    private String empleadoId;
    private Timestamp fechaCreacion;
    private Timestamp fechaUltimaActualizacion;
    private boolean confirmadoRecepcion;
    private Timestamp fechaConfirmacionRecepcion;
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
    
    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public Timestamp getFechaUltimaActualizacion() {
        return fechaUltimaActualizacion;
    }
    
    public void setFechaUltimaActualizacion(Timestamp fechaUltimaActualizacion) {
        this.fechaUltimaActualizacion = fechaUltimaActualizacion;
    }
    
    public boolean isConfirmadoRecepcion() {
        return confirmadoRecepcion;
    }
    
    public void setConfirmadoRecepcion(boolean confirmadoRecepcion) {
        this.confirmadoRecepcion = confirmadoRecepcion;
    }
    
    public Timestamp getFechaConfirmacionRecepcion() {
        return fechaConfirmacionRecepcion;
    }
    
    public void setFechaConfirmacionRecepcion(Timestamp fechaConfirmacionRecepcion) {
        this.fechaConfirmacionRecepcion = fechaConfirmacionRecepcion;
    }
    
    public List<MovimientoResponse> getHistorialMovimientos() {
        return historialMovimientos;
    }
    
    public void setHistorialMovimientos(List<MovimientoResponse> historialMovimientos) {
        this.historialMovimientos = historialMovimientos;
    }
}

