# Backend Java — Explicación del proyecto

Este README explica cómo está organizado el backend Java, las capas principales (Servlets/Handlers, Services, DAOs, DB) y describe en detalle la clase `BannerServlet` con su código y la explicación de cada parte.

**Resumen rápido**
- API HTTP disponible en dos variantes: servlets (para contenedores como Tomcat) y handlers embebidos (para ejecución con `LauncherMain`).
- Capas: Controller (Servlet/Handler) → Service (lógica de negocio) → DAO (acceso a BD) → `DBConnection` (conexión y fallback H2).

## Estructura y responsabilidades

- `servlets/`: clases que extienden `HttpServlet` y se despliegan en un contenedor servlet. Ej: `CompraServlet`, `BannerServlet`.
- `http/`: handlers para `com.sun.net.httpserver.HttpServer` usados por `LauncherMain` para ejecutar la API embebida.
- `services/`: lógica de negocio reutilizable y testeable (ej: `PurchaseService`).
- `dao/`: acceso a la base de datos con `JDBC` (ej: `JdbcVideojuegoDAO`).
- `db/DBConnection.java`: centraliza la conexión; lee variables de entorno (`DB_URL`, `DB_USER`, `DB_PASS`) y en caso de error cae a una base H2 en memoria y crea un esquema mínimo.

## Conceptos clave para explicar

- Ciclo de vida de un servlet: `init()` → `service()` → `doGet/doPost/...` → `destroy()`.
- Separación de responsabilidades: el servlet orquesta la petición; el service aplica reglas; el DAO ejecuta SQL.
- Transacciones: uso de `conn.setAutoCommit(false)`, `FOR UPDATE` y `conn.commit()`/`rollback()` para mantener consistencia.
- Seguridad básica: `PreparedStatement` para evitar inyección SQL.
- CORS & JSON: `BaseServlet` centraliza `setCors()` y `writeJson()` para respuesta JSON y permitir consumo desde frontend.

## Clase: BannerServlet (código y explicación)

A continuación está la implementación del servlet `BannerServlet` tal como la proveíste, seguida de una explicación detallada por bloques.

```java
@WebServlet(name = "BannerServlet", urlPatterns = {"/api/banner/*"})
public class BannerServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            var rs = conn.createStatement().executeQuery("SELECT id, url_imagen, fecha_inicio, fecha_fin FROM Banner");
            java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
            while (rs.next()){
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("id", rs.getInt("id")); m.put("url_imagen", rs.getString("url_imagen")); m.put("fecha_inicio", rs.getTimestamp("fecha_inicio")); m.put("fecha_fin", rs.getTimestamp("fecha_fin"));
                list.add(m);
            }
            writeJson(resp, list);
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            String url = (String) body.get("url_imagen"); String inicio = (String) body.getOrDefault("fecha_inicio", null); String fin = (String) body.getOrDefault("fecha_fin", null);
            var ps = conn.prepareStatement("INSERT INTO Banner (url_imagen, fecha_inicio, fecha_fin) VALUES (?,?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, url);
            if (inicio!=null) ps.setString(2,inicio); else ps.setNull(2, java.sql.Types.VARCHAR);
            if (fin!=null) ps.setString(3,fin); else ps.setNull(3, java.sql.Types.VARCHAR);
            ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
            writeJson(resp, java.util.Map.of("id", id));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}
```

### Explicación detallada (línea a línea — por bloques)

- `@WebServlet(name = "BannerServlet", urlPatterns = {"/api/banner/*"})`:
  - Mapea este `HttpServlet` a rutas que empiecen con `/api/banner`. En un contenedor servlet (Tomcat) estas peticiones se enrutarán a esta clase.

- `public class BannerServlet extends BaseServlet`:
  - Hereda `BaseServlet` que ya contiene utilidades: `gson`, `setCors(...)`, `writeJson(...)` y `parseId(...)`. Evita repetir código.

- `doGet(...)`:
  - `setCors(resp);` — configura cabeceras CORS y `Content-Type` para permitir peticiones desde el frontend.
  - `try (Connection conn = DBConnection.getConnection()) { ... }` — abre conexión JDBC y garantiza `close()` automático.
  - `conn.createStatement().executeQuery("SELECT id, url_imagen, fecha_inicio, fecha_fin FROM Banner")` — obtiene un `ResultSet` con filas de la tabla `Banner`.
  - Iteración `while (rs.next())` — por cada fila construye un `Map<String,Object>` con las columnas: `id`, `url_imagen`, `fecha_inicio`, `fecha_fin`.
  - `writeJson(resp, list);` — serializa la lista a JSON y la escribe en la respuesta.
  - `catch (Exception e)` — en error responde 500 con `{ "error": "mensaje" }`.

- `doPost(...)`:
  - `var body = gson.fromJson(req.getReader(), java.util.Map.class);` — parsea el JSON del cuerpo en un `Map` genérico.
  - Extrae `url_imagen`, `fecha_inicio` y `fecha_fin` (si existen) del `body`.
  - Prepara `PreparedStatement` para insertar la fila en `Banner` y solicita `RETURN_GENERATED_KEYS` para obtener el id insertado.
  - `ps.setString(1, url);` — asigna la URL a la primera posición.
  - Para `fecha_inicio`/`fecha_fin` usa `ps.setString(...)` si el valor existe, o `ps.setNull(..., VARCHAR)` si es `null`. Esto evita excepciones por tipos SQL/NULL.
  - `ps.executeUpdate()` — ejecuta la inserción. Luego `ps.getGeneratedKeys()` para recuperar la clave primaria generada.
  - Responde con JSON `{ "id": <generatedId> }`.

### Buenas prácticas y observaciones sobre este servlet

- Uso de `PreparedStatement`: evita inyección SQL y maneja tipos correctamente.
- Manejo de recursos mediante try-with-resources asegura que `Connection` y `ResultSet` se cierran.
- Validaciones mínimas: actualmente el servlet no valida formato de `url_imagen` ni validación de fechas; puedes agregar validaciones antes del `INSERT`.
- Formato de fechas: aquí se insertan como `String` en la DB; lo ideal sería parsear y usar `java.sql.Timestamp`/`Date` si la columna es de tipo fecha/ts.
- Manejo de errores: devolver un mensaje de error simple está bien para desarrollo; en producción conviene ocultar detalles sensibles.

## Ejecución y despliegue

- Despliegue en Tomcat: empaqueta como WAR y coloca en `webapps/`. Los servlets mapeados con `@WebServlet` funcionarán.
- Ejecución local (servidor embebido): ejecuta `LauncherMain` que registra handlers en `http/*` y expone la API en `http://localhost:8080/backend/api/`.

Comandos útiles (PowerShell):

```powershell
cd "C:\Users\PC\Desktop\El final\Proyecto-Tienda-Videojuegos\backend-java"
mvn package
# Para ejecutar con servidor embebido (si está compilado en clases y dependencias):
java -cp target/classes;target/dependency/* com.example.backend.LauncherMain
```

## Recomendaciones para la presentación

- Muestra el flujo completo para `GET /api/banner` y `POST /api/banner` (petición → servlet → DB → respuesta).
- Explica por qué se utilizan `PreparedStatement` y `try-with-resources`.
- Señala limitaciones actuales (validación de entrada, manejo más fino de fechas) y propuestas de mejora.

---

Si querés, puedo:

- Generar un `README` adicional en la raíz con pasos de despliegue completos.
- Agregar validaciones y pruebas unitarias para `BannerServlet`.
- Convertir las inserciones de fecha para usar `java.sql.Timestamp` en lugar de `String`.

Archivo creado: [backend-java/README.md](backend-java/README.md)
