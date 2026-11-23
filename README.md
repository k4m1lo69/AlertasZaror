# Alertas Zaror

App Android para la gestión de alarmas comunitarias de la Junta de Vecinos Población Zaror (Cerrillos), con un microservicio Spring Boot y base de datos Oracle. Evaluación Final Transversal, DSY1105 Desarrollo de Aplicaciones Móviles, DUOC UC.

## Integrantes
- Camilo Andrés Contreras Silva

## Arquitectura
- **App Android** (Kotlin, Jetpack Compose) con MVVM: `data` (Room, repositorios, Retrofit), `domain` (validaciones, roles y permisos), `viewmodel` y `ui`.
- **Backend** (`backend/`, Spring Boot 3 y Java 21) con Spring Security y JWT, y persistencia en Oracle.
- **Persistencia local** con Room (copia de las alertas y contactos) y DataStore (sesión y token).
- **API externa**: Open-Meteo para mostrar el clima en la pantalla de nueva alerta.

## Roles y permisos
| Rol | Ver alertas | Marcar atendida | Administrar usuarios | Eliminar alertas |
|---|---|---|---|---|
| Vecino | Solo las propias | No | No | No |
| Coordinador de cuadra | Todas | Sí | No | No |
| Directiva | Todas (solo lectura) | No | No | No |
| Administrador | Todas | Sí | Sí | Sí |

El rol viaja en el token JWT. Un cambio de rol se aplica la próxima vez que la persona inicia sesión.

## Funcionalidades
- Registro e inicio de sesión. Las cuentas nuevas quedan pendientes hasta que un administrador las apruebe y les asigne cuadra.
- Alertas comunitarias con formulario validado, foto opcional, ubicación y clima actual.
- Reporte de fallas del sistema de alarmas.
- Historial con cambio de estado (Activa, Atendida, Cancelada) y eliminación con confirmación.
- Contactos de confianza por usuario.
- **Mi cuenta**: editar nombre y dirección, y cambiar la contraseña.
- **Administrar vecinos**: aprobar o rechazar solicitudes, asignar cuadra, cambiar el rol y restablecer contraseñas.
- **Recuperación de contraseña** asistida: la persona pide a un administrador que la restablezca. El administrador recibe una clave temporal, se la entrega en persona y la persona la cambia desde «Mi cuenta». No hay envío por correo ni SMS.
- Validaciones por campo con mensajes de error y animaciones entre pantallas.
- Datos ficticios, solo con fines académicos.

## Recursos nativos
- Ubicación del dispositivo (GPS) para completar la dirección de la alerta.
- Selector de imágenes de la galería para adjuntar evidencia.
- Vibración del dispositivo.

## Endpoints principales (backend)
- `/api/auth`: registro, inicio de sesión, `GET /api/auth/yo`, `PATCH /api/auth/perfil`, `POST /api/auth/cambiar-clave`.
- `/api/admin` (solo administrador): `GET /usuarios?estado=`, `GET /cuadras`, `POST /usuarios/{id}/aprobar`, `POST /usuarios/{id}/rechazar`, `PATCH /usuarios/{id}/rol`, `POST /usuarios/{id}/restablecer-clave`.
- `/api/alertas`: crear, listar, `PATCH /{id}/estado` y `DELETE /{id}`.
- `GET /api/salud/bd`: estado de la base de datos (solo administrador).

## Requisitos
Android Studio reciente (Android Gradle Plugin 9.4.1 y Gradle 9.6.0), JDK 21, Android SDK 36 y conexión a internet en la primera sincronización. El backend necesita acceso a la base Oracle con su wallet.

## Cómo ejecutar
1. Clonar: `git clone https://github.com/k4m1lo69/AlertasZaror.git`
2. **Base de datos**: ejecutar los scripts de `backend/db/` en orden numérico.
3. **Backend** (PowerShell, desde la carpeta `backend`), con las variables `DB_ALIAS`, `DB_WALLET_DIR`, `DB_USER` y `DB_PASSWORD` definidas en la sesión: `.\mvnw.cmd spring-boot:run`
   > Nota: en Windows, DB_WALLET_DIR debe usar barras normales (/), por ejemplo C:/Users/usuario/Downloads/Wallet_DuocUc2. Con \ el driver de Oracle falla con ORA-12261.
4. **App**: abrir la raíz en Android Studio, esperar el Gradle Sync y ejecutar en un emulador. El emulador alcanza el backend local por `10.0.2.2`. Para un teléfono físico, la URL base en `RetrofitClient` debe apuntar a una dirección alcanzable desde el teléfono.

## Pruebas y cobertura
- Backend: `cd backend` y luego `.\mvnw.cmd test`. Las pruebas de integración con Oracle se omiten si no hay variables de base de datos.
- App: `.\gradlew.bat testDebugUnitTest`
- Reporte de cobertura de la app: `.\gradlew.bat testDebugUnitTest createDebugUnitTestCoverageReport`. El HTML queda en `app/build/reports/coverage/test/debug/index.html`. La cobertura de lógica se mide sobre ViewModel, repositorios y dominio. Las pantallas Compose y el código generado por Room no se cubren con pruebas unitarias.

## APK firmado
- Keystore de este proyecto: `app/zaror-release.jks` (alias `zaror`). Fue creado solo para este curso. Las contraseñas se entregan aparte al docente.
- Las contraseñas se leen desde `keystore.properties`, que no se versiona. Debe estar en la raíz del proyecto con las claves `storeFile`, `storePassword`, `keyAlias` y `keyPassword`.
- Generar el APK: `.\gradlew.bat assembleRelease`. Queda en `app/build/outputs/apk/release/`.

## Solución de problemas
Si Gradle indica que no encuentra el SDK, falta el archivo `local.properties`, que es específico de cada equipo y no se versiona. Android Studio lo crea al abrir el proyecto. Si no, créalo en la raíz con `sdk.dir=C\:\\Users\\USUARIO\\AppData\\Local\\Android\\Sdk`.

## Seguimiento del trabajo
Planificación y seguimiento con GitHub (no se usó Trello): historial de commits, ramas por funcionalidad y Pull Requests, visibles en https://github.com/k4m1lo69/AlertasZaror/pulls y en la pestaña Commits.
