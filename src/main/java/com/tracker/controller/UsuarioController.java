package com.tracker.controller;

import com.tracker.dto.ApiResponse;
import com.tracker.dto.RecoverPasswordRequest;
import com.tracker.dto.ResetPasswordRequest;
import com.tracker.dto.UsuarioRequest;
import com.tracker.model.Role;
import com.tracker.model.Usuario;
import com.tracker.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {
    
    @Autowired
    private UsuarioService usuarioService;
    
    @PostMapping
    public ResponseEntity<ApiResponse> crearUsuario(@Valid @RequestBody UsuarioRequest request) {
        try {
            Usuario usuario = usuarioService.crearUsuario(request);
            return ResponseEntity.ok(ApiResponse.success("Usuario creado exitosamente", usuario));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> actualizarUsuario(
            @PathVariable String id,
            @Valid @RequestBody UsuarioRequest request) {
        try {
            Usuario usuario = usuarioService.actualizarUsuario(id, request);
            return ResponseEntity.ok(ApiResponse.success("Usuario actualizado exitosamente", usuario));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse> obtenerTodosLosUsuarios() {
        try {
            List<Usuario> usuarios = usuarioService.obtenerTodosLosUsuarios();
            return ResponseEntity.ok(ApiResponse.success("Usuarios encontrados", usuarios));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> obtenerUsuarioPorId(@PathVariable String id) {
        try {
            return usuarioService.obtenerUsuarioPorId(id)
                    .map(usuario -> ResponseEntity.ok(ApiResponse.success("Usuario encontrado", usuario)))
                    .orElse(ResponseEntity.badRequest().body(ApiResponse.error("Usuario no encontrado")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/rol/{rol}")
    public ResponseEntity<ApiResponse> obtenerUsuariosPorRol(@PathVariable Role rol) {
        try {
            List<Usuario> usuarios = usuarioService.obtenerUsuariosPorRol(rol);
            return ResponseEntity.ok(ApiResponse.success("Usuarios encontrados", usuarios));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse> desactivarUsuario(@PathVariable String id) {
        try {
            usuarioService.desactivarUsuario(id);
            return ResponseEntity.ok(ApiResponse.success("Usuario desactivado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/activar")
    public ResponseEntity<ApiResponse> activarUsuario(@PathVariable String id) {
        try {
            usuarioService.activarUsuario(id);
            return ResponseEntity.ok(ApiResponse.success("Usuario activado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/recoverPassword")
    public ResponseEntity<ApiResponse> recuperarPassword(@Valid @RequestBody RecoverPasswordRequest request) {
        try {
            usuarioService.solicitarRecuperacionPassword(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(
                "Si el email existe en nuestro sistema, recibirás un correo con las instrucciones para recuperar tu contraseña"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/resetPassword")
    public ResponseEntity<ApiResponse> resetearPassword(
            @RequestParam(required = false) String token,
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            String tokenFinal = (token != null && !token.isEmpty()) ? token : request.getToken();
            
            usuarioService.resetearPassword(
                tokenFinal,
                request.getPassword(),
                request.getConfirmPassword()
            );
            return ResponseEntity.ok(ApiResponse.success("Contraseña restablecida exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

