package com.tracker.dto;

import com.tracker.model.Role;

public class LoginResponse {
    
    private String token;
    private String tipoToken = "Bearer";
    private String id;
    private String email;
    private String nombre;
    private Role rol;
    private boolean requiere2FA;
    
    // Constructores
    public LoginResponse() {}
    
    public LoginResponse(String token, String id, String email, String nombre, Role rol) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.nombre = nombre;
        this.rol = rol;
        this.requiere2FA = false;
    }
    
    // Getters y Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getTipoToken() {
        return tipoToken;
    }
    
    public void setTipoToken(String tipoToken) {
        this.tipoToken = tipoToken;
    }
    
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
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public Role getRol() {
        return rol;
    }
    
    public void setRol(Role rol) {
        this.rol = rol;
    }
    
    public boolean isRequiere2FA() {
        return requiere2FA;
    }
    
    public void setRequiere2FA(boolean requiere2FA) {
        this.requiere2FA = requiere2FA;
    }
}

