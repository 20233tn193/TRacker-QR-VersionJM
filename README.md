# Sistema de Rastreo Logístico de Paquetería - Backend API

API REST desarrollada con Spring Boot para el sistema de rastreo logístico de paquetería mediante códigos QR únicos.

## 📋 Características

- ✅ Autenticación JWT con soporte para 2FA (TOTP)
- ✅ Gestión de usuarios con roles (Cliente, Empleado, Supervisor, Administrador)
- ✅ Generación y consulta de paquetes con códigos QR únicos
- ✅ Registro de movimientos logísticos (recolección, tránsito, entrega)
- ✅ Confirmación de recepción por clientes
- ✅ Generación de reportes PDF de trazabilidad
- ✅ Estadísticas de entregas
- ✅ Control de acceso basado en roles (RBAC)
- ✅ Persistencia en Firebase Firestore

## 🛠️ Tecnologías

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** - Autenticación y autorización
- **Firebase Admin (Firestore)** - Persistencia de datos
- **JWT (jjwt)** - Tokens de autenticación
- **TOTP** - Autenticación de dos factores
- **ZXing** - Generación de códigos QR
- **iTextPDF** - Generación de reportes PDF
- **Maven** - Gestión de dependencias

## 📦 Requisitos Previos

- Java 17 o superior
- Maven 3.6+
- Proyecto Firebase (Firestore) con credenciales de servicio (JSON)

## 🚀 Instalación y Configuración

### 0. Requisitos rápidos (TL;DR)
1. `git clone <url>` y `cd back-Tracker`
2. Descarga la **cuenta de servicio** de Firebase y colócala en `src/main/resources/firebase-service-account.json`
3. Edita `src/main/resources/application.yml` y establece tu `JWT_SECRET`
4. Instala dependencias y ejecuta `mvn spring-boot:run`
5. Abre `http://localhost:8080/api/swagger-ui.html` y prueba los endpoints

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd back-Tracker
```

### 2. Configurar Firebase (Firestore)

1. Ve a [Firebase Console](https://console.firebase.google.com/) y selecciona tu proyecto (`qr-traker`).
2. En **Configuración del proyecto → Cuentas de servicio**, haz clic en **Generar nueva clave privada** para obtener el archivo JSON de la cuenta de servicio.
3. Guarda el archivo como `src/main/resources/firebase-service-account.json` (o cambia la ruta en `FirebaseConfig` si prefieres otra ubicación).
4. Asegúrate de que el archivo esté excluido del control de versiones (ya está en `.gitignore`). En producción puedes usar variables de entorno o un gestor de secretos para cargarlo.
5. Firestore creará automáticamente las colecciones (`usuarios`, `paquetes`, `movimientos`) la primera vez que la API las utilice.

### 3. Configurar variables de entorno

Edita el archivo `src/main/resources/application.yml` o crea un archivo `.env` con las siguientes variables:

```yaml
spring:
  security:
    jwt:
      secret: tu-clave-secreta-super-segura-aqui
      expiration: 86400000  # 24 horas
```

**Importante:** Cambia `JWT_SECRET` por una clave secreta segura en producción.

### 4. Compilar el proyecto

```bash
mvn clean install
```

### 5. Ejecutar la aplicación

```bash
mvn spring-boot:run
```

O ejecuta directamente el JAR:

```bash
java -jar target/back-tracker-1.0.0.jar
```

La API estará disponible en: `http://localhost:8080/api`

**⚠️ Importante:** Asegúrate de que el archivo `firebase-service-account.json` esté presente antes de iniciar la aplicación (o configura la ruta correspondiente en `FirebaseConfig`).

### 6. Acceder a la documentación Swagger

Una vez que la aplicación esté corriendo, puedes acceder a la documentación interactiva de la API en:

- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/api-docs`

Swagger UI te permite:
- ✅ Ver todos los endpoints disponibles
- ✅ Probar los endpoints directamente desde el navegador
- ✅ Ver los modelos de datos (DTOs)
- ✅ Autenticarte con JWT y probar endpoints protegidos
- ✅ Ver ejemplos de requests y responses

### 7. Configuración de Firebase (opcional)

La API puede inicializar Firebase Admin para usar Firestore/Auth/Storage. Para ello:

1. Coloca tu archivo de cuenta de servicio en `src/main/resources/firebase-service-account.json`.  
   > **Importante:** no lo subas a repositorios públicos. Usa variables de entorno o un gestor de secretos en producción.
2. Revisa `com.tracker.config.FirebaseConfig` para ver cómo se inicializa `FirebaseApp` y el bean `Firestore`.
3. Si no usarás Firebase, elimina el archivo o ajusta `FirebaseConfig` para que lea desde otra ubicación.

## 📚 Endpoints de la API

### Autenticación

#### `POST /api/auth/login`
Iniciar sesión

**Request:**
```json
{
  "email": "usuario@example.com",
  "password": "password123",
  "codigo2FA": "123456"  // Opcional, requerido si 2FA está habilitado
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login exitoso",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tipoToken": "Bearer",
    "id": "user_id",
    "email": "usuario@example.com",
    "nombre": "Juan",
    "rol": "EMPLEADO",
    "requiere2FA": false
  }
}
```

#### `POST /api/auth/2fa/generar/{userId}`
Generar secreto 2FA para un usuario

#### `GET /api/auth/2fa/qrcode/{userId}`
Obtener código QR para configurar 2FA

#### `POST /api/auth/2fa/habilitar/{userId}?codigo={codigo}`
Habilitar 2FA con código de verificación

---

### Paquetes

#### `POST /api/paquetes`
Crear un nuevo paquete

**Headers:** `Authorization: Bearer {token}` (requerido para EMPLEADO, SUPERVISOR, ADMINISTRADOR)

**Request:**
```json
{
  "descripcion": "Paquete de ejemplo",
  "clienteEmail": "cliente@example.com",
  "direccionOrigen": "Calle Origen 123",
  "direccionDestino": "Calle Destino 456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Paquete creado exitosamente",
  "data": {
    "id": "paquete_id",
    "codigoQR": "PKG-ABC12345",
    "descripcion": "Paquete de ejemplo",
    "estado": "RECOLECTADO",
    "clienteEmail": "cliente@example.com",
    "direccionOrigen": "Calle Origen 123",
    "direccionDestino": "Calle Destino 456",
    "fechaCreacion": "2024-01-15T10:30:00",
    "fechaUltimaActualizacion": "2024-01-15T10:30:00",
    "confirmadoRecepcion": false
  }
}
```

#### `GET /api/paquetes/qr/{codigoQR}`
Consultar paquete por código QR (público, no requiere autenticación)

**Response:** Incluye el historial completo de movimientos

#### `GET /api/paquetes/{id}`
Consultar paquete por ID

**Headers:** `Authorization: Bearer {token}`

#### `GET /api/paquetes/cliente/{clienteEmail}`
Obtener todos los paquetes de un cliente

**Headers:** `Authorization: Bearer {token}`

#### `POST /api/paquetes/qr/{codigoQR}/confirmar-recepcion`
Confirmar recepción del paquete (público, no requiere autenticación)

**Request:**
```json
{
  "firmaDigital": "firma_base64_o_texto"
}
```

#### `GET /api/paquetes/qr/{codigoQR}/imagen`
Obtener imagen del código QR (público)

---

### Movimientos

#### `POST /api/movimientos`
Registrar un nuevo movimiento

**Headers:** `Authorization: Bearer {token}` (requerido para EMPLEADO, SUPERVISOR, ADMINISTRADOR)

**Request:**
```json
{
  "paqueteId": "paquete_id",
  "estado": "EN_TRANSITO",
  "ubicacion": "Ciudad, Estado",
  "observaciones": "En camino al destino"
}
```

**Estados válidos:** `RECOLECTADO`, `EN_TRANSITO`, `ENTREGADO`, `CANCELADO`

#### `GET /api/movimientos/paquete/{paqueteId}`
Obtener movimientos de un paquete

#### `GET /api/movimientos/empleado/{empleadoId}`
Obtener movimientos de un empleado

#### `GET /api/movimientos/rango-fechas?inicio={fecha}&fin={fecha}`
Obtener movimientos por rango de fechas

**Formato de fecha:** ISO 8601 (ej: `2024-01-15T10:30:00`)

---

### Usuarios

#### `POST /api/usuarios`
Crear un nuevo usuario

**Headers:** `Authorization: Bearer {token}` (requerido para ADMINISTRADOR, SUPERVISOR)

**Request:**
```json
{
  "email": "nuevo@example.com",
  "password": "password123",
  "nombre": "Juan",
  "apellidos": "Pérez",
  "rol": "EMPLEADO"
}
```

**Roles válidos:** `CLIENTE`, `EMPLEADO`, `SUPERVISOR`, `ADMINISTRADOR`

#### `PUT /api/usuarios/{id}`
Actualizar usuario

#### `GET /api/usuarios`
Obtener todos los usuarios

#### `GET /api/usuarios/{id}`
Obtener usuario por ID

#### `GET /api/usuarios/rol/{rol}`
Obtener usuarios por rol

#### `PUT /api/usuarios/{id}/activar`
Activar usuario

#### `PUT /api/usuarios/{id}/desactivar`
Desactivar usuario

---

### Reportes

#### `GET /api/reportes/trazabilidad?inicio={fecha}&fin={fecha}&empleadoId={id}`
Generar reporte PDF de trazabilidad

**Headers:** `Authorization: Bearer {token}` (requerido para SUPERVISOR, ADMINISTRADOR)

**Response:** Archivo PDF descargable

#### `GET /api/reportes/estadisticas-entregas?inicio={fecha}&fin={fecha}`
Obtener estadísticas de entregas

**Headers:** `Authorization: Bearer {token}` (requerido para SUPERVISOR, ADMINISTRADOR)

---

## 🔐 Seguridad

### Autenticación JWT

Todos los endpoints protegidos requieren un token JWT en el header:

```
Authorization: Bearer {token}
```

### Roles y Permisos

- **CLIENTE**: Solo puede consultar sus propios paquetes
- **EMPLEADO**: Puede registrar movimientos y consultar paquetes
- **SUPERVISOR**: Puede generar reportes, gestionar usuarios y ver todos los movimientos
- **ADMINISTRADOR**: Acceso completo al sistema

### Autenticación de Dos Factores (2FA)

El sistema soporta 2FA mediante TOTP (Time-based One-Time Password). Para habilitarlo:

1. Generar secreto: `POST /api/auth/2fa/generar/{userId}`
2. Obtener QR: `GET /api/auth/2fa/qrcode/{userId}`
3. Escanear QR con app autenticadora (Google Authenticator, Authy, etc.)
4. Habilitar 2FA: `POST /api/auth/2fa/habilitar/{userId}?codigo={codigo}`

## 📊 Estructura del Proyecto

```
src/main/java/com/tracker/
├── controller/          # Controladores REST
│   ├── AuthController.java
│   ├── PaqueteController.java
│   ├── MovimientoController.java
│   ├── UsuarioController.java
│   └── ReporteController.java
├── service/            # Lógica de negocio
│   ├── AuthService.java
│   ├── PaqueteService.java
│   ├── MovimientoService.java
│   ├── UsuarioService.java
│   └── ReporteService.java
├── repository/         # Repositorios Firestore
│   ├── UsuarioRepository.java
│   ├── PaqueteRepository.java
│   └── MovimientoRepository.java
├── model/              # Entidades
│   ├── Usuario.java
│   ├── Paquete.java
│   ├── Movimiento.java
│   ├── Role.java
│   └── EstadoPaquete.java
├── dto/                # Data Transfer Objects
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── PaqueteRequest.java
│   ├── PaqueteResponse.java
│   └── ...
├── security/           # Configuración de seguridad
│   ├── SecurityConfig.java
│   └── JwtAuthenticationFilter.java
├── util/               # Utilidades
│   ├── JwtUtil.java
│   ├── QRCodeGenerator.java
│   └── TOTPUtil.java
├── exception/          # Manejo de excepciones
│   └── GlobalExceptionHandler.java
└── TrackerApplication.java
```

## 🧪 Pruebas

Para ejecutar las pruebas:

```bash
mvn test
```

## 📝 Notas Importantes

1. **Firebase**: Asegúrate de que la cuenta de servicio esté disponible antes de iniciar la aplicación
2. **JWT Secret**: Cambia la clave secreta en producción
3. **CORS**: La configuración actual permite todos los orígenes. Ajusta según tus necesidades
4. **2FA**: Los usuarios con 2FA habilitado deben proporcionar el código en cada login
5. **Bloqueo de cuenta**: Después de 3 intentos fallidos, la cuenta se bloquea por 24 horas

## 🔧 Consejos para Firebase / Firestore

- **No subas el archivo de servicio** al repositorio. Usa variables de entorno o un gestor de secretos en producción.
- Para despliegues en servidores, puedes apuntar `FirebaseConfig` a una ruta externa (`GOOGLE_APPLICATION_CREDENTIALS`) o inyectar el JSON como variable.
- **Estructura de datos recomendada**  
  - Colección `usuarios`: documentos con el `id` del usuario.  
  - Colección `paquetes`: cada documento representa un paquete con sus metadatos.  
  - Colección `movimientos`: registra cada evento/logística asociado a un paquete (campo `paqueteId`).
- **Migraciones de datos**: si vienes de MongoDB, exporta los documentos y súbelos a Firestore mediante scripts o la consola.
- **Índices**: Firestore crea índices simples automáticamente. Para consultas avanzadas (por ejemplo, rango + filtro), revisa la consola si te solicita crear índices compuestos.
- **Límites**: recuerda que Firestore tiene cuotas de lectura/escritura, evalúa el plan (Spark/Blaze) según tu volumen.

## 🤝 Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT.

## 👤 Autor

Desarrollado para el curso de Arquitectura de Software - UTEZ

---

**Versión:** 1.0.0  
**Última actualización:** 2024

# TRacker-QR-VersionJM
