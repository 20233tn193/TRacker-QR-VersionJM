package com.tracker.controller;

import com.tracker.dto.ApiResponse;
import com.tracker.dto.ConfirmacionRecepcionRequest;
import com.tracker.dto.PaqueteRequest;
import com.tracker.dto.PaqueteResponse;
import com.tracker.dto.SatisfaccionResponse;
import com.tracker.model.EstadoPaquete;
import com.tracker.service.PaqueteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paquetes")
@CrossOrigin(origins = "*")
@Tag(name = "Paquetes", description = "Gestión de paquetes, códigos QR y confirmación de recepción")
public class PaqueteController {
    
    @Autowired
    private PaqueteService paqueteService;
    
    @Operation(summary = "Crear paquete", description = "Crea un nuevo paquete y genera un código QR único. El empleadoId se obtiene automáticamente de la sesión autenticada.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse> crearPaquete(@Valid @RequestBody PaqueteRequest request, HttpServletRequest httpRequest) {
        try {
            PaqueteResponse response = convertirAPaqueteResponse(paqueteService.crearPaquete(request, httpRequest));
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
    
    @Operation(summary = "Obtener cantidad de paquetes por estado", description = "Obtiene la cantidad de paquetes que tienen un estado específico")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<ApiResponse> obtenerPaquetesPorEstado(@PathVariable String estado) {
        try {
            EstadoPaquete estadoPaquete = EstadoPaquete.valueOf(estado.toUpperCase());
            long cantidad = paqueteService.obtenerPaquetesPorEstado(estadoPaquete);
            return ResponseEntity.ok(ApiResponse.success("Cantidad de paquetes encontrados", cantidad));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Estado inválido. Estados válidos: RECOLECTADO, EN_TRANSITO, ENTREGADO, CANCELADO"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Calcular índice de satisfacción", description = "Calcula el índice de cumplimiento comparando la cantidad total de paquetes contra los paquetes entregados")
    @GetMapping("/satisfaccion")
    public ResponseEntity<ApiResponse> calcularIndiceSatisfaccion() {
        try {
            SatisfaccionResponse response = paqueteService.calcularIndiceSatisfaccion();
            return ResponseEntity.ok(ApiResponse.success("Índice de satisfacción calculado exitosamente", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Calcular índice de satisfacción por repartidor", description = "Calcula el índice de cumplimiento de los paquetes entregados por un repartidor específico. Verifica que el usuario tenga rol EMPLEADO.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/satisfaccion/repartidor/{repartidorId}")
    public ResponseEntity<ApiResponse> calcularIndiceSatisfaccionPorRepartidor(@PathVariable String repartidorId) {
        try {
            SatisfaccionResponse response = paqueteService.calcularIndiceSatisfaccionPorRepartidor(repartidorId);
            return ResponseEntity.ok(ApiResponse.success("Índice de satisfacción del repartidor calculado exitosamente", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Calcular índice de satisfacción por cliente", description = "Calcula el índice de cumplimiento comparando el total de paquetes del cliente contra los paquetes con estado ENTREGADO. Verifica que el usuario tenga rol CLIENTE.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/satisfaccion/cliente/{clienteId}")
    public ResponseEntity<ApiResponse> calcularIndiceSatisfaccionPorCliente(@PathVariable String clienteId) {
        try {
            SatisfaccionResponse response = paqueteService.calcularIndiceSatisfaccionPorCliente(clienteId);
            return ResponseEntity.ok(ApiResponse.success("Índice de satisfacción del cliente calculado exitosamente", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Obtener paquetes filtrados", 
        description = "Obtiene paquetes filtrados por empleado, estado y/o mes. Todos los parámetros son opcionales. Formato de mes: YYYY-MM (ej: 2024-01). Si no se proporciona empleadoId, se obtienen todos los paquetes. Si no se proporciona estado, se obtienen paquetes de todos los estados. Si no se proporciona mes, se obtienen paquetes de todos los meses.", 
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/empleado")
    public ResponseEntity<ApiResponse> obtenerPaquetesPorEmpleado(
            @RequestParam(required = false) String empleadoId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String mes) {
        try {
            EstadoPaquete estadoPaquete = null;
            if (estado != null && !estado.isBlank()) {
                try {
                    estadoPaquete = EstadoPaquete.valueOf(estado.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Estado inválido. Estados válidos: RECOLECTADO, EN_TRANSITO, ENTREGADO, CANCELADO"));
                }
            }
            
            List<PaqueteResponse> paquetes = paqueteService.obtenerPaquetesPorEmpleado(empleadoId, estadoPaquete, mes);
            return ResponseEntity.ok(ApiResponse.success("Paquetes encontrados", paquetes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Obtener 10 paquetes actualizados recientemente", description = "Obtiene los 10 paquetes más recientemente actualizados, ordenados por fechaUltimaActualizacion en orden descendente")
    @GetMapping("/recientes")
    public ResponseEntity<ApiResponse> obtener10PaquetesRecientes() {
        try {
            List<PaqueteResponse> paquetes = paqueteService.obtener10PaquetesRecientes();
            return ResponseEntity.ok(ApiResponse.success("Paquetes recientes encontrados", paquetes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
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
        response.setUbicacion(paquete.getUbicacion());
        response.setEmpleadoId(paquete.getEmpleadoId());
        response.setFechaCreacion(paquete.getFechaCreacion());
        response.setFechaUltimaActualizacion(paquete.getFechaUltimaActualizacion());
        response.setConfirmadoRecepcion(paquete.isConfirmadoRecepcion());
        response.setFechaConfirmacionRecepcion(paquete.getFechaConfirmacionRecepcion());
        return response;
    }
}

