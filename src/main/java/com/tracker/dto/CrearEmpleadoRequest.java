package com.tracker.dto;

import com.tracker.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para crear usuarios con rol EMPLEADO o ADMINISTRADOR
 * Solo puede ser usado por usuarios con rol ADMINISTRADOR
 */
public class CrearEmpleadoRequest {
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;
    
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoPaterno;
    
    private String apellidoMaterno; // Opcional
    
    @NotNull(message = "El rol es obligatorio")
    private Role rol; // EMPLEADO o ADMINISTRADOR
    
    // Constructores
    public CrearEmpleadoRequest() {}
    
    // Getters y Setters
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
    
    public Role getRol() {
        return rol;
    }
    
    public void setRol(Role rol) {
        this.rol = rol;
    }
}
