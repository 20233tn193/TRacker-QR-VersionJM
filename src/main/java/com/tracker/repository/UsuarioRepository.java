package com.tracker.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.Timestamp;
import com.tracker.model.Role;
import com.tracker.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class UsuarioRepository {

    private static final String COLLECTION_NAME = "usuarios";
    private final Firestore firestore;

    public UsuarioRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection() {
        return firestore.collection(COLLECTION_NAME);
    }

    public Usuario save(Usuario usuario) {
        Timestamp now = Timestamp.now();
        if (usuario.getId() == null || usuario.getId().isBlank()) {
            usuario.setId(collection().document().getId());
            usuario.setFechaCreacion(now);
        }
        usuario.setFechaActualizacion(now);

        try {
            // Convertir Usuario a Map para Firestore
            Map<String, Object> data = new HashMap<>();
            data.put("email", usuario.getEmail());
            data.put("password", usuario.getPassword());
            data.put("nombre", usuario.getNombre());
            data.put("apellidoPaterno", usuario.getApellidoPaterno());
            data.put("apellidoMaterno", usuario.getApellidoMaterno());
            data.put("ubicacion", usuario.getUbicacion());
            data.put("rol", usuario.getRol() != null ? usuario.getRol().name() : null);
            data.put("activo", usuario.isActivo());
            data.put("intentosFallidos", usuario.getIntentosFallidos());
            data.put("habilitado2FA", usuario.isHabilitado2FA());
            data.put("secret2FA", usuario.getSecret2FA());

            // Convertir Instant a Timestamp para Firestore
            if (usuario.getFechaCreacion() != null) {
                data.put("fechaCreacion", Timestamp.ofTimeSecondsAndNanos(
                    usuario.getFechaCreacion().getEpochSecond(),
                    usuario.getFechaCreacion().getNano()
                ));
            }
            if (usuario.getFechaActualizacion() != null) {
                data.put("fechaActualizacion", Timestamp.ofTimeSecondsAndNanos(
                    usuario.getFechaActualizacion().getEpochSecond(),
                    usuario.getFechaActualizacion().getNano()
                ));
            }
            if (usuario.getBloqueadoHasta() != null) {
                data.put("bloqueadoHasta", Timestamp.ofTimeSecondsAndNanos(
                    usuario.getBloqueadoHasta().getEpochSecond(),
                    usuario.getBloqueadoHasta().getNano()
                ));
            }

            collection().document(usuario.getId()).set(data).get();
            return usuario;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al guardar usuario en Firestore", e);
        }
    }

    public Optional<Usuario> findById(String id) {
        try {
            DocumentSnapshot snapshot = collection().document(id).get().get();
            return Optional.ofNullable(fromDocument(snapshot));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al obtener usuario por ID", e);
        }
    }

    public Optional<Usuario> findByEmail(String email) {
        try {
            List<QueryDocumentSnapshot> documents = collection()
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get()
                    .getDocuments();
            return documents.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable(fromDocument(documents.get(0)));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al obtener usuario por email", e);
        }
    }

    public boolean existsByEmail(String email) {
        try {
            return !collection()
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get()
                    .isEmpty();
        } catch (ExecutionException e) {
            throw new RuntimeException("Error al verificar email de usuario: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operación interrumpida al verificar email de usuario", e);
        }
    }

    public List<Usuario> findByRol(Role rol) {
        try {
            return collection()
                    .whereEqualTo("rol", rol)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::fromDocument)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al obtener usuarios por rol", e);
        }
    }

    public List<Usuario> findByActivo(boolean activo) {
        try {
            return collection()
                    .whereEqualTo("activo", activo)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::fromDocument)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al obtener usuarios por estado", e);
        }
    }

    public List<Usuario> findAll() {
        try {
            return collection()
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::fromDocument)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al obtener todos los usuarios", e);
        }
    }

    private Usuario fromDocument(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }

        Usuario usuario = new Usuario();
        usuario.setId(snapshot.getId());
        usuario.setEmail(snapshot.getString("email"));
        usuario.setPassword(snapshot.getString("password"));
        usuario.setNombre(snapshot.getString("nombre"));
        usuario.setApellidoPaterno(snapshot.getString("apellidoPaterno"));
        usuario.setApellidoMaterno(snapshot.getString("apellidoMaterno"));
        usuario.setUbicacion(snapshot.getString("ubicacion"));

        // Convertir rol
        String rolStr = snapshot.getString("rol");
        if (rolStr != null) {
            usuario.setRol(Role.valueOf(rolStr));
        }

        // Campos booleanos y numéricos
        Boolean activo = snapshot.getBoolean("activo");
        usuario.setActivo(activo != null ? activo : true);

        Long intentosFallidos = snapshot.getLong("intentosFallidos");
        usuario.setIntentosFallidos(intentosFallidos != null ? intentosFallidos.intValue() : 0);

        Boolean habilitado2FA = snapshot.getBoolean("habilitado2FA");
        usuario.setHabilitado2FA(habilitado2FA != null ? habilitado2FA : false);

        usuario.setSecret2FA(snapshot.getString("secret2FA"));

        // Convertir fechas de Timestamp a Instant
        com.google.cloud.Timestamp fechaCreacion = snapshot.getTimestamp("fechaCreacion");
        if (fechaCreacion != null) {
            usuario.setFechaCreacion(Instant.ofEpochSecond(fechaCreacion.getSeconds(), fechaCreacion.getNanos()));
        }

        com.google.cloud.Timestamp fechaActualizacion = snapshot.getTimestamp("fechaActualizacion");
        if (fechaActualizacion != null) {
            usuario.setFechaActualizacion(Instant.ofEpochSecond(fechaActualizacion.getSeconds(), fechaActualizacion.getNanos()));
        }

        com.google.cloud.Timestamp bloqueadoHasta = snapshot.getTimestamp("bloqueadoHasta");
        if (bloqueadoHasta != null) {
            usuario.setBloqueadoHasta(Instant.ofEpochSecond(bloqueadoHasta.getSeconds(), bloqueadoHasta.getNanos()));
        }

        return usuario;
    }
}

