package com.tracker.service;

import com.tracker.dto.ConfirmacionRecepcionRequest;
import com.tracker.dto.PaqueteRequest;
import com.tracker.dto.PaqueteResponse;
import com.tracker.dto.MovimientoResponse;
import com.tracker.dto.SatisfaccionResponse;
import com.tracker.model.EstadoPaquete;
import com.tracker.model.Paquete;
import com.tracker.model.Role;
import com.tracker.model.Usuario;
import com.tracker.repository.PaqueteRepository;
import com.tracker.repository.MovimientoRepository;
import com.tracker.repository.UsuarioRepository;
import com.tracker.util.QRCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaqueteService {
    
    @Autowired
    private PaqueteRepository paqueteRepository;
    
    @Autowired
    private MovimientoRepository movimientoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private QRCodeGenerator qrCodeGenerator;
    
    public Paquete crearPaquete(PaqueteRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getClienteEmail());
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Cliente no encontrado con el email proporcionado");
        }
        
        Usuario cliente = usuarioOpt.get();
        if (cliente.getUbicacion() == null || cliente.getUbicacion().isEmpty()) {
            throw new RuntimeException("El cliente no tiene una ubicación registrada");
        }
        
        Paquete paquete = new Paquete();
        paquete.setDescripcion(request.getDescripcion());
        paquete.setClienteEmail(request.getClienteEmail());
        paquete.setDireccionOrigen(cliente.getUbicacion()); 
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
    
    public long obtenerPaquetesPorEstado(EstadoPaquete estado) {
        return paqueteRepository.countByEstado(estado);
    }
    
    public SatisfaccionResponse calcularIndiceSatisfaccion() {
        long totalPaquetes = paqueteRepository.countAll();
        long paquetesEntregados = paqueteRepository.countByEstado(EstadoPaquete.ENTREGADO);
        
        double indiceCumplimiento = 0.0;
        if (totalPaquetes > 0) {
            indiceCumplimiento = (double) paquetesEntregados / totalPaquetes * 100.0;
        }
        
        return new SatisfaccionResponse(totalPaquetes, paquetesEntregados, indiceCumplimiento);
    }
    
    public SatisfaccionResponse calcularIndiceSatisfaccionPorRepartidor(String repartidorId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(repartidorId);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Repartidor no encontrado");
        }
        
        Usuario repartidor = usuarioOpt.get();
        
        if (repartidor.getRol() != Role.EMPLEADO) {
            throw new RuntimeException("El usuario especificado no es un repartidor (EMPLEADO)");
        }
        
        List<com.tracker.model.Movimiento> todosLosMovimientos = movimientoRepository.findByEmpleadoId(repartidorId);
        
        List<com.tracker.model.Movimiento> movimientosEntregados = 
                movimientoRepository.findByEmpleadoIdAndEstado(repartidorId, EstadoPaquete.ENTREGADO);
        
        Set<String> paquetesUnicos = todosLosMovimientos.stream()
                .map(com.tracker.model.Movimiento::getPaqueteId)
                .collect(Collectors.toSet());
        long totalPaquetes = paquetesUnicos.size();
        
        Set<String> paquetesEntregadosUnicos = movimientosEntregados.stream()
                .map(com.tracker.model.Movimiento::getPaqueteId)
                .collect(Collectors.toSet());
        long paquetesEntregados = paquetesEntregadosUnicos.size();
        
        double indiceCumplimiento = 0.0;
        if (totalPaquetes > 0) {
            indiceCumplimiento = (double) paquetesEntregados / totalPaquetes * 100.0;
        }
        
        return new SatisfaccionResponse(totalPaquetes, paquetesEntregados, indiceCumplimiento);
    }
    
    public SatisfaccionResponse calcularIndiceSatisfaccionPorCliente(String clienteId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(clienteId);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Cliente no encontrado");
        }
        
        Usuario cliente = usuarioOpt.get();
        
        if (cliente.getRol() != Role.CLIENTE) {
            throw new RuntimeException("El usuario especificado no es un cliente");
        }
        
        String clienteEmail = cliente.getEmail();
        long totalPaquetes = paqueteRepository.countByClienteEmail(clienteEmail);
        long paquetesEntregados = paqueteRepository.countByClienteEmailAndEstado(clienteEmail, EstadoPaquete.ENTREGADO);
        
        double indiceCumplimiento = 0.0;
        if (totalPaquetes > 0) {
            indiceCumplimiento = (double) paquetesEntregados / totalPaquetes * 100.0;
        }
        
        return new SatisfaccionResponse(totalPaquetes, paquetesEntregados, indiceCumplimiento);
    }
    
    public List<PaqueteResponse> obtenerPaquetesPorEmpleado(String empleadoId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(empleadoId);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Empleado no encontrado");
        }
        
        Usuario empleado = usuarioOpt.get();
        
        if (empleado.getRol() != Role.EMPLEADO) {
            throw new RuntimeException("El usuario especificado no es un empleado");
        }
        
        List<com.tracker.model.Movimiento> movimientos = movimientoRepository.findByEmpleadoId(empleadoId);
        
        Set<String> paquetesIds = movimientos.stream()
                .map(com.tracker.model.Movimiento::getPaqueteId)
                .collect(Collectors.toSet());
        
        return paquetesIds.stream()
                .map(paqueteId -> {
                    Optional<Paquete> paqueteOpt = paqueteRepository.findById(paqueteId);
                    return paqueteOpt.map(this::convertirAPaqueteResponse).orElse(null);
                })
                .filter(paquete -> paquete != null)
                .collect(Collectors.toList());
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

