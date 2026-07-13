# EventService

Microservicio Spring Boot para la **gestión de eventos universitarios**: publicación/catálogo de eventos, filtrado por categoría y rango de fechas, y gestión de asistencia mediante reservas (RSVP) con control de cupo. Forma parte de una arquitectura de microservicios más amplia (paquete base `eci.dosw.alpha`, sufijo `Alpha`), donde este componente es responsable exclusivo del dominio "Eventos".

---

## 1. Descripción general y propósito de negocio

`EventService` expone una API REST que permite:

- **Publicar y consultar eventos** (charlas, actividades de bienestar, eventos técnicos, etc.), identificados por nombre, descripción, categoría, fecha y capacidad (cupo máximo de asistentes).
- **Filtrar eventos** por categoría (ej. `TECH`, `BIENESTAR`) y por rango de fechas (`startDate` / `endDate`, formato ISO `YYYY-MM-DD`).
- **Gestionar RSVP ("Répondez s'il vous plaît")**: los usuarios pueden **confirmar** su asistencia a un evento (lo que descuenta un cupo del `availableCapacity`) y **cancelar** su asistencia (lo que libera el cupo nuevamente).
- **Consultar la agenda de un usuario**: lista de identificadores de eventos en los que el usuario tiene una reserva actualmente confirmada.

El nombre de las entidades (`Event`, `RSVP`), de los controladores (`EventController`) y de las excepciones de negocio (`EventNotActiveException`, `CapacityExhaustedException`, `RsvpNotFoundException`) confirma que el dominio real del microservicio es la **gestión del ciclo de vida de eventos y de la asistencia/reservas asociadas a ellos**, con reglas de negocio centradas en el control de cupo y el estado del evento (`ACTIVE` / `CANCELLED`).

---

## 2. Arquitectura y patrón de diseño

El proyecto sigue una **arquitectura en capas (layered architecture)** clásica de Spring Boot, con separación estricta de responsabilidades:

```
Cliente HTTP
     │
     ▼
┌─────────────────────┐
│   controller/        │  ← Capa web: recibe HTTP, valida entrada mínima, delega y traduce la respuesta
│   EventController     │
└─────────┬─────────────┘
          ▼
┌─────────────────────┐
│   service/            │  ← Capa de negocio: reglas de dominio (cupo, estado del evento, RSVP)
│   EventService         │
└─────────┬─────────────┘
          ▼
┌─────────────────────┐
│   repository/         │  ← Capa de acceso a datos: interfaces Spring Data MongoDB
│   EventRepository       │
│   RSVPRepository        │
└─────────┬─────────────┘
          ▼
┌─────────────────────┐
│   model/               │  ← Entidades de persistencia (documentos MongoDB)
│   Event, RSVP           │
└─────────────────────┘
```

Capas transversales:

- **`dto/`**: objetos de transferencia de datos que desacoplan el contrato HTTP del modelo de persistencia (entrada de creación de eventos, entrada/salida de RSVP).
- **`exception/`**: excepciones de dominio específicas + un `@RestControllerAdvice` central que traduce excepciones en respuestas HTTP consistentes.
- **`config/`**: configuración de infraestructura (cliente MongoDB, documentación OpenAPI).

Este patrón corresponde a una variante de **arquitectura en 3 capas (3-tier) con inyección de dependencias por constructor**, propia del ecosistema Spring: los controladores no acceden nunca directamente a los repositorios, y la lógica de negocio (validaciones de cupo, transición de estados, cálculo de agenda) vive exclusivamente en `EventService`, manteniendo el controlador como una capa delgada ("thin controller").

---

## 3. Tecnologías y stack completo

| Tecnología / Dependencia | Versión | Propósito en el proyecto | Por qué se eligió |
|---|---|---|---|
| **Java** | 21 (LTS) | Lenguaje de implementación | LTS más reciente disponible al momento del desarrollo; soporta records, pattern matching, virtual threads y mejoras de rendimiento sobre versiones anteriores; total compatibilidad con Spring Boot 4.x. |
| **Spring Boot** (`spring-boot-starter-parent`) | 4.1.0 | Framework base, gestión de dependencias transitivas y auto-configuración | Reduce drásticamente el código de arranque (`main()` de una sola línea), estandariza la configuración vía `application.properties`, e integra de forma nativa el resto del stack (web, datos, testing). |
| **spring-boot-starter-web** | gestionado por el parent | Servidor embebido (Tomcat) + Spring MVC para exponer la API REST | Permite levantar un servicio HTTP autocontenible sin necesidad de un servidor de aplicaciones externo, ideal para el modelo de despliegue en contenedores de un microservicio. |
| **spring-boot-starter-data-mongodb** | gestionado por el parent | Integración con MongoDB vía Spring Data (repositorios, plantilla, mapeo de documentos) | Proporciona repositorios declarativos (`MongoRepository`) que eliminan código boilerplate de acceso a datos y encajan naturalmente con el modelo semi-estructurado de un evento (campos opcionales, evolución de esquema sin migraciones rígidas). |
| **MongoDB Driver Sync** (`com.mongodb.client`) | transitivo del starter | Cliente Java nativo usado en `MongoConfig` para crear el `MongoClient` | Necesario para construir explícitamente el `MongoClient`/`MongoDatabaseFactory`/`MongoTemplate` de forma personalizada en vez de depender solo del auto-configurado por Spring Boot. |
| **Lombok** | gestionada por el parent (`optional=true`) | Generación de getters/setters/constructores/equals/hashCode en tiempo de compilación (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`) | Elimina código repetitivo en entidades y DTOs, mejora la legibilidad y reduce la probabilidad de errores manuales en getters/setters. |
| **springdoc-openapi-starter-webmvc-ui** | 2.8.14 | Generación automática de la especificación OpenAPI 3 y UI interactiva (Swagger UI) a partir de las anotaciones del controlador | Facilita la documentación viva de la API (siempre sincronizada con el código), imprescindible en un ecosistema de microservicios donde otros equipos/servicios consumen este contrato. |
| **spring-boot-starter-test** | gestionado por el parent | Conjunto de librerías de testing (JUnit 5, Mockito, AssertJ, Spring Test) | Es el starter estándar de Spring Boot para pruebas unitarias y de integración; incluye todo lo necesario sin configurar dependencias sueltas. |
| **JUnit 5 (Jupiter)** | transitivo de `spring-boot-starter-test` | Framework de ejecución de pruebas unitarias | Estándar de facto en el ecosistema Java moderno; soporta extensiones (`@ExtendWith`), ciclo de vida por método y aserciones expresivas. |
| **Mockito** (`mockito-junit-jupiter`) | transitivo de `spring-boot-starter-test` | Mocking de `EventRepository`, `RSVPRepository` y `EventService` en pruebas unitarias | Permite aislar la unidad bajo prueba (servicio o controlador) de sus dependencias reales, evitando levantar una base de datos para pruebas unitarias rápidas. |
| **AssertJ** | transitivo de `spring-boot-starter-test` | Aserciones fluidas (`assertThat(...).isEqualTo(...)`) | API más legible y expresiva que las aserciones nativas de JUnit, con mejores mensajes de fallo. |
| **JaCoCo Maven Plugin** | 0.8.12 | Medición de cobertura de código y verificación de un umbral mínimo (80% de instrucciones) en la fase `verify` | Garantiza automáticamente en el pipeline de build que la lógica de negocio (excluyendo `model`, `dto`, `repository`, `config` y la clase principal) mantiene un piso de cobertura de pruebas. |
| **spring-boot-maven-plugin** | gestionado por el parent | Empaquetado del proyecto como JAR ejecutable ("fat jar") con servidor embebido | Permite generar un artefacto autocontenible (`app.jar`) apto para ejecutarse con `java -jar`, requisito para la contenerización con Docker. |
| **Maven Wrapper (mvnw)** | Maven 3.9.16 (wrapper 3.3.4) | Build tool reproducible sin depender de una instalación local de Maven | Garantiza que cualquier desarrollador o pipeline de CI/CD use exactamente la misma versión de Maven, evitando incompatibilidades de build. |

> Nota: el `pom.xml` no declara explícitamente `spring-boot-starter-validation`, `spring-security`, `mapstruct`, clientes Feign/Eureka ni drivers de bases de datos relacionales — este microservicio **no** usa base de datos relacional, ni seguridad basada en tokens, ni service discovery vía Eureka en su configuración actual. Todo el acceso a datos se realiza sobre **MongoDB**.

---

## 4. Estructura de paquetes

```
src/main/java/eci/dosw/alpha/EventService/
├── EventServiceApplication.java     # Clase principal (@SpringBootApplication)
├── config/
│   ├── MongoConfig.java             # Configuración manual del cliente/plantilla MongoDB
│   └── OpenApiConfig.java           # Metadatos de la documentación OpenAPI/Swagger
├── controller/
│   └── EventController.java         # Capa REST: expone /events y sub-rutas
├── service/
│   └── EventService.java            # Lógica de negocio: eventos y RSVP
├── repository/
│   ├── EventRepository.java         # Acceso a datos de eventos (Spring Data MongoDB)
│   └── RSVPRepository.java          # Acceso a datos de reservas RSVP
├── model/
│   ├── Event.java                   # Documento MongoDB "events"
│   └── RSVP.java                    # Documento MongoDB "rsvps"
├── dto/
│   ├── CreateEventRequest.java      # Entrada para crear un evento
│   ├── RSVPRequest.java             # Entrada para confirmar/cancelar un RSVP
│   └── RSVPResponse.java            # Salida de las operaciones de RSVP
└── exception/
    ├── EventNotActiveException.java     # Error de negocio E1
    ├── CapacityExhaustedException.java  # Error de negocio E2
    ├── RsvpNotFoundException.java       # Error de negocio E3
    └── GlobalExceptionHandler.java      # @RestControllerAdvice centralizado

src/test/java/eci/dosw/alpha/EventService/
├── EventServiceApplicationTests.java        # Pruebas unitarias de EventService (con Mockito)
└── EventControllerAndExceptionTest.java     # Pruebas unitarias de EventController y GlobalExceptionHandler

src/main/resources/
└── application.properties           # Configuración única (puerto, nombre de app, URI de Mongo)
```

Explicación de cada paquete:

- **`(raíz) eci.dosw.alpha.EventService`**: contiene únicamente la clase de arranque `EventServiceApplication`.
- **`config`**: beans de infraestructura que no encajan en ninguna capa de negocio (cliente MongoDB personalizado, configuración de Swagger/OpenAPI).
- **`controller`**: adaptadores de entrada HTTP (puerto de entrada en términos de arquitectura hexagonal); traduce peticiones/respuestas HTTP a llamadas de servicio.
- **`service`**: núcleo de la lógica de negocio; contiene todas las reglas y validaciones del dominio "eventos y asistencia".
- **`repository`**: interfaces declarativas de persistencia que extienden `MongoRepository`, sin implementación manual.
- **`model`**: entidades de dominio/persistencia mapeadas como documentos de MongoDB (`@Document`).
- **`dto`**: contratos de entrada/salida de la API, independientes del modelo de persistencia.
- **`exception`**: excepciones de negocio tipadas + manejador global de errores.

---

## 5. Catálogo detallado de clases

### 5.1 Clase principal

#### `EventServiceApplication` (raíz del paquete)
- **Anotaciones**: `@SpringBootApplication` (combina `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`).
- **Responsabilidad**: punto de entrada de la aplicación. Su método `main(String[] args)` invoca `SpringApplication.run(...)`, arrancando el contenedor de Spring, el servidor embebido (Tomcat) y disparando el escaneo de componentes de todo el paquete base `eci.dosw.alpha.EventService`.
- **Relaciones**: implícitamente activa el descubrimiento de todos los `@RestController`, `@Service`, `@Repository` (interfaces Spring Data) y `@Configuration` del proyecto.

### 5.2 Paquete `config`

#### `MongoConfig`
- **Paquete**: `eci.dosw.alpha.EventService.config`
- **Anotaciones**: `@Configuration`.
- **Responsabilidad**: define manualmente la cadena de infraestructura de acceso a MongoDB en lugar de dejarlo completamente a la auto-configuración de Spring Boot.
- **Campos**: `mongoUri` inyectado vía `@Value("${spring.data.mongodb.uri}")`.
- **Beans que expone**:
  - `mongoClient()`: crea un `com.mongodb.client.MongoClient` a partir de la URI configurada.
  - `mongoDatabaseFactory(MongoClient)`: crea un `SimpleMongoClientDatabaseFactory` apuntando a la base de datos `eventsdb`.
  - `mongoTemplate(MongoDatabaseFactory)`: expone un `MongoTemplate`, la API de bajo nivel de Spring Data MongoDB (aunque en el estado actual del proyecto los repositorios `MongoRepository` son el mecanismo de acceso a datos usado directamente; `MongoTemplate` queda disponible como bean para operaciones más avanzadas si se necesitan).
- **Relaciones**: provee la infraestructura sobre la que operan `EventRepository` y `RSVPRepository`.

#### `OpenApiConfig`
- **Paquete**: `eci.dosw.alpha.EventService.config`
- **Anotaciones**: `@Configuration`.
- **Responsabilidad**: define los metadatos globales (título, descripción, versión) de la especificación OpenAPI generada por springdoc.
- **Bean que expone**: `eventServiceOpenAPI()` → `OpenAPI` con título `"EventService API"`, descripción de negocio y versión `"1.0.0"`.
- **Relaciones**: springdoc combina este bean con las anotaciones `@Operation`/`@ApiResponse`/`@Tag` presentes en `EventController` para generar `/v3/api-docs` y la UI de Swagger.

### 5.3 Paquete `model` (entidades de persistencia)

#### `Event`
- **Paquete**: `eci.dosw.alpha.EventService.model`
- **Anotaciones**: `@Data` (Lombok, genera getters/setters/`equals`/`hashCode`/`toString`), `@Document(collection = "events")` (mapea la clase a la colección MongoDB `events`).
- **Responsabilidad**: representar un evento persistido.
- **Campos**:
  - `id` (`@Id`, `String`): identificador único generado por MongoDB (`ObjectId` como `String`).
  - `name`, `description`, `category` (`String`): metadatos descriptivos del evento.
  - `date` (`String`, formato `YYYY-MM-DD`): fecha del evento; se maneja como cadena para permitir comparación lexicográfica directa en los filtros de rango.
  - `capacity` (`int`): cupo total configurado al crear el evento.
  - `availableCapacity` (`int`): cupo restante; se decrementa en cada RSVP confirmado y se incrementa al cancelar uno.
  - `status` (`String`): estado del evento, valores esperados `"ACTIVE"` o `"CANCELLED"`.
- **Relaciones**: referenciado por `RSVP` mediante `eventId` (relación lógica, no hay `@DBRef` — es una referencia por identificador, típica en modelado NoSQL para evitar documentos anidados grandes).

#### `RSVP`
- **Paquete**: `eci.dosw.alpha.EventService.model`
- **Anotaciones**: `@Data`, `@Document(collection = "rsvps")`.
- **Responsabilidad**: representar la reserva/confirmación de asistencia de un usuario a un evento.
- **Campos**:
  - `id` (`@Id`, `String`).
  - `userId` (`String`): identificador del usuario que reserva (no se valida contra un servicio de usuarios externo dentro de este microservicio).
  - `eventId` (`String`): referencia lógica al `Event.id`.
  - `status` (`String`): `"CONFIRMED"` o `"CANCELLED"`.

### 5.4 Paquete `repository` (acceso a datos)

#### `EventRepository` (interfaz)
- **Paquete**: `eci.dosw.alpha.EventService.repository`
- **Extiende**: `MongoRepository<Event, String>` → hereda automáticamente `findAll()`, `findById()`, `save()`, `deleteById()`, etc.
- **Métodos declarados propios**:
  - `List<Event> findByCategory(String category)`: consulta derivada (query derivation) de Spring Data que genera automáticamente la consulta Mongo equivalente a `{ category: ? }`.
- **Relaciones**: usado exclusivamente por `EventService`.

#### `RSVPRepository` (interfaz)
- **Paquete**: `eci.dosw.alpha.EventService.repository`
- **Extiende**: `MongoRepository<RSVP, String>`.
- **Métodos declarados propios**:
  - `Optional<RSVP> findByUserIdAndEventId(String userId, String eventId)`: localiza la reserva única de un usuario para un evento específico (clave de negocio compuesta).
  - `List<RSVP> findByUserIdAndStatus(String userId, String status)`: usado para calcular la agenda de eventos confirmados de un usuario.
- **Relaciones**: usado exclusivamente por `EventService`.

### 5.5 Paquete `dto` (contratos de entrada/salida)

#### `CreateEventRequest`
- **Paquete**: `eci.dosw.alpha.EventService.dto`
- **Anotaciones**: `@Data`.
- **Responsabilidad**: modelar el cuerpo JSON de entrada para `POST /events`, evitando exponer directamente la entidad `Event` en la capa web (mitiga *mass assignment*, documentado explícitamente en el Javadoc de la clase como corrección al hallazgo SonarCloud `java:S4684`).
- **Campos**: `name`, `description`, `category`, `date`, `capacity` (sin `id`, `status` ni `availableCapacity`, que son responsabilidad exclusiva del servidor).

#### `RSVPRequest`
- **Paquete**: `eci.dosw.alpha.EventService.dto`
- **Anotaciones**: `@Data`.
- **Responsabilidad**: cuerpo de entrada para confirmar (`POST /events/{id}/rsvp`) y cancelar (`PUT /events/{id}/rsvp`) una reserva.
- **Campos**: `userId` (único campo requerido; el `eventId` viaja como parte de la ruta).

#### `RSVPResponse`
- **Paquete**: `eci.dosw.alpha.EventService.dto`
- **Anotaciones**: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`.
- **Responsabilidad**: cuerpo de salida común a las operaciones de confirmar/cancelar RSVP.
- **Campos**: `rsvpStatus` (`"CONFIRMED"`/`"CANCELLED"`), `availableCapacity` (cupo actualizado del evento tras la operación), `agendaItems` (lista de IDs de eventos confirmados del usuario, recalculada en cada respuesta).

### 5.6 Paquete `exception` (errores de dominio y manejo centralizado)

#### `EventNotActiveException`
- **Extiende**: `RuntimeException` (excepción no verificada).
- **Código de negocio**: **E1**.
- **Cuándo se lanza**: al intentar confirmar un RSVP sobre un evento cuyo `status` no es `"ACTIVE"` (por ejemplo, `"CANCELLED"`).
- **Mensaje**: incluye el `eventId` afectado.

#### `CapacityExhaustedException`
- **Extiende**: `RuntimeException`.
- **Código de negocio**: **E2**.
- **Cuándo se lanza**: al intentar confirmar un RSVP cuando `availableCapacity <= 0`.

#### `RsvpNotFoundException`
- **Extiende**: `RuntimeException`.
- **Código de negocio**: **E3**.
- **Cuándo se lanza**: al intentar cancelar un RSVP que no existe para el par `(userId, eventId)`.
- **Mensaje**: incluye ambos identificadores.

#### `GlobalExceptionHandler`
- **Anotaciones**: `@RestControllerAdvice` (intercepta excepciones lanzadas por cualquier `@RestController` de la aplicación).
- **Responsabilidad**: centralizar la traducción de excepciones de negocio a respuestas HTTP consistentes, evitando bloques `try/catch` repetidos en el controlador.
- **Manejadores**:
  - `handleEventNotActive(EventNotActiveException)` → `409 CONFLICT`, cuerpo `{ "error": "...", "code": "E1" }`.
  - `handleCapacityExhausted(CapacityExhaustedException)` → `409 CONFLICT`, cuerpo `{ "error": "...", "code": "E2" }`.
  - `handleRsvpNotFound(RsvpNotFoundException)` → `404 NOT_FOUND`, cuerpo `{ "error": "...", "code": "E3" }`.
  - `handleGeneric(RuntimeException)` → `500 INTERNAL_SERVER_ERROR`, cuerpo `{ "error": "..." }` (captura genérica, incluye por ejemplo el "evento no encontrado" o "RSVP ya confirmado/cancelado" lanzados como `RuntimeException` simple dentro de `EventService`).

### 5.7 Paquete `service` (lógica de negocio)

#### `EventService`
- **Paquete**: `eci.dosw.alpha.EventService.service`
- **Anotaciones**: `@Service`.
- **Dependencias inyectadas** (constructor, sin `@Autowired` explícito — inyección implícita de Spring por constructor único): `EventRepository`, `RSVPRepository`.
- **Responsabilidad**: concentrar toda la lógica de negocio del dominio eventos/RSVP.
- **Métodos públicos**:
  - `getAllEvents()`: retorna todos los eventos (`findAll`).
  - `getEventsByCategory(String category)`: delega en `findByCategory`.
  - `getEventsByFilter(String category, String startDate, String endDate)`: filtro combinado y **opcional** — parte de todos los eventos o de los filtrados por categoría, y aplica después filtrado en memoria (`Stream`) por comparación lexicográfica de fechas ISO (`compareTo`) para `startDate`/`endDate`.
  - `getEventById(String id)`: busca por ID; si no existe lanza `RuntimeException("Evento no encontrado: " + id)` genérica (capturada por `handleGeneric` → 500).
  - `createEvent(Event event)`: inicializa `availableCapacity = capacity` y `status = "ACTIVE"` antes de persistir. Esta es la única vía de creación; no acepta un `Event` ya persistido con estado arbitrario.
  - `confirmRSVP(String userId, String eventId)` — **regla de negocio central**:
    1. Recupera el evento (`getEventById`, puede fallar con 500 si no existe).
    2. Valida `status == "ACTIVE"`; si no, lanza `EventNotActiveException` (**E1** → 409).
    3. Valida `availableCapacity > 0`; si no, lanza `CapacityExhaustedException` (**E2** → 409).
    4. Busca un RSVP previo del usuario para ese evento; si ya existe uno con `status = "CONFIRMED"`, lanza `RuntimeException` ("Ya tienes un RSVP confirmado...") → 500.
    5. Si no existe o estaba `CANCELLED`, crea/reutiliza el `RSVP`, lo marca `CONFIRMED` y lo guarda (permite **re-confirmar** tras una cancelación previa).
    6. Decrementa `availableCapacity` del evento y lo persiste.
    7. Retorna `RSVPResponse` con el nuevo estado, el cupo actualizado y la agenda recalculada del usuario.
  - `cancelRSVP(String userId, String eventId)` — **regla de negocio central**:
    1. Busca el RSVP del par `(userId, eventId)`; si no existe, lanza `RsvpNotFoundException` (**E3** → 404).
    2. Si ya estaba `CANCELLED`, lanza `RuntimeException` ("El RSVP ya estaba cancelado.") → 500.
    3. Marca el RSVP como `CANCELLED` y lo persiste.
    4. Incrementa `availableCapacity` del evento (libera el cupo) y lo persiste.
    5. Retorna `RSVPResponse` con el estado actualizado, cupo y agenda recalculada.
  - `getUserAgenda(String userId)`: consulta todos los RSVP del usuario con `status = "CONFIRMED"` y mapea a la lista de `eventId`.
- **Relaciones**: única clase que orquesta `EventRepository` y `RSVPRepository`; es la dependencia directa (única) de `EventController`; lanza las tres excepciones de negocio tipadas del paquete `exception`.

### 5.8 Paquete `controller` (capa web)

#### `EventController`
- **Paquete**: `eci.dosw.alpha.EventService.controller`
- **Anotaciones**: `@RestController`, `@RequestMapping("/events")`, `@Tag(name = "Eventos", ...)` (agrupación en Swagger UI).
- **Dependencia inyectada**: `EventService` (constructor).
- **Responsabilidad**: exponer y documentar (vía anotaciones springdoc `@Operation`/`@ApiResponse(s)`/`@Parameter`) los endpoints REST del dominio, delegando toda la lógica al `EventService` y envolviendo las respuestas en `ResponseEntity`.
- **Métodos / endpoints** (ver detalle completo en la sección 6).

### 5.9 Clases de prueba (`src/test/java`)

#### `EventServiceApplicationTests`
- **Framework**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) + AssertJ.
- **Objetivo**: pruebas unitarias puras de `EventService`, con `EventRepository` y `RSVPRepository` mockeados (`@Mock`) e inyectados en el servicio bajo prueba (`@InjectMocks`).
- **Casos cubiertos**: listado de todos los eventos, filtrado por categoría, filtrado combinado (sin filtros, solo categoría, solo `startDate`, solo `endDate`, rango completo), búsqueda por ID (encontrado / no encontrado), creación de evento (fija `status=ACTIVE` y `availableCapacity=capacity`), confirmación de RSVP (éxito, error E1 evento no activo, error E2 sin cupo, RSVP ya confirmado, re-confirmación tras cancelación), cancelación de RSVP (éxito, error E3 no encontrado, ya cancelado), y cálculo de agenda (con resultados y vacío).
- Pese a su nombre (heredado de la plantilla por defecto de Spring Initializr, que normalmente solo verifica que el contexto de Spring cargue), **esta clase fue reescrita como suite de pruebas unitarias de `EventService`** y no realiza un `@SpringBootTest` de carga de contexto completo.

#### `EventControllerAndExceptionTest`
- **Framework**: JUnit 5 + Mockito + AssertJ.
- **Objetivo**: pruebas unitarias de `EventController` (con `EventService` mockeado) y de `GlobalExceptionHandler` (instanciado directamente, sin contexto Spring).
- **Casos cubiertos**: cada endpoint del controlador delega correctamente al servicio y propaga la respuesta (`getEvents` con y sin filtros, `getEvent` por ID, `createEvent`, `confirmRSVP`, `cancelRSVP`, `getAgenda`); cada manejador de `GlobalExceptionHandler` retorna el código de negocio (`E1`/`E2`/`E3`) y el mensaje esperado; los mensajes de las tres excepciones de negocio contienen los identificadores relevantes.

---

## 6. Endpoints REST expuestos

Todos los endpoints están bajo el prefijo base `/events`.

| Método | Ruta | Descripción | Request Body | Response Body | Códigos de estado |
|---|---|---|---|---|---|
| `GET` | `/events` | Lista eventos, con filtros opcionales de `category`, `startDate`, `endDate` (query params, formato `YYYY-MM-DD`) | — | `List<Event>` | `200 OK` (lista, puede ser vacía) |
| `GET` | `/events/{id}` | Obtiene un evento por su ID | — | `Event` | `200 OK`; `500 INTERNAL_SERVER_ERROR` si no existe (excepción genérica, sin código de negocio propio) |
| `POST` | `/events` | Crea un nuevo evento. El servidor fija `status=ACTIVE` y `availableCapacity=capacity` | `CreateEventRequest` (`name`, `description`, `category`, `date`, `capacity`) | `Event` (documento creado, incluye `id` generado) | `200 OK` |
| `POST` | `/events/{id}/rsvp` | Confirma la asistencia (RSVP) de un usuario a un evento y descuenta un cupo | `RSVPRequest` (`userId`) | `RSVPResponse` (`rsvpStatus`, `availableCapacity`, `agendaItems`) | `200 OK`; `409 CONFLICT` con `code=E1` (evento no activo) o `code=E2` (cupo agotado); `500` si ya existe un RSVP `CONFIRMED` previo o el evento no existe |
| `PUT` | `/events/{id}/rsvp` | Cancela el RSVP de un usuario para un evento y libera el cupo | `RSVPRequest` (`userId`) | `RSVPResponse` | `200 OK`; `404 NOT_FOUND` con `code=E3` (RSVP no encontrado); `500` si el RSVP ya estaba cancelado |
| `GET` | `/events/agenda?userId=...` | Devuelve los IDs de eventos con RSVP `CONFIRMED` para el usuario | — (query param `userId`) | `List<String>` (IDs de evento) | `200 OK` |

Adicionalmente, gracias a `springdoc-openapi-starter-webmvc-ui`, el servicio expone automáticamente:

- `GET /v3/api-docs` — especificación OpenAPI 3 en JSON.
- `GET /swagger-ui.html` (redirige a `/swagger-ui/index.html`) — interfaz interactiva para explorar y probar la API.

> Observación de diseño: el error "evento no encontrado" (en `getEventById`) y algunos errores de conflicto no modelados con excepción propia (RSVP ya confirmado, RSVP ya cancelado) se propagan como `RuntimeException` genérica y son capturados por el manejador `handleGeneric`, devolviendo `500` en vez de un código semánticamente más preciso (`404`/`409`). Es un punto de mejora a tener en cuenta si se redacta documentación técnica formal o se plantea evolucionar el manejo de errores.

---

## 7. Modelo de datos / entidades y relaciones

MongoDB es una base de datos **documental (NoSQL)**, por lo que no existen claves foráneas ni JOINs; las relaciones entre `Event` y `RSVP` son **lógicas**, mantenidas por convención a nivel de aplicación mediante el campo `eventId`.

### Colección `events` (entidad `Event`)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` (`ObjectId`) | Identificador único generado por MongoDB. |
| `name` | `String` | Nombre del evento. |
| `description` | `String` | Descripción del evento. |
| `category` | `String` | Categoría (ej. `TECH`, `BIENESTAR`). |
| `date` | `String` (`YYYY-MM-DD`) | Fecha del evento, comparada lexicográficamente en los filtros. |
| `capacity` | `int` | Cupo máximo total. |
| `availableCapacity` | `int` | Cupo restante disponible; fluctúa con cada RSVP. |
| `status` | `String` | `ACTIVE` o `CANCELLED`. |

### Colección `rsvps` (entidad `RSVP`)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` (`ObjectId`) | Identificador único generado por MongoDB. |
| `userId` | `String` | Identificador del usuario que reserva. |
| `eventId` | `String` | Referencia lógica a `Event.id`. |
| `status` | `String` | `CONFIRMED` o `CANCELLED`. |

**Relación conceptual**: un `Event` puede tener **0..N** `RSVP` asociados (uno por usuario que reserva); un usuario (`userId`) puede tener a lo sumo un `RSVP` "vigente" por evento gracias a la consulta `findByUserIdAndEventId`, que la lógica de `EventService` reutiliza en vez de crear duplicados (patrón "upsert lógico": reutiliza el documento existente si el usuario ya había reservado y cancelado antes).

No existen `@DBRef` (referencias nativas de Spring Data MongoDB) — la relación se resuelve manualmente en la capa de servicio, lo cual es una práctica común en MongoDB para evitar los costos de resolución de referencias y mantener los documentos desacoplados.

---

## 8. Configuración

### `src/main/resources/application.properties`

```properties
spring.application.name=EventService
spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/eventsdb}
server.port=8081
```

| Propiedad | Valor por defecto | Variable de entorno que la sobreescribe | Descripción |
|---|---|---|---|
| `spring.application.name` | `EventService` | — | Nombre lógico de la aplicación (usado en logs, actuator si se añadiera, y potencialmente en un futuro registro de descubrimiento de servicios). |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/eventsdb` | `SPRING_DATA_MONGODB_URI` | Cadena de conexión completa a MongoDB (host, puerto, base de datos `eventsdb`). Permite apuntar a un MongoDB local en desarrollo o a una instancia gestionada/en contenedor en otros entornos sin recompilar. |
| `server.port` | `8081` | — (fijo en el archivo) | Puerto HTTP en el que Tomcat embebido expone la API. |

**Perfiles**: el proyecto **no define archivos `application-{profile}.yml/properties`** (ni `dev`, `prod` ni `docker`); existe una única configuración base, y la externalización a distintos entornos se resuelve exclusivamente mediante la variable de entorno `SPRING_DATA_MONGODB_URI`. Esto es coherente con el patrón de **configuración externalizada de doce factores (12-factor app)**: el binario/imagen es el mismo en todos los entornos, y solo cambia la variable de entorno inyectada en tiempo de despliegue (por ejemplo, en `docker run -e SPRING_DATA_MONGODB_URI=...` o en las variables de un orquestador).

**Otros archivos de `resources`**: no existen `schema.sql`, `data.sql` ni archivos de mensajes internacionalizados (`messages.properties`) — al ser MongoDB (esquema flexible, sin necesidad de scripts DDL) y no requerir aún i18n, no son necesarios.

---

## 9. Persistencia

- **Motor de base de datos**: **MongoDB** (NoSQL documental).
- **ORM / capa de mapeo**: **Spring Data MongoDB**, que mapea clases anotadas con `@Document` a colecciones y expone repositorios declarativos (`MongoRepository<T, ID>`) con *query derivation* a partir del nombre del método (`findByCategory`, `findByUserIdAndEventId`, `findByUserIdAndStatus`).
- **Migraciones**: no aplica en el sentido tradicional (no hay Flyway/Liquibase ni scripts DDL), ya que MongoDB no impone un esquema rígido; la "evolución de esquema" ocurre añadiendo o quitando campos en las clases `@Document` sin necesidad de scripts de migración.
- **Cliente MongoDB**: configurado explícitamente en `MongoConfig` (en vez de dejar toda la configuración a la auto-configuración por defecto de `spring-boot-starter-data-mongodb`), construyendo manualmente `MongoClient`, `MongoDatabaseFactory` (apuntando a la base `eventsdb`) y `MongoTemplate`.

---

## 10. Seguridad

El proyecto **no incluye `spring-boot-starter-security`** ni ninguna dependencia de autenticación/autorización (no hay JWT, OAuth2, filtros de seguridad, roles ni `@PreAuthorize`). Todos los endpoints están, en el estado actual del código, **abiertos sin autenticación**. El campo `userId` en `RSVPRequest` se recibe tal cual del cliente sin validarse contra un servicio de identidad — la responsabilidad de autenticar al usuario y garantizar que el `userId` enviado corresponde al usuario autenticado recaería, en una arquitectura de microservicios más amplia, en un API Gateway o un servicio de identidad externo (fuera del alcance de este componente).

---

## 11. Manejo de errores / excepciones

El manejo de errores está centralizado en `GlobalExceptionHandler` (`@RestControllerAdvice`), que evita repetir bloques `try/catch` en `EventController`. Jerarquía de excepciones de negocio (todas extienden `RuntimeException`, es decir, son *unchecked*):

| Excepción | Código de negocio | HTTP Status | Disparador |
|---|---|---|---|
| `EventNotActiveException` | `E1` | `409 CONFLICT` | RSVP sobre un evento no `ACTIVE`. |
| `CapacityExhaustedException` | `E2` | `409 CONFLICT` | RSVP sobre un evento sin cupo disponible. |
| `RsvpNotFoundException` | `E3` | `404 NOT_FOUND` | Cancelación de un RSVP inexistente. |
| `RuntimeException` (genérica) | — | `500 INTERNAL_SERVER_ERROR` | Evento no encontrado por ID, RSVP duplicado ya confirmado, RSVP ya cancelado (casos sin excepción tipada propia). |

El cuerpo de error sigue el formato uniforme `{ "error": "<mensaje>", "code": "<E1|E2|E3>" }` (el manejador genérico omite el campo `code`).

---

## 12. Comunicación con otros microservicios

En el estado actual del código **no existen clientes salientes** hacia otros microservicios: no hay `RestTemplate`, `WebClient`, `FeignClient`, ni configuración de `Eureka`/`Consul`/`spring-cloud-*` en el `pom.xml`. `EventService` es, por tanto, un microservicio **autocontenido** cuya única dependencia externa de infraestructura es la instancia de MongoDB (`spring.data.mongodb.uri`). Al no depender de descubrimiento de servicios, su integración con el resto del ecosistema (por ejemplo, un servicio de usuarios que valide el `userId` de un RSVP, o un API Gateway que enrute `/events/**` hacia este servicio) se asume resuelta a nivel de infraestructura externa (gateway, red del contenedor, orquestador), no dentro del código de este repositorio.

---

## 13. Contenerización (Dockerfile)

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

El `Dockerfile` implementa un **build multi-etapa (multi-stage build)**:

1. **Etapa `builder`**: parte de `eclipse-temurin:21-jdk-alpine` (imagen Alpine con el JDK 21 completo, necesario para compilar). Copia todo el contexto del proyecto, otorga permisos de ejecución al Maven Wrapper y ejecuta `./mvnw -B package -DskipTests` en modo *batch* (`-B`, sin interacción) para generar el JAR ejecutable, **omitiendo las pruebas** en la etapa de construcción de imagen (las pruebas se asumen ejecutadas antes, típicamente en el pipeline de CI).
2. **Etapa final**: parte de `eclipse-temurin:21-jre-alpine`, una imagen mucho más liviana que solo contiene el **JRE** (no el JDK completo), suficiente para *ejecutar* la aplicación pero no para compilarla. Copia únicamente el artefacto `target/*.jar` generado en la etapa anterior como `app.jar`, descartando el código fuente, Maven y las dependencias de compilación del resultado final.
3. **`EXPOSE 8081`**: documenta el puerto en el que la aplicación escucha (coincide con `server.port=8081`).
4. **`ENTRYPOINT ["java", "-jar", "app.jar"]`**: arranca el JAR ejecutable como proceso principal del contenedor.

**Por qué este enfoque**:
- El *multi-stage build* produce una **imagen final significativamente más pequeña y con menor superficie de ataque**, al no incluir el JDK, Maven ni el código fuente/tests en la imagen que se despliega en producción.
- Usar **Alpine Linux** como base reduce aún más el tamaño de la imagen frente a distribuciones basadas en Debian/Ubuntu.
- Fijar la misma versión de Java (21) en ambas etapas evita incompatibilidades de bytecode entre build y runtime.
- El uso del **Maven Wrapper** dentro del contenedor asegura una compilación reproducible sin depender de que la imagen base tenga Maven preinstalado con una versión concreta.

---

## 14. Testing

| Aspecto | Detalle |
|---|---|
| **Framework de ejecución** | JUnit 5 (Jupiter), vía `@ExtendWith(MockitoExtension.class)`. |
| **Mocking** | Mockito (`@Mock`, `@InjectMocks`) para aislar `EventService` y `EventController` de sus dependencias reales. |
| **Aserciones** | AssertJ (`assertThat`, `assertThatThrownBy`) para aserciones fluidas y verificación de excepciones. |
| **Tipo de pruebas presentes** | Exclusivamente **pruebas unitarias** (no hay `@SpringBootTest` con contexto completo, ni `MockMvc`/`WebTestClient` para pruebas de integración HTTP reales, ni Testcontainers para levantar un MongoDB real en pruebas). |
| **Clases bajo prueba** | `EventService` (todas sus reglas de negocio, incluidos los tres errores E1/E2/E3 y los casos límite de reconfirmación/doble cancelación) y `EventController` + `GlobalExceptionHandler` (delegación correcta a servicio y mapeo de excepciones a respuestas HTTP). |
| **Clases sin prueba dedicada** | `EventServiceApplication`, `MongoConfig`, `OpenApiConfig`, `EventRepository`/`RSVPRepository` (interfaces Spring Data, sin lógica propia que probar), y las clases `dto`/`model` (POJOs generados por Lombok) — todas ellas están además **excluidas explícitamente** del cálculo de cobertura en el plugin JaCoCo (`**/EventServiceApplication.class`, `**/config/**`, `**/model/**`, `**/dto/**`, `**/repository/**`). |
| **Umbral de cobertura exigido (JaCoCo)** | 80% de instrucciones (`INSTRUCTION` / `COVEREDRATIO ≥ 0.80`) a nivel de todo el `BUNDLE`, verificado en la fase `verify` del ciclo de vida Maven. |

---

## 15. Cómo ejecutar el proyecto localmente

### Requisitos previos
- JDK 21 instalado (o usar directamente el wrapper, que no requiere Maven instalado globalmente, pero sí un JDK 21 disponible en el `PATH`/`JAVA_HOME`).
- Una instancia de MongoDB accesible (local en `localhost:27017` por defecto, o remota vía `SPRING_DATA_MONGODB_URI`).
- Docker (opcional, para ejecución contenerizada).

### Ejecutar con Maven Wrapper (desarrollo local)

```bash
# Compilar, ejecutar pruebas y generar el reporte/verificación de cobertura JaCoCo
./mvnw clean verify

# Compilar sin ejecutar pruebas
./mvnw clean package -DskipTests

# Ejecutar la aplicación directamente con el plugin de Spring Boot
./mvnw spring-boot:run
```
En Windows, usar `mvnw.cmd` en lugar de `./mvnw`.

Por defecto, la aplicación intentará conectarse a `mongodb://localhost:27017/eventsdb`. Para apuntar a otra instancia:

```bash
# Bash / PowerShell (ejemplo conceptual, ajustar sintaxis según la shell)
SPRING_DATA_MONGODB_URI="mongodb://usuario:password@host:27017/eventsdb" ./mvnw spring-boot:run
```

Una vez iniciada, la API queda disponible en `http://localhost:8081/events`, y la documentación interactiva en `http://localhost:8081/swagger-ui.html`.

### Ejecutar con Docker

```bash
# Construir la imagen
docker build -t eventservice:latest .

# Ejecutar el contenedor, apuntando a un MongoDB accesible desde la red del contenedor
docker run -p 8081:8081 \
  -e SPRING_DATA_MONGODB_URI="mongodb://host.docker.internal:27017/eventsdb" \
  eventservice:latest
```

### Ejecutar solo las pruebas

```bash
./mvnw test
```

---

## 16. Justificación de decisiones tecnológicas

- **Spring Boot como framework base**: reduce el tiempo de arranque de un microservicio nuevo al proveer auto-configuración, un servidor embebido y un modelo de configuración externalizada estándar (`application.properties` + variables de entorno), lo cual encaja naturalmente con el patrón de despliegue en contenedores independientes que caracteriza a una arquitectura de microservicios.
- **MongoDB como base de datos**: un evento y sus reservas son entidades con una estructura relativamente simple y estable, sin necesidad de transacciones multi-tabla complejas ni de integridad referencial estricta entre múltiples entidades relacionadas; el modelo documental de MongoDB permite iterar rápido sobre el esquema de `Event`/`RSVP` (agregar/quitar campos) sin migraciones formales, y su escalabilidad horizontal nativa es apropiada para un servicio que puede recibir picos de tráfico de RSVP en eventos populares.
- **Spring Data MongoDB (repositorios declarativos)** en lugar de acceso manual: minimiza el código boilerplate de consultas simples (`findByCategory`, `findByUserIdAndEventId`) mediante *query derivation*, manteniendo el código de acceso a datos legible y fácil de mantener.
- **Arquitectura en capas (controller/service/repository/model/dto)**: separa responsabilidades y facilita las pruebas unitarias aisladas (mockeando repositorios para probar el servicio, mockeando el servicio para probar el controlador), además de hacer explícito dónde debe vivir cada tipo de lógica (validación HTTP vs. reglas de negocio vs. persistencia), lo que reduce el acoplamiento y mejora la mantenibilidad a medida que el microservicio crece.
- **DTOs separados de las entidades de persistencia** (`CreateEventRequest`, `RSVPRequest`, `RSVPResponse`): evita exponer directamente la entidad `Event`/`RSVP` en el contrato HTTP, previene vulnerabilidades de *mass assignment* (un cliente no puede, por ejemplo, enviar un `status` o `availableCapacity` arbitrarios al crear un evento) y permite que el contrato de la API evolucione independientemente del modelo de persistencia interno.
- **Excepciones de negocio tipadas + `@RestControllerAdvice` centralizado**: hace explícitas y auto-documentadas las reglas de negocio críticas (evento inactivo, cupo agotado, RSVP inexistente) como clases de primera clase en el código, en vez de condicionales dispersos, y centraliza la política de traducción a HTTP en un único punto, favoreciendo la consistencia de la API ante nuevos endpoints futuros.
- **Lombok**: reduce drásticamente el código repetitivo en las entidades (`Event`, `RSVP`) y DTOs, mejorando la legibilidad del código de dominio sin sacrificar funcionalidad (getters/setters/equals/hashCode siguen existiendo, solo se generan en tiempo de compilación).
- **springdoc-openapi (Swagger)**: en un ecosistema de microservicios, la documentación de la API debe mantenerse sincronizada con el código sin esfuerzo manual adicional; generar la especificación OpenAPI directamente desde las anotaciones del controlador garantiza que la documentación nunca queda desactualizada respecto al comportamiento real del servicio, y facilita la integración con otros equipos/servicios consumidores.
- **Docker con build multi-etapa**: produce una imagen final mínima (solo JRE + JAR), reduciendo tanto el tamaño de la imagen (tiempos de despliegue más rápidos) como la superficie de ataque (sin herramientas de build ni código fuente en el contenedor de producción), alineado con buenas prácticas de contenerización para microservicios en producción.
- **JUnit 5 + Mockito + AssertJ para pruebas unitarias, con umbral de cobertura JaCoCo**: garantizar automáticamente en el build (fase `verify`) un piso de cobertura sobre la lógica de negocio real (excluyendo código generado/boilerplate como `model`, `dto`, `repository`, `config`) da confianza para refactorizar y evolucionar las reglas de negocio del microservicio sin introducir regresiones silenciosas.
- **Maven + Maven Wrapper**: Maven es el estándar de facto para proyectos Spring Boot, con un modelo declarativo de dependencias y ciclo de vida de build bien conocido; el *wrapper* asegura reproducibilidad exacta de la versión de build tanto en máquinas de desarrollo como en el pipeline de CI/CD y dentro del propio `Dockerfile`.

---

## 17. Resumen del catálogo de clases (índice rápido)

| # | Clase / Interfaz | Paquete | Tipo |
|---|---|---|---|
| 1 | `EventServiceApplication` | raíz | Clase principal (`@SpringBootApplication`) |
| 2 | `MongoConfig` | `config` | Configuración (`@Configuration`) |
| 3 | `OpenApiConfig` | `config` | Configuración (`@Configuration`) |
| 4 | `EventController` | `controller` | Controlador REST (`@RestController`) |
| 5 | `EventService` | `service` | Servicio de negocio (`@Service`) |
| 6 | `EventRepository` | `repository` | Repositorio Spring Data MongoDB (interfaz) |
| 7 | `RSVPRepository` | `repository` | Repositorio Spring Data MongoDB (interfaz) |
| 8 | `Event` | `model` | Entidad/documento MongoDB |
| 9 | `RSVP` | `model` | Entidad/documento MongoDB |
| 10 | `CreateEventRequest` | `dto` | DTO de entrada |
| 11 | `RSVPRequest` | `dto` | DTO de entrada |
| 12 | `RSVPResponse` | `dto` | DTO de salida |
| 13 | `EventNotActiveException` | `exception` | Excepción de negocio (E1) |
| 14 | `CapacityExhaustedException` | `exception` | Excepción de negocio (E2) |
| 15 | `RsvpNotFoundException` | `exception` | Excepción de negocio (E3) |
| 16 | `GlobalExceptionHandler` | `exception` | Manejador global (`@RestControllerAdvice`) |
| 17 | `EventServiceApplicationTests` | test (raíz) | Suite de pruebas unitarias de `EventService` |
| 18 | `EventControllerAndExceptionTest` | test (raíz) | Suite de pruebas unitarias de `EventController` y `GlobalExceptionHandler` |

**Total: 16 clases/interfaces de producción + 2 clases de prueba = 18 tipos documentados**, cubriendo el 100% de los archivos `.java` presentes en el repositorio.
