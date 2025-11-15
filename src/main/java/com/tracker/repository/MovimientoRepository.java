package com.tracker.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.tracker.model.EstadoPaquete;
import com.tracker.model.Movimiento;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class MovimientoRepository {

    private static final String COLLECTION_NAME = "movimientos";
    private final Firestore firestore;

    public MovimientoRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection() {
        return firestore.collection(COLLECTION_NAME);
    }

    public Movimiento save(Movimiento movimiento) {
        if (movimiento.getId() == null || movimiento.getId().isBlank()) {
            movimiento.setId(collection().document().getId());
        }
        if (movimiento.getFechaHora() == null) {
            movimiento.setFechaHora(Instant.now());
        }
        try {
            collection().document(movimiento.getId()).set(movimiento).get();
            return movimiento;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al guardar movimiento en Firestore", e);
        }
    }

    public List<Movimiento> findByPaqueteId(String paqueteId) {
        return findByField("paqueteId", paqueteId);
    }

    public List<Movimiento> findByPaqueteIdOrderByFechaHoraDesc(String paqueteId) {
        try {
            return collection()
                    .whereEqualTo("paqueteId", paqueteId)
                    .orderBy("fechaHora", Query.Direction.DESCENDING)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::fromDocument)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al obtener movimientos por paquete", e);
        }
    }

    public List<Movimiento> findByEmpleadoId(String empleadoId) {
        return findByField("empleadoId", empleadoId);
    }

    public List<Movimiento> findByEstado(EstadoPaquete estado) {
        return findByField("estado", estado);
    }

    public List<Movimiento> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin) {
        return findByDateRange(null, inicio, fin);
    }

    public List<Movimiento> findByEmpleadoIdAndFechaHoraBetween(String empleadoId, LocalDateTime inicio, LocalDateTime fin) {
        return findByDateRange(QueryFilter.of("empleadoId", empleadoId), inicio, fin);
    }

    public List<Movimiento> findByPaqueteIdAndFechaHoraBetween(String paqueteId, LocalDateTime inicio, LocalDateTime fin) {
        return findByDateRange(QueryFilter.of("paqueteId", paqueteId), inicio, fin);
    }

    private List<Movimiento> findByField(String field, Object value) {
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
            throw new RuntimeException("Error al consultar movimientos", e);
        }
    }

    private List<Movimiento> findByDateRange(QueryFilter filter, LocalDateTime inicio, LocalDateTime fin) {
        Instant inicioInstant = toInstant(inicio);
        Instant finInstant = toInstant(fin);
        try {
            Query query = collection();
            if (filter != null) {
                query = query.whereEqualTo(filter.field(), filter.value());
            }
            if (inicioInstant != null) {
                query = query.whereGreaterThanOrEqualTo("fechaHora", inicioInstant);
            }
            if (finInstant != null) {
                query = query.whereLessThanOrEqualTo("fechaHora", finInstant);
            }
            return query.get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::fromDocument)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al consultar movimientos por rango", e);
        }
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneOffset.UTC).toInstant();
    }

    private Movimiento fromDocument(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        Movimiento movimiento = snapshot.toObject(Movimiento.class);
        if (movimiento != null) {
            movimiento.setId(snapshot.getId());
        }
        return movimiento;
    }

    private record QueryFilter(String field, Object value) {
        static QueryFilter of(String field, Object value) {
            return new QueryFilter(field, value);
        }
    }
}

