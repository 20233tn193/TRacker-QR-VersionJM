package com.tracker.service;

import com.tracker.dto.CrearEmpleadoRequest;
import com.tracker.model.Role;
import com.tracker.model.Usuario;
import com.tracker.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Crear empleado o administrador (solo por ADMIN)
     */
    public Usuario crearEmpleado(CrearEmpleadoRequest request) {
        // Validar que el rol sea EMPLEADO o ADMINISTRADOR
        if (request.getRol() != Role.EMPLEADO && request.getRol() != Role.ADMINISTRADOR) {
            throw new RuntimeException("Solo se pueden crear usuarios con rol EMPLEADO o ADMINISTRADOR");
        }
        
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        
        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setNombre(request.getNombre());
        usuario.setApellidoPaterno(request.getApellidoPaterno());
        usuario.setApellidoMaterno(request.getApellidoMaterno());
        usuario.setUbicacion(null); // Empleados/Admins no tienen ubicación
        usuario.setRol(request.getRol());
        usuario.setActivo(true);
        usuario.setHabilitado2FA(true);
        usuario.setFechaCreacion(Instant.now());
        usuario.setFechaActualizacion(Instant.now());
        
        return usuarioRepository.save(usuario);
    }
    
    public Usuario actualizarUsuario(String id, CrearEmpleadoRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Verificar si el email cambió y no está en uso
        if (!usuario.getEmail().equals(request.getEmail()) && 
            usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está en uso");
        }
        
        usuario.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        usuario.setNombre(request.getNombre());
        usuario.setApellidoPaterno(request.getApellidoPaterno());
        usuario.setApellidoMaterno(request.getApellidoMaterno());
        // No actualizar ubicación para empleados/admins
        usuario.setRol(request.getRol());
        usuario.setFechaActualizacion(Instant.now());
        
        return usuarioRepository.save(usuario);
    }
    
    public void desactivarUsuario(String id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        
        Usuario usuario = usuarioOpt.get();
        usuario.setActivo(false);
        usuario.setFechaActualizacion(Instant.now());
        usuarioRepository.save(usuario);
    }
    
    public void activarUsuario(String id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        
        Usuario usuario = usuarioOpt.get();
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setFechaActualizacion(Instant.now());
        usuarioRepository.save(usuario);
    }
    
    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }
    
    public List<Usuario> obtenerUsuariosPorRol(Role rol) {
        return usuarioRepository.findByRol(rol);
    }
    
    public Optional<Usuario> obtenerUsuarioPorId(String id) {
        return usuarioRepository.findById(id);
    }
    
    public Optional<Usuario> obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }
}

