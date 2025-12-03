package com.tracker.service;

import com.tracker.dto.ConfirmacionRecepcionRequest;
import com.tracker.dto.PaqueteRequest;
import com.tracker.dto.PaqueteResponse;
import com.tracker.dto.MovimientoResponse;
import com.tracker.dto.SatisfaccionResponse;
import com.tracker.model.EstadoPaquete;
import com.tracker.model.Movimiento;
import com.tracker.model.Paquete;
import com.tracker.model.Role;
import com.tracker.model.Usuario;
import com.google.cloud.Timestamp;
import com.tracker.repository.PaqueteRepository;
import com.tracker.repository.MovimientoRepository;
import com.tracker.repository.UsuarioRepository;
import com.tracker.util.QRCodeGenerator;
import com.tracker.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    
    @Autowired
    private JwtUtil jwtUtil;
    
    public Paquete crearPaquete(PaqueteRequest request, HttpServletRequest httpRequest) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getClienteEmail());
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Cliente no encontrado con el email proporcionado");
        }
        
        Usuario cliente = usuarioOpt.get();
        if (cliente.getUbicacion() == null || cliente.getUbicacion().isEmpty()) {
            throw new RuntimeException("El cliente no tiene una ubicación registrada");
        }
        
        String token = extractToken(httpRequest);
        String empleadoId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
        
        if (!role.equals(Role.EMPLEADO.name())) {
            throw new RuntimeException("Solo los empleados pueden crear paquetes");
        }
        
        Optional<Usuario> empleadoOpt = usuarioRepository.findById(empleadoId);
        if (empleadoOpt.isEmpty()) {
            throw new RuntimeException("Empleado no encontrado");
        }
        
        Usuario empleado = empleadoOpt.get();
        if (empleado.getRol() != Role.EMPLEADO) {
            throw new RuntimeException("El usuario autenticado no es un empleado");
        }
        
        Paquete paquete = new Paquete();
        paquete.setDescripcion(request.getDescripcion());
        paquete.setClienteEmail(request.getClienteEmail());
        paquete.setDireccionOrigen(request.getDireccionOrigen()); 
        paquete.setDireccionDestino(cliente.getUbicacion());
        paquete.setEstado(EstadoPaquete.RECOLECTADO);
        paquete.setEmpleadoId(empleadoId);
        paquete.setUbicacion(request.getUbicacion());
        
        // Generar código QR único
        String codigoQR = generarCodigoQRUnico();
        paquete.setCodigoQR(codigoQR);
        
        Paquete paqueteGuardado = paqueteRepository.save(paquete);
        
        Movimiento movimiento = new Movimiento();
        movimiento.setPaqueteId(paqueteGuardado.getId());
        movimiento.setEstado(EstadoPaquete.RECOLECTADO);
        movimiento.setUbicacion(request.getUbicacion());
        movimiento.setEmpleadoId(empleadoId);
        movimiento.setEmpleadoNombre(empleado.getNombre() + " " + empleado.getApellidos());
        movimiento.setFechaHora(Timestamp.now());
        movimiento.setObservaciones("Paquete recolectado por el repartidor " + empleado.getNombre() + " " + empleado.getApellidos());
        
        movimientoRepository.save(movimiento);
        
        return paqueteGuardado;
    }
    
    private String generarCodigoQRUnico() {
        String codigo;
        do {
            codigo = "PKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (paqueteRepository.findByCodigoQR(codigo).isPresent());
        
        return codigo;
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("Token no encontrado. Se requiere autenticación");
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
    
    public List<PaqueteResponse> obtenerPaquetesPorEmpleado(String empleadoId, EstadoPaquete estado, String mes) {
        // Si se proporciona empleadoId, validar que existe y es empleado
        if (empleadoId != null && !empleadoId.isBlank()) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(empleadoId);
            if (usuarioOpt.isEmpty()) {
                throw new RuntimeException("Empleado no encontrado");
            }
            
            Usuario empleado = usuarioOpt.get();
            if (empleado.getRol() != Role.EMPLEADO) {
                throw new RuntimeException("El usuario especificado no es un empleado");
            }
        }
        
        // Convertir mes a rango de fechas
        LocalDateTime inicioMes = null;
        LocalDateTime finMes = null;
        if (mes != null && !mes.isBlank()) {
            try {
                // Formato esperado: YYYY-MM (ej: "2024-01")
                YearMonth yearMonth = YearMonth.parse(mes, DateTimeFormatter.ofPattern("yyyy-MM"));
                inicioMes = yearMonth.atDay(1).atStartOfDay();
                finMes = yearMonth.atEndOfMonth().atTime(23, 59, 59);
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Formato de mes inválido. Use el formato YYYY-MM (ej: 2024-01)");
            }
        }
        
        return paqueteRepository.findByEmpleadoIdAndEstado(empleadoId, estado, inicioMes, finMes)
                .stream()
                .map(this::convertirAPaqueteResponse)
                .collect(Collectors.toList());
    }

    public List<PaqueteResponse> obtenerPaquetesFiltrados(Role rol, String usuarioId, EstadoPaquete estado, LocalDateTime fechaInicio, LocalDateTime fechaFin, String mes) {
        String empleadoId = null;
        String clienteEmail = null;
        
        if (usuarioId != null && !usuarioId.isBlank()) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            if (usuarioOpt.isEmpty()) {
                throw new RuntimeException("Usuario no encontrado");
            }
            
            Usuario usuario = usuarioOpt.get();
            
            if (rol != null && usuario.getRol() != rol) {
                throw new RuntimeException("El usuario especificado no tiene el rol " + rol.name());
            }
            
            if (usuario.getRol() == Role.EMPLEADO) {
                empleadoId = usuarioId;
            } else if (usuario.getRol() == Role.CLIENTE) {
                clienteEmail = usuario.getEmail();
            }
        } else if (rol != null) {
            if (rol != Role.ADMINISTRADOR && rol != Role.EMPLEADO && rol != Role.CLIENTE) {
                throw new RuntimeException("Rol inválido");
            }
        }
        
        if ((fechaInicio != null || fechaFin != null) && mes != null && !mes.isBlank()) {
            throw new RuntimeException("No se pueden usar fechaInicio/fechaFin y mes al mismo tiempo. Use uno u otro.");
        }
        
        LocalDateTime fechaInicioFinal = fechaInicio;
        LocalDateTime fechaFinFinal = fechaFin;
        
        if (mes != null && !mes.isBlank()) {
            try {
                YearMonth yearMonth = YearMonth.parse(mes, DateTimeFormatter.ofPattern("yyyy-MM"));
                fechaInicioFinal = yearMonth.atDay(1).atStartOfDay();
                fechaFinFinal = yearMonth.atEndOfMonth().atTime(23, 59, 59);
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Formato de mes inválido. Use el formato YYYY-MM (ej: 2024-01)");
            }
        }
        
        return paqueteRepository.findWithFilters(empleadoId, clienteEmail, estado, fechaInicioFinal, fechaFinFinal)
                .stream()
                .map(this::convertirAPaqueteResponse)
                .collect(Collectors.toList());
    }

    public List<PaqueteResponse> obtener10PaquetesRecientes() {
        return paqueteRepository.findTop10ByFechaUltimaActualizacionDesc()
                .stream()
                .map(this::convertirAPaqueteResponse)
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
        response.setUbicacion(paquete.getUbicacion());
        response.setEmpleadoId(paquete.getEmpleadoId());
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

