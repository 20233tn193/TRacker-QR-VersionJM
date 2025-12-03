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
import java.util.ArrayList;
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
        
        // Actualizar estado, empleadoId y ubicación del paquete
        paquete.setEstado(request.getEstado());
        
        // Actualizar la ruta cuando el paquete está en tránsito
        // Esto se ejecuta cuando el repartidor registra que el paquete llegó a un estado
        // El paquete llega al estado, se registra, y luego saldrá en ruta al siguiente estado
        if (request.getEstado() == EstadoPaquete.EN_TRANSITO && paquete.getEstadosRuta() != null && !paquete.getEstadosRuta().isEmpty()) {
            actualizarRutaPaquete(paquete, request.getUbicacion());
        }
        
        paqueteRepository.save(paquete);
        
        return convertirAMovimientoResponse(movimientoGuardado);
    }
    
    /**
     * Actualiza la ruta del paquete basándose en la ubicación del movimiento.
     * 
     * Lógica:
     * 1. El array estadosRuta contiene TODOS los estados que debe recorrer el paquete
     * 2. Cuando el paquete llega a un estado (se registra el movimiento), ese estado se elimina del array
     * 3. El paquete se entrega/registra en ese estado y luego sale en ruta al siguiente estado del array
     * 4. Esto continúa hasta que el array esté vacío (llegó al destino final)
     * 
     * Ejemplo:
     * - Ruta inicial: ["Ciudad de México", "Estado de México", "Puebla", "Veracruz", "Yucatán"]
     * - Llega a CDMX: estadoActualRuta = "Ciudad de México", elimina CDMX -> ["Estado de México", "Puebla", "Veracruz", "Yucatán"]
     * - Llega a Estado de México: estadoActualRuta = "Estado de México", elimina EdoMex -> ["Puebla", "Veracruz", "Yucatán"]
     * - Y así sucesivamente hasta que el array esté vacío
     */
    private void actualizarRutaPaquete(Paquete paquete, String ubicacion) {
        if (ubicacion == null || ubicacion.trim().isEmpty()) {
            return;
        }
        
        List<String> estadosRuta = paquete.getEstadosRuta();
        if (estadosRuta == null || estadosRuta.isEmpty()) {
            return;
        }
        
        // Extraer el estado de la ubicación
        String estadoUbicacion = extraerEstadoDeUbicacion(ubicacion);
        if (estadoUbicacion == null) {
            return;
        }
        
        // Normalizar el estado
        String estadoNormalizado = normalizarEstado(estadoUbicacion);
        
        // Verificar si el estado está en la ruta
        if (!estadosRuta.contains(estadoNormalizado)) {
            return;
        }
        
        // Encontrar el índice del estado en la ruta
        int indiceEstado = estadosRuta.indexOf(estadoNormalizado);
        
        if (indiceEstado >= 0) {
            // El paquete llegó a este estado
            // Actualizar el estado actual donde se encuentra el paquete
            paquete.setEstadoActualRuta(estadoNormalizado);
            
            // Eliminar este estado del array de estados por recorrer
            // El paquete ya pasó por este estado, ahora debe ir al siguiente
            List<String> estadosRestantes = new ArrayList<>();
            
            // Crear nueva lista sin el estado actual (y sin los anteriores si los hay)
            // Mantener solo los estados que faltan por recorrer
            for (int i = indiceEstado + 1; i < estadosRuta.size(); i++) {
                estadosRestantes.add(estadosRuta.get(i));
            }
            
            // Actualizar la lista de estados restantes
            if (!estadosRestantes.isEmpty()) {
                // Aún hay estados por recorrer
                paquete.setEstadosRuta(estadosRestantes);
                // El estadoActualRuta queda en el estado donde llegó
                // El siguiente movimiento debería registrar que sale en ruta al siguiente estado del array
            } else {
                // El array está vacío: el paquete llegó al estado destino final
                paquete.setEstadosRuta(new ArrayList<>());
                // El estadoActualRuta queda en el estado destino final
                // El paquete está en el estado destino, listo para entregarse al cliente
            }
        }
    }
    
    /**
     * Extrae el estado de una ubicación (puede ser una dirección completa)
     */
    private String extraerEstadoDeUbicacion(String ubicacion) {
        if (ubicacion == null || ubicacion.trim().isEmpty()) {
            return null;
        }
        
        // Lista de estados de México para buscar
        String[] estados = {
            "Aguascalientes", "Baja California", "Baja California Sur", "Campeche",
            "Chiapas", "Chihuahua", "Ciudad de México", "CDMX", "Coahuila", "Colima",
            "Durango", "Guanajuato", "Guerrero", "Hidalgo", "Jalisco", "México",
            "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca", "Puebla",
            "Querétaro", "Quintana Roo", "San Luis Potosí", "Sinaloa", "Sonora",
            "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas"
        };
        
        String ubicacionLower = ubicacion.toLowerCase();
        
        // Buscar el estado en la ubicación
        for (String estado : estados) {
            if (ubicacionLower.contains(estado.toLowerCase()) || 
                ubicacionLower.contains(estado.toLowerCase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u"))) {
                return estado;
            }
        }
        
        // Si no se encuentra, intentar extraer la última parte (puede ser el estado)
        String[] partes = ubicacion.split(",");
        if (partes.length > 0) {
            return partes[partes.length - 1].trim();
        }
        
        return null;
    }
    
    /**
     * Normaliza el nombre del estado a su forma estándar
     */
    private String normalizarEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return null;
        }
        
        estado = estado.trim();
        
        // Casos especiales
        if (estado.equalsIgnoreCase("CDMX") || 
            estado.equalsIgnoreCase("Distrito Federal") ||
            estado.toLowerCase().contains("ciudad de méxico")) {
            return "Ciudad de México";
        }
        if (estado.equalsIgnoreCase("México") || estado.equalsIgnoreCase("Mexico")) {
            return "México";
        }
        
        // Lista completa de estados para normalización
        String[] estadosCompletos = {
            "Aguascalientes", "Baja California", "Baja California Sur", "Campeche",
            "Chiapas", "Chihuahua", "Ciudad de México", "Coahuila", "Colima",
            "Durango", "Guanajuato", "Guerrero", "Hidalgo", "Jalisco", "México",
            "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca", "Puebla",
            "Querétaro", "Quintana Roo", "San Luis Potosí", "Sinaloa", "Sonora",
            "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas"
        };
        
        for (String estadoCompleto : estadosCompletos) {
            if (estadoCompleto.equalsIgnoreCase(estado) ||
                estadoCompleto.toLowerCase().contains(estado.toLowerCase()) ||
                estado.toLowerCase().contains(estadoCompleto.toLowerCase())) {
                return estadoCompleto;
            }
        }
        
        return estado; // Devolver tal cual si no se encuentra
    }
    
    private boolean esEstadoValido(EstadoPaquete nuevoEstado, EstadoPaquete estadoActual) {
        // Validar transiciones de estado válidas
        switch (estadoActual) {
            case RECOLECTADO:
                return nuevoEstado == EstadoPaquete.EN_TRANSITO || nuevoEstado == EstadoPaquete.CANCELADO;
            case EN_TRANSITO:
                // Permitir múltiples movimientos EN_TRANSITO para registrar paso por cada estado/ciudad
                return nuevoEstado == EstadoPaquete.EN_TRANSITO || 
                       nuevoEstado == EstadoPaquete.ENTREGADO || 
                       nuevoEstado == EstadoPaquete.CANCELADO;
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

