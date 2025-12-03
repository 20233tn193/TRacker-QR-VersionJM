package com.tracker.controller;

import com.tracker.dto.ApiResponse;
import com.tracker.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/reportes")
@CrossOrigin(origins = "*")
public class ReporteController {
    
    @Autowired
    private ReporteService reporteService;
    
    @GetMapping("/trazabilidad")
    public ResponseEntity<byte[]> generarReporteTrazabilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) String empleadoId) {
        try {
            byte[] pdf = reporteService.generarReporteTrazabilidad(inicio, fin, empleadoId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_trazabilidad.pdf");
            
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/estadisticas-entregas")
    public ResponseEntity<ApiResponse> obtenerEstadisticasEntregas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        try {
            Map<String, Long> estadisticas = reporteService.obtenerEstadisticasEntregas(inicio, fin);
            return ResponseEntity.ok(ApiResponse.success("Estadísticas obtenidas", estadisticas));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/paquete/{paqueteId}")
    public ResponseEntity<byte[]> generarReportePaquete(@PathVariable String paqueteId) {
        try {
            byte[] pdf = reporteService.generarReportePaquete(paqueteId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_paquete_" + paqueteId + ".pdf");
            
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

