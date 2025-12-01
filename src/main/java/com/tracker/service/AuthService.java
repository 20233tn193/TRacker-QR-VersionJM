package com.tracker.service;

import com.google.cloud.Timestamp;
import com.tracker.dto.LoginRequest;
import com.tracker.dto.LoginResponse;
import com.tracker.model.Usuario;
import com.tracker.repository.UsuarioRepository;
import com.tracker.util.JwtUtil;
import com.tracker.util.TOTPUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class AuthService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private TOTPUtil totpUtil;
    
    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());
        
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Credenciales inválidas");
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Verificar si la cuenta está bloqueada
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().compareTo(Timestamp.now()) > 0) {
            throw new RuntimeException("Cuenta bloqueada temporalmente. Intente más tarde.");
        }
        
        // Verificar si el usuario está activo
        if (!usuario.isActivo()) {
            throw new RuntimeException("Usuario inactivo");
        }
        
        // Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            incrementarIntentosFallidos(usuario);
            throw new RuntimeException("Credenciales inválidas");
        }
        
        // Verificar 2FA si está habilitado
        if (usuario.isHabilitado2FA()) {
            if (request.getCodigo2FA() == null || request.getCodigo2FA().isEmpty()) {
                LoginResponse response = new LoginResponse();
                response.setRequiere2FA(true);
                return response;
            }
            
            if (!totpUtil.verifyCode(usuario.getSecret2FA(), request.getCodigo2FA())) {
                incrementarIntentosFallidos(usuario);
                throw new RuntimeException("Código 2FA inválido");
            }
        }
        
        // Resetear intentos fallidos si la autenticación es exitosa
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        
        // Generar token JWT
        String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getId(), usuario.getRol().name());
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setId(usuario.getId());
        response.setEmail(usuario.getEmail());
        response.setNombre(usuario.getNombre());
        response.setRol(usuario.getRol());
        response.setRequiere2FA(false);
        
        return response;
    }
    
    private void incrementarIntentosFallidos(Usuario usuario) {
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        
        if (usuario.getIntentosFallidos() >= 3) {
            java.util.Date fechaBloqueo = new java.util.Date(
                Timestamp.now().toDate().getTime() + (24 * 60 * 60 * 1000L)
            );
            usuario.setBloqueadoHasta(Timestamp.of(fechaBloqueo));
        }
        
        usuarioRepository.save(usuario);
    }
    
    public String generarSecret2FA(String userId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(userId);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        
        Usuario usuario = usuarioOpt.get();
        String secret = totpUtil.generateSecret();
        usuario.setSecret2FA(secret);
        usuarioRepository.save(usuario);
        
        return secret;
    }
    
    public String generarQRCode2FA(String userId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(userId);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        
        Usuario usuario = usuarioOpt.get();
        if (usuario.getSecret2FA() == null || usuario.getSecret2FA().isEmpty()) {
            generarSecret2FA(userId);
            usuario = usuarioRepository.findById(userId).get();
        }
        
        try {
            return totpUtil.generateQRCodeImageUri(usuario.getEmail(), usuario.getSecret2FA());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar código QR para 2FA", e);
        }
    }
    
    public void habilitar2FA(String userId, String codigo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(userId);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        
        Usuario usuario = usuarioOpt.get();
        if (usuario.getSecret2FA() == null || usuario.getSecret2FA().isEmpty()) {
            throw new RuntimeException("Primero debe generar el secreto 2FA");
        }
        
        if (!totpUtil.verifyCode(usuario.getSecret2FA(), codigo)) {
            throw new RuntimeException("Código 2FA inválido");
        }
        
        usuario.setHabilitado2FA(true);
        usuarioRepository.save(usuario);
    }
}

