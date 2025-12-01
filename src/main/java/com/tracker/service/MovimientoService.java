package com.tracker.service;

import com.google.cloud.Timestamp;
import com.tracker.dto.MovimientoRequest;
import com.tracker.dto.MovimientoResponse;
import com.tracker.model.EstadoPaquete;
import com.tracker.model.Movimiento;
import com.tracker.model.Paquete;
import com.tracker.model.Usuario;
import com.tracker.repository.MovimientoRepository;
import com.tracker.repository.PaqueteRepository;
import com.tracker.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MovimientoService {
    
    @Autowired
    private MovimientoRepository movimientoRepository;
    
    @Autowired
    private PaqueteRepository paqueteRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    public MovimientoResponse registrarMovimiento(MovimientoRequest request, String empleadoId) {
        // Validar que el paquete existe
        Optional<Paquete> paqueteOpt = paqueteRepository.findById(request.getPaqueteId());
        if (paqueteOpt.isEmpty()) {
            throw new RuntimeException("Paquete no encontrado");
        }
        
        Paquete paquete = paqueteOpt.get();
        
        // Validar que el estado es válido
        if (!esEstadoValido(request.getEstado(), paquete.getEstado())) {
            throw new RuntimeException("Estado inválido para la transición actual");
        }
        
        // Obtener información del empleado
        Optional<Usuario> empleadoOpt = usuarioRepository.findById(empleadoId);
        if (empleadoOpt.isEmpty()) {
            throw new RuntimeException("Empleado no encontrado");
        }
        
        Usuario empleado = empleadoOpt.get();
        
        // Crear movimiento
        Movimiento movimiento = new Movimiento();
        movimiento.setPaqueteId(request.getPaqueteId());
        movimiento.setEstado(request.getEstado());
        movimiento.setUbicacion(request.getUbicacion());
        movimiento.setEmpleadoId(empleadoId);
        movimiento.setEmpleadoNombre(empleado.getNombre() + " " + empleado.getApellidos());
        movimiento.setFechaHora(Timestamp.now());
        movimiento.setObservaciones(request.getObservaciones());
        
        // Guardar movimiento
        Movimiento movimientoGuardado = movimientoRepository.save(movimiento);
        
        // Actualizar estado del paquete
        paquete.setEstado(request.getEstado());
        paqueteRepository.save(paquete);
        
        return convertirAMovimientoResponse(movimientoGuardado);
    }
    
    private boolean esEstadoValido(EstadoPaquete nuevoEstado, EstadoPaquete estadoActual) {
        // Validar transiciones de estado válidas
        switch (estadoActual) {
            case RECOLECTADO:
                return nuevoEstado == EstadoPaquete.EN_TRANSITO || nuevoEstado == EstadoPaquete.CANCELADO;
            case EN_TRANSITO:
                return nuevoEstado == EstadoPaquete.ENTREGADO || nuevoEstado == EstadoPaquete.CANCELADO;
            case ENTREGADO:
            case CANCELADO:
                return false; // No se pueden cambiar estados finales
            default:
                return false;
        }
    }
    
    public List<MovimientoResponse> obtenerMovimientosPorPaquete(String paqueteId) {
        return movimientoRepository.findByPaqueteIdOrderByFechaHoraDesc(paqueteId)
                .stream()
                .map(this::convertirAMovimientoResponse)
                .collect(Collectors.toList());
    }
    
    public List<MovimientoResponse> obtenerMovimientosPorEmpleado(String empleadoId) {
        return movimientoRepository.findByEmpleadoId(empleadoId)
                .stream()
                .map(this::convertirAMovimientoResponse)
                .collect(Collectors.toList());
    }
    
    public List<MovimientoResponse> obtenerMovimientosPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return movimientoRepository.findByFechaHoraBetween(inicio, fin)
                .stream()
                .map(this::convertirAMovimientoResponse)
                .collect(Collectors.toList());
    }
    
    public List<MovimientoResponse> obtenerMovimientosPorEmpleadoYRangoFechas(
            String empleadoId, LocalDateTime inicio, LocalDateTime fin) {
        return movimientoRepository.findByEmpleadoIdAndFechaHoraBetween(empleadoId, inicio, fin)
                .stream()
                .map(this::convertirAMovimientoResponse)
                .collect(Collectors.toList());
    }
    
    private MovimientoResponse convertirAMovimientoResponse(Movimiento movimiento) {
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

