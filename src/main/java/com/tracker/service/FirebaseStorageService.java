package com.tracker.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FirebaseStorageService {
    
    private static final String BUCKET_NAME = "tracker-qr.firebasestorage.app";
    
    /**
     * Sube un archivo al Firebase Storage
     * 
     * @param bytes Contenido del archivo en bytes
     * @param fileName Nombre del archivo (incluir extensión)
     * @param contentType Tipo de contenido (ej: "image/png")
     * @param folder Carpeta donde guardar (ej: "qr-codes")
     * @return URL pública del archivo subido
     */
    public String uploadFile(byte[] bytes, String fileName, String contentType, String folder) {
        try {
            Storage storage = StorageClient.getInstance().bucket().getStorage();
            
            // Construir la ruta completa: folder/fileName
            String filePath = folder + "/" + fileName;
            
            BlobId blobId = BlobId.of(BUCKET_NAME, filePath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();
            
            // Subir el archivo
            storage.create(blobInfo, bytes);
            
            // Construir URL pública
            // Formato: https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{path}?alt=media
            String encodedPath = filePath.replace("/", "%2F");
            return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                BUCKET_NAME,
                encodedPath
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo a Firebase Storage: " + e.getMessage(), e);
        }
    }
    
    /**
     * Elimina un archivo del Firebase Storage
     * 
     * @param filePath Ruta completa del archivo (ej: "qr-codes/PKG-ABC123.png")
     */
    public void deleteFile(String filePath) {
        try {
            Storage storage = StorageClient.getInstance().bucket().getStorage();
            BlobId blobId = BlobId.of(BUCKET_NAME, filePath);
            storage.delete(blobId);
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar archivo de Firebase Storage: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene un archivo del Firebase Storage
     * 
     * @param filePath Ruta completa del archivo
     * @return Contenido del archivo en bytes
     */
    public byte[] downloadFile(String filePath) {
        try {
            Storage storage = StorageClient.getInstance().bucket().getStorage();
            BlobId blobId = BlobId.of(BUCKET_NAME, filePath);
            Blob blob = storage.get(blobId);
            
            if (blob == null) {
                throw new RuntimeException("Archivo no encontrado en Firebase Storage");
            }
            
            return blob.getContent();
        } catch (Exception e) {
            throw new RuntimeException("Error al descargar archivo de Firebase Storage: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verifica si un archivo existe en Firebase Storage
     * 
     * @param filePath Ruta completa del archivo
     * @return true si existe, false si no
     */
    public boolean fileExists(String filePath) {
        try {
            Storage storage = StorageClient.getInstance().bucket().getStorage();
            BlobId blobId = BlobId.of(BUCKET_NAME, filePath);
            Blob blob = storage.get(blobId);
            return blob != null && blob.exists();
        } catch (Exception e) {
            return false;
        }
    }
}

