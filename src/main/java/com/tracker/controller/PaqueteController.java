package com.tracker.controller;

import com.tracker.dto.ApiResponse;
import com.tracker.dto.ConfirmacionRecepcionRequest;
import com.tracker.dto.PaqueteRequest;
import com.tracker.dto.PaqueteResponse;
import com.tracker.service.PaqueteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/paquetes")
@CrossOrigin(origins = "*")
@Tag(name = "Paquetes", description = "Gestión de paquetes, códigos QR y confirmación de recepción")
public class PaqueteController {
    
    @Autowired
    private PaqueteService paqueteService;
    
    @Operation(summary = "Crear paquete", description = "Crea un nuevo paquete y genera un código QR único", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse> crearPaquete(@Valid @RequestBody PaqueteRequest request) {
        try {
            PaqueteResponse response = convertirAPaqueteResponse(paqueteService.crearPaquete(request));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Paquete creado exitosamente", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Consultar paquete por QR", description = "Consulta la información de un paquete usando su código QR. Endpoint público, no requiere autenticación.")
    @GetMapping("/qr/{codigoQR}")
    public ResponseEntity<ApiResponse> consultarPaquetePorQR(@PathVariable String codigoQR) {
        try {
            PaqueteResponse response = paqueteService.consultarPaquetePorQR(codigoQR);
            return ResponseEntity.ok(ApiResponse.success("Paquete encontrado", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> consultarPaquetePorId(@PathVariable String id) {
        try {
            PaqueteResponse response = paqueteService.consultarPaquetePorId(id);
            return ResponseEntity.ok(ApiResponse.success("Paquete encontrado", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/cliente/{clienteEmail}")
    public ResponseEntity<ApiResponse> obtenerPaquetesPorCliente(@PathVariable String clienteEmail) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Paquetes encontrados",
                    paqueteService.obtenerPaquetesPorCliente(clienteEmail)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/qr/{codigoQR}/confirmar-recepcion")
    public ResponseEntity<ApiResponse> confirmarRecepcion(
            @PathVariable String codigoQR,
            @Valid @RequestBody ConfirmacionRecepcionRequest request) {
        try {
            paqueteService.confirmarRecepcion(codigoQR, request);
            return ResponseEntity.ok(ApiResponse.success("Recepción confirmada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/qr/{codigoQR}/imagen")
    public ResponseEntity<byte[]> generarQRCodeImage(@PathVariable String codigoQR) {
        try {
            byte[] qrCodeImage = paqueteService.generarQRCodeImage(codigoQR);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private PaqueteResponse convertirAPaqueteResponse(com.tracker.model.Paquete paquete) {
        PaqueteResponse response = new PaqueteResponse();
        response.setId(paquete.getId());
        response.setCodigoQR(paquete.getCodigoQR());
        response.setDescripcion(paquete.getDescripcion());
        response.setEstado(paquete.getEstado());
        response.setClienteEmail(paquete.getClienteEmail());
        response.setDireccionOrigen(paquete.getDireccionOrigen());
        response.setDireccionDestino(paquete.getDireccionDestino());
        response.setFechaCreacion(paquete.getFechaCreacion());
        response.setFechaUltimaActualizacion(paquete.getFechaUltimaActualizacion());
        response.setConfirmadoRecepcion(paquete.isConfirmadoRecepcion());
        response.setFechaConfirmacionRecepcion(paquete.getFechaConfirmacionRecepcion());
        return response;
    }
}

