package com.tracker;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class TrackerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(TrackerApplication.class);
    
    public static void main(String[] args) {
        loadDotenv();
        
        SpringApplication.run(TrackerApplication.class, args);
    }
    
    private static void loadDotenv() {
        try {
            File envFile = new File(".env");
            
            if (envFile.exists()) {
                Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
                
                dotenv.entries().forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                        logger.debug("Variable cargada desde .env: {}", key);
                    }
                });
                
                logger.info("Archivo .env cargado exitosamente. {} variables cargadas.", dotenv.entries().size());
            } else {
                logger.warn("Archivo .env no encontrado en la raíz del proyecto. Se usarán variables de entorno del sistema.");
            }
        } catch (Exception e) {
            logger.error("Error al cargar el archivo .env: {}", e.getMessage(), e);
        }
    }
}

