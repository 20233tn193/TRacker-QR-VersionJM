package com.tracker.service;

import com.tracker.dto.ConfirmacionRecepcionRequest;
import com.tracker.dto.PaqueteRequest;
import com.tracker.dto.PaqueteResponse;
import com.tracker.dto.MovimientoResponse;
import com.tracker.model.EstadoPaquete;
import com.tracker.model.Paquete;
import com.tracker.repository.PaqueteRepository;
import com.tracker.repository.MovimientoRepository;
import com.tracker.util.QRCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaqueteService {
    
    @Autowired
    private PaqueteRepository paqueteRepository;
    
    @Autowired
    private MovimientoRepository movimientoRepository;
    
    @Autowired
    private QRCodeGenerator qrCodeGenerator;
    
    public Paquete crearPaquete(PaqueteRequest request) {
        Paquete paquete = new Paquete();
        paquete.setDescripcion(request.getDescripcion());
        paquete.setClienteEmail(request.getClienteEmail());
        paquete.setDireccionOrigen(request.getDireccionOrigen());
        paquete.setDireccionDestino(request.getDireccionDestino());
        paquete.setEstado(EstadoPaquete.RECOLECTADO);
        
        // Generar código QR único
        String codigoQR = generarCodigoQRUnico();
        paquete.setCodigoQR(codigoQR);
        
        return paqueteRepository.save(paquete);
    }
    
    private String generarCodigoQRUnico() {
        String codigo;
        do {
            codigo = "PKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (paqueteRepository.findByCodigoQR(codigo).isPresent());
        
        return codigo;
    }
    
    public PaqueteResponse consultarPaquetePorQR(String codigoQR) {
        Optional<Paquete> paqueteOpt = paqueteRepository.findByCodigoQR(codigoQR);
        if (paqueteOpt.isEmpty()) {
            throw new RuntimeException("Paquete no encontrado");
        }
        
        Paquete paquete = paqueteOpt.get();
        PaqueteResponse response = convertirAPaqueteResponse(paquete);
        
        // Obtener historial de movimientos
        List<MovimientoResponse> movimientos = movimientoRepository
                .findByPaqueteIdOrderByFechaHoraDesc(paquete.getId())
                .stream()
                .map(this::convertirAMovimientoResponse)
                .collect(Collectors.toList());
        
        response.setHistorialMovimientos(movimientos);
        
        return response;
    }
    
    public PaqueteResponse consultarPaquetePorId(String id) {
        Optional<Paquete> paqueteOpt = paqueteRepository.findById(id);
        if (paqueteOpt.isEmpty()) {
            throw new RuntimeException("Paquete no encontrado");
        }
        
        Paquete paquete = paqueteOpt.get();
        PaqueteResponse response = convertirAPaqueteResponse(paquete);
        
        // Obtener historial de movimientos
        List<MovimientoResponse> movimientos = movimientoRepository
                .findByPaqueteIdOrderByFechaHoraDesc(paquete.getId())
                .stream()
                .map(this::convertirAMovimientoResponse)
                .collect(Collectors.toList());
        
        response.setHistorialMovimientos(movimientos);
        
        return response;
    }
    
    public List<PaqueteResponse> obtenerPaquetesPorCliente(String clienteEmail) {
        return paqueteRepository.findByClienteEmail(clienteEmail)
                .stream()
                .map(this::convertirAPaqueteResponse)
                .collect(Collectors.toList());
    }
    
    public void confirmarRecepcion(String codigoQR, ConfirmacionRecepcionRequest request) {
        Optional<Paquete> paqueteOpt = paqueteRepository.findByCodigoQR(codigoQR);
        if (paqueteOpt.isEmpty()) {
            throw new RuntimeException("Paquete no encontrado");
        }
        
        Paquete paquete = paqueteOpt.get();
        
        if (paquete.getEstado() != EstadoPaquete.EN_TRANSITO) {
            throw new RuntimeException("El paquete debe estar en tránsito para confirmar recepción");
        }
        
        paquete.setConfirmadoRecepcion(true);
        paquete.setFirmaDigital(request.getFirmaDigital());
        paquete.setEstado(EstadoPaquete.ENTREGADO);
        
        paqueteRepository.save(paquete);
    }
    
    public byte[] generarQRCodeImage(String codigoQR) {
        try {
            return qrCodeGenerator.generateQRCodeImage(codigoQR);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar código QR", e);
        }
    }
    
    private PaqueteResponse convertirAPaqueteResponse(Paquete paquete) {
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
    
    private MovimientoResponse convertirAMovimientoResponse(com.tracker.model.Movimiento movimiento) {
        MovimientoResponse response = new MovimientoResponse();
        response.setId(movimiento.getId());
        response.setPaqueteId(movimiento.getPaqueteId());
        response.setEstado(movimiento.getEstado());
        response.setUbicacion(movimiento.getUbicacion());
        response.setEmpleadoId(movimiento.getEmpleadoId());
        response.setEmpleadoNombre(movimiento.getEmpleadoNombre());
        response.setFechaHora(movimiento.getFechaHora());
        response.setObservaciones(movimiento.getObservaciones());
        return response;
    }
}

