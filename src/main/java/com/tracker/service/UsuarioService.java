package com.tracker.service;

import com.google.cloud.Timestamp;
import com.tracker.dto.UsuarioRequest;
import com.tracker.model.Role;
import com.tracker.model.Usuario;
import com.tracker.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    public Usuario crearUsuario(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        
        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setRol(request.getRol());
        usuario.setActivo(true);
        usuario.setFechaCreacion(Timestamp.now());
        usuario.setFechaActualizacion(Timestamp.now());
        
        return usuarioRepository.save(usuario);
    }
    
    public Usuario actualizarUsuario(String id, UsuarioRequest request) {
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
        usuario.setApellidos(request.getApellidos());
        usuario.setRol(request.getRol());
        usuario.setFechaActualizacion(Timestamp.now());
        
        return usuarioRepository.save(usuario);
    }
    
    public void desactivarUsuario(String id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        
        Usuario usuario = usuarioOpt.get();
        usuario.setActivo(false);
        usuario.setFechaActualizacion(Timestamp.now());
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
        usuario.setFechaActualizacion(Timestamp.now());
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
    
    public void solicitarRecuperacionPassword(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        
        if (usuarioOpt.isEmpty()) {
            return;
        }
        
        Usuario usuario = usuarioOpt.get();
        
        String token = UUID.randomUUID().toString();
        
        Timestamp ahora = Timestamp.now();
        long segundosEnUnaHora = 3600;
        Timestamp expiracion = Timestamp.ofTimeSecondsAndNanos(
            ahora.getSeconds() + segundosEnUnaHora,
            ahora.getNanos()
        );
        
        usuario.setPasswordResetToken(token);
        usuario.setPasswordResetTokenExpiry(expiracion);
        usuario.setFechaActualizacion(Timestamp.now());
        
        usuarioRepository.save(usuario);
        
        emailService.enviarEmailRecuperacionPassword(email, token);
    }
    
    public void resetearPassword(String token, String password, String confirmPassword) {
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("El token es obligatorio");
        }
        
        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }
        
        Optional<Usuario> usuarioOpt = usuarioRepository.findByPasswordResetToken(token);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Token de recuperación inválido o expirado");
        }
        
        Usuario usuario = usuarioOpt.get();
        
        Timestamp ahora = Timestamp.now();
        if (usuario.getPasswordResetTokenExpiry() == null || 
            usuario.getPasswordResetTokenExpiry().compareTo(ahora) < 0) {
            throw new RuntimeException("Token de recuperación expirado");
        }
        
        if (!token.equals(usuario.getPasswordResetToken())) {
            throw new RuntimeException("Token de recuperación inválido");
        }
        
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setPasswordResetToken(null);
        usuario.setPasswordResetTokenExpiry(null);
        usuario.setFechaActualizacion(Timestamp.now());
        
        usuarioRepository.save(usuario);
    }
}

