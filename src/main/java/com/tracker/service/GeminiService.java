package com.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
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
            logger.info("🤖 Calculando ruta optimizada con Gemini desde CDMX a {}", estadoDestino);
            String prompt = construirPrompt(estadoDestino);
            logger.debug("📝 Prompt enviado a Gemini: {}", prompt);
            
            String response = llamarGeminiAPI(prompt);
            logger.debug("📨 Respuesta de Gemini recibida (primeros 200 chars): {}", 
                response != null && response.length() > 200 ? response.substring(0, 200) : response);
            
            List<String> ruta = parsearRespuesta(response, estadoDestino);
            logger.info("✅ Ruta calculada exitosamente: {} estados", ruta.size());
            logger.debug("🗺️  Ruta: {}", ruta);
            
            return ruta;
        } catch (Exception e) {
            logger.error("❌ Error al llamar a Gemini API: {} - Usando ruta simple", e.getMessage());
            logger.debug("Stack trace completo:", e);
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
        
        logger.debug("🌐 URL de Gemini: {}", apiUrl);
        logger.debug("🔑 API Key configurada: {}...", apiKey != null && apiKey.length() > 10 ? apiKey.substring(0, 10) : "NO CONFIGURADA");
        
        String requestBody = String.format(
            "{\n" +
            "  \"contents\": [{\n" +
            "    \"parts\": [{\n" +
            "      \"text\": \"%s\"\n" +
            "    }]\n" +
            "  }]\n" +
            "}",
            prompt.replace("\"", "\\\"").replace("\n", "\\n")
        );
        
        try {
            String response = webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response == null || response.isEmpty()) {
                logger.warn("⚠️  Gemini devolvió respuesta vacía");
                throw new RuntimeException("Respuesta vacía de Gemini");
            }
            
            return response;
        } catch (Exception e) {
            logger.error("❌ Error en llamada HTTP a Gemini: {}", e.getMessage());
            throw new RuntimeException("Error al comunicarse con Gemini API", e);
        }
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
        // Ruta con estados intermedios realistas (fallback mejorado)
        logger.info("⚠️  Usando fallback - Rutas predefinidas para {}", estadoDestino);
        
        List<String> ruta = new ArrayList<>();
        ruta.add(ALMACEN_ESTADO);
        
        String estadoNormalizado = normalizarEstado(estadoDestino);
        
        if (estadoNormalizado == null || estadoNormalizado.equalsIgnoreCase(ALMACEN_ESTADO)) {
            return ruta;
        }
        
        // Rutas predefinidas realistas basadas en geografía de México
        // Estas son rutas lógicas siguiendo carreteras principales
        switch (estadoNormalizado) {
            // SURESTE
            case "Yucatán":
            case "Quintana Roo":
            case "Campeche":
                ruta.add("Puebla");
                ruta.add("Veracruz");
                ruta.add("Tabasco");
                ruta.add("Campeche");
                if (estadoNormalizado.equals("Yucatán")) {
                    ruta.add("Yucatán");
                } else if (estadoNormalizado.equals("Quintana Roo")) {
                    ruta.add("Yucatán");
                    ruta.add("Quintana Roo");
                }
                break;
                
            // SUR
            case "Chiapas":
                ruta.add("Puebla");
                ruta.add("Oaxaca");
                ruta.add("Chiapas");
                break;
                
            case "Oaxaca":
                ruta.add("Puebla");
                ruta.add("Oaxaca");
                break;
                
            case "Guerrero":
                ruta.add("Morelos");
                ruta.add("Guerrero");
                break;
                
            // GOLFO DE MÉXICO
            case "Veracruz":
                ruta.add("Puebla");
                ruta.add("Veracruz");
                break;
                
            case "Tabasco":
                ruta.add("Puebla");
                ruta.add("Veracruz");
                ruta.add("Tabasco");
                break;
                
            // OCCIDENTE
            case "Jalisco":
                ruta.add("Querétaro");
                ruta.add("Guanajuato");
                ruta.add("Jalisco");
                break;
                
            case "Michoacán":
                ruta.add("México");
                ruta.add("Michoacán");
                break;
                
            case "Colima":
                ruta.add("Querétaro");
                ruta.add("Guanajuato");
                ruta.add("Jalisco");
                ruta.add("Colima");
                break;
                
            case "Nayarit":
                ruta.add("Querétaro");
                ruta.add("Guanajuato");
                ruta.add("Jalisco");
                ruta.add("Nayarit");
                break;
                
            // NOROESTE
            case "Sinaloa":
                ruta.add("Querétaro");
                ruta.add("Guanajuato");
                ruta.add("Zacatecas");
                ruta.add("Durango");
                ruta.add("Sinaloa");
                break;
                
            case "Sonora":
                ruta.add("Querétaro");
                ruta.add("Guanajuato");
                ruta.add("Zacatecas");
                ruta.add("Durango");
                ruta.add("Sinaloa");
                ruta.add("Sonora");
                break;
                
            case "Baja California":
                ruta.add("Querétaro");
                ruta.add("Guanajuato");
                ruta.add("Zacatecas");
                ruta.add("Durango");
                ruta.add("Sinaloa");
                ruta.add("Sonora");
                ruta.add("Baja California");
                break;
                
            case "Baja California Sur":
                ruta.add("Querétaro");
                ruta.add("Guanajuato");
                ruta.add("Zacatecas");
                ruta.add("Durango");
                ruta.add("Sinaloa");
                ruta.add("Baja California Sur");
                break;
                
            // NORTE
            case "Chihuahua":
                ruta.add("Querétaro");
                ruta.add("Zacatecas");
                ruta.add("Durango");
                ruta.add("Chihuahua");
                break;
                
            case "Coahuila":
                ruta.add("Querétaro");
                ruta.add("San Luis Potosí");
                ruta.add("Coahuila");
                break;
                
            case "Nuevo León":
                ruta.add("Querétaro");
                ruta.add("San Luis Potosí");
                ruta.add("Nuevo León");
                break;
                
            case "Tamaulipas":
                ruta.add("Querétaro");
                ruta.add("San Luis Potosí");
                ruta.add("Tamaulipas");
                break;
                
            // CENTRO
            case "Puebla":
                ruta.add("Puebla");
                break;
                
            case "Tlaxcala":
                ruta.add("Puebla");
                ruta.add("Tlaxcala");
                break;
                
            case "Hidalgo":
                ruta.add("Hidalgo");
                break;
                
            case "Morelos":
                ruta.add("Morelos");
                break;
                
            case "México":
                ruta.add("México");
                break;
                
            case "Querétaro":
                ruta.add("Querétaro");
                break;
                
            case "Guanajuato":
                ruta.add("Querétaro");
                ruta.add("Guanajuato");
                break;
                
            case "San Luis Potosí":
                ruta.add("Querétaro");
                ruta.add("San Luis Potosí");
                break;
                
            case "Aguascalientes":
                ruta.add("Querétaro");
                ruta.add("Guanajuato");
                ruta.add("Aguascalientes");
                break;
                
            case "Zacatecas":
                ruta.add("Querétaro");
                ruta.add("San Luis Potosí");
                ruta.add("Zacatecas");
                break;
                
            case "Durango":
                ruta.add("Querétaro");
                ruta.add("Zacatecas");
                ruta.add("Durango");
                break;
                
            default:
                // Para estados no mapeados, ruta simple directa
                if (!estadoNormalizado.equalsIgnoreCase(ALMACEN_ESTADO)) {
                    ruta.add(estadoNormalizado);
                }
                break;
        }
        
        logger.info("📍 Ruta predefinida calculada: {}", ruta);
        return ruta;
    }
}

