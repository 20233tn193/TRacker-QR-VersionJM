package com.tracker.exception;

import com.tracker.dto.ApiResponse;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Errores de validación", errors));
    }
    
    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ApiResponse> handleStatusRuntimeException(StatusRuntimeException ex) {
        String message = ex.getMessage();
        
        // Detectar si es un error de índice faltante
        if (message != null && message.contains("requires an index")) {
            // Extraer la URL del índice si está disponible
            String indexUrl = extractIndexUrl(message);
            if (indexUrl != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("La consulta requiere un índice en Firestore. " +
                                "Por favor, crea el índice necesario siguiendo este enlace: " + indexUrl));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("La consulta requiere un índice en Firestore. " +
                            "Revisa la consola de Firebase para crear el índice necesario."));
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error de base de datos: " + ex.getStatus().getCode()));
    }
    
    @ExceptionHandler(ExecutionException.class)
    public ResponseEntity<ApiResponse> handleExecutionException(ExecutionException ex) {
        // Verificar si la causa es una StatusRuntimeException
        Throwable cause = ex.getCause();
        if (cause instanceof StatusRuntimeException) {
            return handleStatusRuntimeException((StatusRuntimeException) cause);
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al ejecutar la operación: " + 
                        (cause != null ? cause.getMessage() : ex.getMessage())));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntimeException(RuntimeException ex) {
        // Verificar si la causa es una StatusRuntimeException o ExecutionException
        Throwable cause = ex.getCause();
        if (cause instanceof StatusRuntimeException) {
            return handleStatusRuntimeException((StatusRuntimeException) cause);
        }
        if (cause instanceof ExecutionException) {
            return handleExecutionException((ExecutionException) cause);
        }
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        // Verificar si la causa es una StatusRuntimeException
        Throwable cause = ex.getCause();
        if (cause instanceof StatusRuntimeException) {
            return handleStatusRuntimeException((StatusRuntimeException) cause);
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error interno del servidor: " + ex.getMessage()));
    }
    
    private String extractIndexUrl(String message) {
        // Buscar la URL del índice en el mensaje
        int urlStart = message.indexOf("https://console.firebase.google.com");
        if (urlStart != -1) {
            int urlEnd = message.indexOf("\n", urlStart);
            if (urlEnd == -1) {
                urlEnd = message.length();
            }
            return message.substring(urlStart, urlEnd).trim();
        }
        return null;
    }
}

