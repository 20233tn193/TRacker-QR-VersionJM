package com.tracker.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.tracker.model.Role;
import com.tracker.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.List;
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
            collection().document(usuario.getId()).set(usuario).get();
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
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al verificar email de usuario", e);
        }
    }
    
    public Optional<Usuario> findByPasswordResetToken(String token) {
        try {
            List<QueryDocumentSnapshot> documents = collection()
                    .whereEqualTo("passwordResetToken", token)
                    .limit(1)
                    .get()
                    .get()
                    .getDocuments();
            return documents.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable(fromDocument(documents.get(0)));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al obtener usuario por token de recuperación", e);
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
        Usuario usuario = snapshot.toObject(Usuario.class);
        if (usuario != null) {
            usuario.setId(snapshot.getId());
        }
        return usuario;
    }
}

