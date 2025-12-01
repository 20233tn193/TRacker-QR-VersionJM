package com.tracker.model;

import java.time.Instant;

public class Usuario {
    
    private String id;
    private String email;
    private String password;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String ubicacion; // Municipio de Morelos
    private Role rol;
    private boolean activo;
    private int intentosFallidos;
    private Instant bloqueadoHasta;
    private String secret2FA;
    private boolean habilitado2FA;
    private Instant fechaCreacion;
    private Instant fechaActualizacion;
    
    // Constructores
    public Usuario() {
        this.fechaCreacion = Instant.now();
        this.fechaActualizacion = Instant.now();
        this.activo = true;
        this.intentosFallidos = 0;
        this.habilitado2FA = false;
    }
    
    // Getters y Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getApellidoPaterno() {
        return apellidoPaterno;
    }
    
    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }
    
    public String getApellidoMaterno() {
        return apellidoMaterno;
    }
    
    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }
    
    public String getUbicacion() {
        return ubicacion;
    }
    
    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
    
    // Método de compatibilidad para obtener apellidos completos
    public String getApellidos() {
        if (apellidoMaterno != null && !apellidoMaterno.isEmpty()) {
            return apellidoPaterno + " " + apellidoMaterno;
        }
        return apellidoPaterno;
    }
    
    public Role getRol() {
        return rol;
    }
    
    public void setRol(Role rol) {
        this.rol = rol;
    }
    
    public boolean isActivo() {
        return activo;
    }
    
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
    
    public int getIntentosFallidos() {
        return intentosFallidos;
    }
    
    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }
    
    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }
    
    public void setBloqueadoHasta(Instant bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }
    
    public String getSecret2FA() {
        return secret2FA;
    }
    
    public void setSecret2FA(String secret2FA) {
        this.secret2FA = secret2FA;
    }
    
    public boolean isHabilitado2FA() {
        return habilitado2FA;
    }
    
    public void setHabilitado2FA(boolean habilitado2FA) {
        this.habilitado2FA = habilitado2FA;
    }
    
    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }
    
    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}

