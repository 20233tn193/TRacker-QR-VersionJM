package com.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class GeminiService {
    
    private static final String ALMACEN_ESTADO = "Ciudad de México";
    private static final List<String> ESTADOS_MEXICO = Arrays.asList(
        "Aguascalientes", "Baja California", "Baja California Sur", "Campeche",
        "Chiapas", "Chihuahua", "Ciudad de México", "Coahuila", "Colima",
        "Durango", "Guanajuato", "Guerrero", "Hidalgo", "Jalisco", "México",
        "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca", "Puebla",
        "Querétaro", "Quintana Roo", "San Luis Potosí", "Sinaloa", "Sonora",
        "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas"
    );
    
    @Value("${app.gemini.api-key}")
    private String apiKey;
    
    @Value("${app.gemini.api-url}")
    private String apiUrl;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public GeminiService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Calcula la ruta optimizada de estados desde el almacén en CDMX hasta el destino del cliente
     * @param estadoDestino Estado de destino del cliente
     * @return Lista de estados en orden de la ruta más corta
     */
    public List<String> calcularRutaOptimizada(String estadoDestino) {
        if (ALMACEN_ESTADO.equalsIgnoreCase(estadoDestino)) {
            // Si el destino es CDMX, la ruta solo tiene un estado
            return List.of(ALMACEN_ESTADO);
        }
        
        try {
            String prompt = construirPrompt(estadoDestino);
            String response = llamarGeminiAPI(prompt);
            return parsearRespuesta(response, estadoDestino);
        } catch (Exception e) {
            // En caso de error, devolver ruta simple directa
            return calcularRutaSimple(estadoDestino);
        }
    }
    
    private String construirPrompt(String estadoDestino) {
        return String.format(
            "Eres un experto en logística de México. " +
            "Necesito la ruta COMPLETA y más corta para enviar un paquete desde Ciudad de México (CDMX) hasta %s. " +
            "El paquete debe pasar por CENTROS DE DISTRIBUCIÓN en cada estado intermedio. " +
            "Debes incluir TODOS los estados por los que pasará el paquete, incluyendo estados intermedios. " +
            "Por ejemplo, de CDMX a Yucatán podría ser: Ciudad de México, Estado de México, Puebla, Veracruz, Tabasco, Yucatán. " +
            "La respuesta debe ser SOLO una lista completa de estados separados por comas, en orden de la ruta. " +
            "SIEMPRE debe empezar con 'Ciudad de México' y terminar con '%s'. " +
            "Incluye todos los estados intermedios necesarios para la ruta más eficiente. " +
            "Responde SOLO con la lista de estados separados por comas, sin números, sin guiones, sin explicaciones, sin puntos.",
            estadoDestino, estadoDestino
        );
    }
    
    private String llamarGeminiAPI(String prompt) {
        String url = apiUrl + "?key=" + apiKey;
        
        String requestBody = String.format(
            "{\n" +
            "  \"contents\": [{\n" +
            "    \"parts\": [{\n" +
            "      \"text\": \"%s\"\n" +
            "    }]\n" +
            "  }]\n" +
            "}",
            prompt.replace("\"", "\\\"")
        );
        
        return webClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
    
    private List<String> parsearRespuesta(String response, String estadoDestino) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            String text = jsonNode
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
            
            // Limpiar y parsear la respuesta
            text = text.trim();
            // Remover posibles números, guiones o viñetas al inicio
            text = text.replaceAll("^[\\d\\-\\.\\*]+\\s*", "");
            
            // Dividir por comas
            String[] estados = text.split("[,;]");
            List<String> ruta = new ArrayList<>();
            
            for (String estado : estados) {
                estado = estado.trim();
                // Normalizar el nombre del estado
                String estadoNormalizado = normalizarEstado(estado);
                if (estadoNormalizado != null && !ruta.contains(estadoNormalizado)) {
                    ruta.add(estadoNormalizado);
                }
            }
            
            // Asegurar que siempre empiece con CDMX
            if (!ruta.isEmpty() && !ruta.get(0).equalsIgnoreCase(ALMACEN_ESTADO)) {
                ruta.add(0, ALMACEN_ESTADO);
            }
            
            // Asegurar que termine con el estado destino
            String estadoDestinoNormalizado = normalizarEstado(estadoDestino);
            if (!ruta.isEmpty() && !ruta.get(ruta.size() - 1).equalsIgnoreCase(estadoDestinoNormalizado)) {
                if (!ruta.contains(estadoDestinoNormalizado)) {
                    ruta.add(estadoDestinoNormalizado);
                }
            }
            
            return ruta.isEmpty() ? calcularRutaSimple(estadoDestino) : ruta;
            
        } catch (Exception e) {
            return calcularRutaSimple(estadoDestino);
        }
    }
    
    private String normalizarEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return null;
        }
        
        estado = estado.trim();
        
        // Buscar coincidencia exacta o parcial en la lista de estados
        for (String estadoLista : ESTADOS_MEXICO) {
            if (estadoLista.equalsIgnoreCase(estado) || 
                estado.toLowerCase().contains(estadoLista.toLowerCase()) ||
                estadoLista.toLowerCase().contains(estado.toLowerCase())) {
                return estadoLista;
            }
        }
        
        // Casos especiales
        if (estado.toLowerCase().contains("cdmx") || 
            estado.toLowerCase().contains("ciudad de méxico") ||
            estado.toLowerCase().contains("distrito federal")) {
            return "Ciudad de México";
        }
        if (estado.toLowerCase().contains("méxico") && 
            !estado.toLowerCase().contains("ciudad")) {
            return "México";
        }
        
        return estado; // Devolver tal cual si no se encuentra coincidencia
    }
    
    private List<String> calcularRutaSimple(String estadoDestino) {
        // Ruta simple: CDMX -> Destino (fallback)
        List<String> ruta = new ArrayList<>();
        ruta.add(ALMACEN_ESTADO);
        
        String estadoNormalizado = normalizarEstado(estadoDestino);
        if (estadoNormalizado != null && !estadoNormalizado.equalsIgnoreCase(ALMACEN_ESTADO)) {
            ruta.add(estadoNormalizado);
        }
        
        return ruta;
    }
}

