package com.tracker.controller;

import com.tracker.dto.ApiResponse;
import com.tracker.dto.MovimientoRequest;
import com.tracker.dto.MovimientoResponse;
import com.tracker.service.MovimientoService;
import com.tracker.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/movimientos")
@CrossOrigin(origins = "*")
public class MovimientoController {
    
    @Autowired
    private MovimientoService movimientoService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping
    public ResponseEntity<ApiResponse> registrarMovimiento(
            @Valid @RequestBody MovimientoRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = extractToken(httpRequest);
            String empleadoId = jwtUtil.extractUserId(token);
            
            MovimientoResponse response = movimientoService.registrarMovimiento(request, empleadoId);
            return ResponseEntity.ok(ApiResponse.success("Movimiento registrado exitosamente", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/paquete/{paqueteId}")
    public ResponseEntity<ApiResponse> obtenerMovimientosPorPaquete(@PathVariable String paqueteId) {
        List<MovimientoResponse> movimientos = movimientoService.obtenerMovimientosPorPaquete(paqueteId);
        return ResponseEntity.ok(ApiResponse.success("Movimientos encontrados", movimientos));
    }
    
    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<ApiResponse> obtenerMovimientosPorEmpleado(@PathVariable String empleadoId) {
        try {
            List<MovimientoResponse> movimientos = movimientoService.obtenerMovimientosPorEmpleado(empleadoId);
            return ResponseEntity.ok(ApiResponse.success("Movimientos encontrados", movimientos));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/rango-fechas")
    public ResponseEntity<ApiResponse> obtenerMovimientosPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        try {
            List<MovimientoResponse> movimientos = movimientoService.obtenerMovimientosPorRangoFechas(inicio, fin);
            return ResponseEntity.ok(ApiResponse.success("Movimientos encontrados", movimientos));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("Token no encontrado");
    }
}

