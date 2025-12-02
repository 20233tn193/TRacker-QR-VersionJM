package com.tracker.controller;

import com.tracker.dto.ApiResponse;
import com.tracker.dto.RecoverPasswordRequest;
import com.tracker.dto.ResetPasswordRequest;
import com.tracker.dto.CrearEmpleadoRequest;
import com.tracker.dto.ActualizarEmpleadoRequest;
import com.tracker.model.Role;
import com.tracker.model.Usuario;
import com.tracker.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*")
@Tag(name = "Usuarios", description = "Gestión de usuarios (solo accesible por ADMINISTRADOR)")
public class UsuarioController {
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Operation(summary = "Crear empleado o administrador", description = "Permite al ADMINISTRADOR crear usuarios con rol EMPLEADO o ADMINISTRADOR")
    @PostMapping
    public ResponseEntity<ApiResponse> crearEmpleado(@Valid @RequestBody CrearEmpleadoRequest request) {
        try {
            request.setRol(Role.EMPLEADO);
            Usuario usuario = usuarioService.crearEmpleado(request);
            return ResponseEntity.ok(ApiResponse.success("Usuario creado exitosamente", usuario));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Actualizar empleado o administrador", description = "Actualiza los datos de un empleado o administrador")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> actualizarUsuario(
            @PathVariable String id,
            @Valid @RequestBody CrearEmpleadoRequest request) {
        try {
            Usuario usuario = usuarioService.actualizarUsuario(id, request);
            return ResponseEntity.ok(ApiResponse.success("Usuario actualizado exitosamente", usuario));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar datos del empleado", description = "Permite al ADMINISTRADOR actualizar nombre, apellidos y email de un empleado sin cambiar el rol")
    @PutMapping("/{id}/datos")
    public ResponseEntity<ApiResponse> actualizarDatosEmpleado(
            @PathVariable String id,
            @Valid @RequestBody ActualizarEmpleadoRequest request) {
        try {
            Usuario usuario = usuarioService.actualizarDatosEmpleado(id, request);
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