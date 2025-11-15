package com.tracker.controller;

import com.tracker.dto.ApiResponse;
import com.tracker.dto.LoginRequest;
import com.tracker.dto.LoginResponse;
import com.tracker.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Autenticación", description = "Endpoints para autenticación, login y gestión de 2FA")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario y devuelve un token JWT. Si el usuario tiene 2FA habilitado, se requiere el código 2FA.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login exitoso", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Generar secreto 2FA", description = "Genera un secreto para autenticación de dos factores para un usuario")
    @PostMapping("/2fa/generar/{userId}")
    public ResponseEntity<ApiResponse> generarSecret2FA(@PathVariable String userId) {
        try {
            String secret = authService.generarSecret2FA(userId);
            return ResponseEntity.ok(ApiResponse.success("Secreto 2FA generado", secret));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Obtener código QR para 2FA", description = "Genera un código QR que puede ser escaneado con una app autenticadora (Google Authenticator, Authy, etc.)")
    @GetMapping("/2fa/qrcode/{userId}")
    public ResponseEntity<ApiResponse> generarQRCode2FA(@PathVariable String userId) {
        try {
            String qrCode = authService.generarQRCode2FA(userId);
            return ResponseEntity.ok(ApiResponse.success("Código QR generado", qrCode));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Habilitar 2FA", description = "Habilita la autenticación de dos factores para un usuario después de verificar el código")
    @PostMapping("/2fa/habilitar/{userId}")
    public ResponseEntity<ApiResponse> habilitar2FA(
            @PathVariable String userId,
            @RequestParam String codigo) {
        try {
            authService.habilitar2FA(userId, codigo);
            return ResponseEntity.ok(ApiResponse.success("2FA habilitado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

