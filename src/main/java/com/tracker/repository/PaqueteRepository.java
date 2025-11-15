package com.tracker.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.tracker.model.EstadoPaquete;
import com.tracker.model.Paquete;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class PaqueteRepository {

    private static final String COLLECTION_NAME = "paquetes";
    private final Firestore firestore;

    public PaqueteRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection() {
        return firestore.collection(COLLECTION_NAME);
    }

    public Paquete save(Paquete paquete) {
        Instant now = Instant.now();
        if (paquete.getId() == null || paquete.getId().isBlank()) {
            paquete.setId(collection().document().getId());
            paquete.setFechaCreacion(now);
        }
        paquete.setFechaUltimaActualizacion(now);
        try {
            collection().document(paquete.getId()).set(paquete).get();
            return paquete;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al guardar paquete en Firestore", e);
        }
    }

    public Optional<Paquete> findById(String id) {
        try {
            DocumentSnapshot snapshot = collection().document(id).get().get();
            return Optional.ofNullable(fromDocument(snapshot));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al obtener paquete por ID", e);
        }
    }

    public Optional<Paquete> findByCodigoQR(String codigoQR) {
        try {
            List<QueryDocumentSnapshot> documents = collection()
                    .whereEqualTo("codigoQR", codigoQR)
                    .limit(1)
                    .get()
                    .get()
                    .getDocuments();
            return documents.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable(fromDocument(documents.get(0)));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al obtener paquete por código QR", e);
        }
    }

    public List<Paquete> findByClienteEmail(String clienteEmail) {
        return findByField("clienteEmail", clienteEmail);
    }

    public List<Paquete> findByEstado(EstadoPaquete estado) {
        return findByField("estado", estado);
    }

    public List<Paquete> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin) {
        return findByDateRange("fechaCreacion", inicio, fin);
    }

    public List<Paquete> findByFechaUltimaActualizacionBetween(LocalDateTime inicio, LocalDateTime fin) {
        return findByDateRange("fechaUltimaActualizacion", inicio, fin);
    }

    private List<Paquete> findByField(String field, Object value) {
        try {
            return collection()
                    .whereEqualTo(field, value)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::fromDocument)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al consultar paquetes", e);
        }
    }

    private List<Paquete> findByDateRange(String field, LocalDateTime inicio, LocalDateTime fin) {
        Instant inicioInstant = toInstant(inicio);
        Instant finInstant = toInstant(fin);
        try {
            Query query = collection();
            if (inicioInstant != null) {
                query = query.whereGreaterThanOrEqualTo(field, inicioInstant);
            }
            if (finInstant != null) {
                query = query.whereLessThanOrEqualTo(field, finInstant);
            }
            return query.get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::fromDocument)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al consultar paquetes por rango de fechas", e);
        }
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneOffset.UTC).toInstant();
    }

    private Paquete fromDocument(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        Paquete paquete = snapshot.toObject(Paquete.class);
        if (paquete != null) {
            paquete.setId(snapshot.getId());
        }
        return paquete;
    }
}

