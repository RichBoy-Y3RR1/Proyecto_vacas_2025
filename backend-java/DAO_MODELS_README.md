# DAO y Models — Explicación extendida

Este documento explica en profundidad qué son los *models* (modelos) y los *DAOs* (Data Access Objects), cómo funcionan internamente en un proyecto Java JDBC, y qué necesitas saber para explicarlo en una presentación o para mantener/extendender el código.

## Resumen breve

- **Modelos (Models):** clases POJO que representan entidades del dominio (por ejemplo `Videojuego`, `Usuario`, `Empresa`). Contienen campos, constructores, getters/setters y validaciones simples.
- **DAOs:** responsables de toda la interacción con la base de datos: abrir conexiones, ejecutar SQL, mapear `ResultSet` a objetos y persistir cambios. Proveen una API (métodos CRUD) que el resto de la aplicación consume.

## Estructura típica de un Model

- Campos privados y tipos adecuados (ej.: `BigDecimal` para precios, `Date`/`Timestamp` o `java.time` para fechas).
- Métodos: getters, setters, `toString()`, `equals()` y `hashCode()` si se necesita comparar o usar en colecciones.
- Validaciones dentro del modelo solo si son reglas del dominio (pocas); normalmente la validación de entrada se hace antes de persistir.

Ejemplo de archivo en el proyecto: [backend-java/src/main/java/com/example/backend/models/Videojuego.java](backend-java/src/main/java/com/example/backend/models/Videojuego.java)

## Interfaz DAO vs Implementación

- Definir una **interfaz** (`VideojuegoDAO`) con métodos como:
  - `List<Videojuego> listAll()`
  - `Integer create(Videojuego v)`
  - `boolean update(Integer id, Videojuego v)`
  - `Optional<Videojuego> findById(Integer id)`
- Implementar la interfaz con JDBC en `JdbcVideojuegoDAO` (o con JPA/Hibernate si se usa ORM).

Archivo ejemplo en el repo: [backend-java/src/main/java/com/example/backend/dao/JdbcVideojuegoDAO.java](backend-java/src/main/java/com/example/backend/dao/JdbcVideojuegoDAO.java)

## Paso a paso: cómo implementa JDBC un `Jdbc*DAO`

1. **Obtener conexión**
   - `try (Connection conn = DBConnection.getConnection()) { ... }`
   - `DBConnection` centraliza URL/usuario/contraseña y provee fallback a H2 para desarrollo: [backend-java/src/main/java/com/example/backend/db/DBConnection.java](backend-java/src/main/java/com/example/backend/db/DBConnection.java)

2. **Preparar la consulta**
   - Usar `PreparedStatement` con placeholders `?` para proporcionar parámetros.
   - Ejemplo: `conn.prepareStatement("INSERT INTO Videojuego (...) VALUES (?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);`

3. **Asignar parámetros**
   - `ps.setString(1, data.getNombre());` `ps.setBigDecimal(3, data.getPrecio());` etc.
   - Para `NULL` usar `ps.setNull(index, java.sql.Types.VARCHAR)`.

4. **Ejecutar y mapear el resultado**
   - `ResultSet rs = ps.executeQuery(); while (rs.next()) { model.setNombre(rs.getString("nombre")); ... }`

5. **Obtener claves generadas**
   - Para inserts: `ps.executeUpdate(); ResultSet keys = ps.getGeneratedKeys(); if (keys.next()) id = keys.getInt(1);`

6. **Cerrar recursos**
   - Siempre usar try-with-resources para `Connection`, `PreparedStatement` y `ResultSet`.

## Transacciones, bloqueo y concurrencia

- Si la operación involucra varios pasos que deben ser atómicos (por ejemplo, insertar una compra y descontar saldo), debes:
  - `conn.setAutoCommit(false);`
  - Usar `SELECT ... FOR UPDATE` al leer filas que luego se actualizarán (evita race conditions entre hilos/requests).
  - `conn.commit()` al final; `conn.rollback()` en caso de error.
- **Importante:** usa la misma `Connection` para toda la transacción. No mezcles conexiones.

Ejemplo aplicado en el proyecto: flujo de compra en `CompraServlet` / `CompraHandler` (usa `FOR UPDATE`, `setAutoCommit(false)`, commit/rollback).

## Mapeo de tipos SQL ↔ Java y manejo de NULL

- `INT` ↔ `int` / `Integer` (`rs.getInt("col")`)
- `DECIMAL` ↔ `BigDecimal` (`rs.getBigDecimal("col")`)
- `VARCHAR/TEXT` ↔ `String`
- `DATE/TIMESTAMP` ↔ `java.sql.Date` / `java.sql.Timestamp` o `java.time.LocalDate/LocalDateTime` con conversión
- Para `NULL`: comprobar `rs.wasNull()` o usar `getObject()` y aceptar `null`.

## Buenas prácticas (lista de control)

- Usar `PreparedStatement` siempre para parámetros.
- Usar try-with-resources para cerrar `Connection`, `PreparedStatement` y `ResultSet`.
- Definir interfaces DAO y usar inyección (constructor o framework) para facilitar tests.
- Usar pool de conexiones (HikariCP recommended) en vez de crear conexiones con `DriverManager` por cada operación.
- Manejar transacciones explícitas cuando la operación involucre múltiples escrituras.
- Convertir y validar fechas correctamente (usar `Timestamp` en lugar de Strings si la columna es temporal).
- Escribir tests de integración con H2 o Testcontainers para las consultas DAO.

## Testing de DAOs

- Tests de integración con H2 (en memoria): crear esquema, insertar datos de prueba y ejecutar métodos DAO.
- Para CI más realista usar Testcontainers con una imagen de MySQL.

## Migración a tecnologías más avanzadas

- Si el proyecto crece, considera migrar DAOs a:
  - **ORM**: JPA/Hibernate (menos boilerplate, manejo de relaciones, caché)
  - **MyBatis**: mapeo SQL-to-objects con más control SQL
  - **Spring Data**: repositorios con poca implementación manual

## Ejemplos y referencias en este repositorio

- `JdbcVideojuegoDAO`: [backend-java/src/main/java/com/example/backend/dao/JdbcVideojuegoDAO.java](backend-java/src/main/java/com/example/backend/dao/JdbcVideojuegoDAO.java)
- `DBConnection` con fallback H2: [backend-java/src/main/java/com/example/backend/db/DBConnection.java](backend-java/src/main/java/com/example/backend/db/DBConnection.java)
- Flujo de compra que muestra transacciones: [backend-java/src/main/java/com/example/backend/servlets/CompraServlet.java](backend-java/src/main/java/com/example/backend/servlets/CompraServlet.java)

---

## Código de ejemplo: `JdbcVideojuegoDAO` (incluir y explicar)

A continuación se inserta el código completo de `JdbcVideojuegoDAO` tal como aparece en el proyecto, seguido por una explicación línea a línea y por método.

```java
package com.example.backend.dao;

import com.example.backend.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import com.example.backend.models.Videojuego;

public class JdbcVideojuegoDAO implements VideojuegoDAO {
   @Override
   public List<Videojuego> listAll() throws Exception {
      List<Videojuego> list = new ArrayList<>();
      try (Connection conn = DBConnection.getConnection()){
         ResultSet rs = conn.createStatement().executeQuery("SELECT v.id, v.nombre, v.descripcion, v.precio, v.estado, e.nombre AS empresa, v.edad_clasificacion FROM Videojuego v JOIN Empresa e ON v.empresa_id = e.id");
         while (rs.next()){
            Videojuego v = new Videojuego();
            v.setId(rs.getInt("id"));
            v.setNombre(rs.getString("nombre"));
            v.setDescripcion(rs.getString("descripcion"));
            v.setPrecio(rs.getBigDecimal("precio"));
            v.setEstado(rs.getString("estado"));
            v.setEmpresa(rs.getString("empresa"));
            v.setEdad_clasificacion(rs.getString("edad_clasificacion"));
            list.add(v);
         }
      }
      return list;
   }

   @Override
   public Integer create(com.example.backend.models.Videojuego data) throws Exception {
      try (Connection conn = DBConnection.getConnection()){
         PreparedStatement ps = conn.prepareStatement("INSERT INTO Videojuego (nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion) VALUES (?,?,?,?,?,NULL,?)", PreparedStatement.RETURN_GENERATED_KEYS);
         ps.setString(1, data.getNombre());
         ps.setString(2, data.getDescripcion());
         // use default empresa_id = 1 when none provided
         ps.setInt(3, 1);
         ps.setBigDecimal(4, data.getPrecio() != null ? data.getPrecio() : new java.math.BigDecimal("0"));
         ps.setString(5, "PUBLICADO");
         ps.setString(6, data.getEdad_clasificacion());
         ps.executeUpdate(); ResultSet rs = ps.getGeneratedKeys(); if (rs.next()) return rs.getInt(1); return null;
      }
   }

   @Override
   public boolean update(Integer id, com.example.backend.models.Videojuego data) throws Exception {
      try (Connection conn = DBConnection.getConnection()){
         PreparedStatement ps = conn.prepareStatement("UPDATE Videojuego SET nombre=?, descripcion=?, precio=?, edad_clasificacion=? WHERE id=?");
         ps.setString(1, data.getNombre());
         ps.setString(2, data.getDescripcion());
         ps.setBigDecimal(3, data.getPrecio() != null ? data.getPrecio() : new java.math.BigDecimal("0"));
         ps.setString(4, data.getEdad_clasificacion());
         ps.setInt(5, id);
         return ps.executeUpdate() > 0;
      }
   }

   @Override
   public boolean setForSale(Integer id, boolean forSale) throws Exception {
      try (Connection conn = DBConnection.getConnection()){
         PreparedStatement ps = conn.prepareStatement("UPDATE Videojuego SET estado = ? WHERE id = ?");
         ps.setString(1, forSale ? "PUBLICADO" : "SUSPENDIDO"); ps.setInt(2, id);
         return ps.executeUpdate() > 0;
      }
   }
}
```

### Explicación detallada

General:
- Esta clase implementa la interfaz `VideojuegoDAO` y contiene la implementación JDBC.
- Cada método abre una `Connection` mediante `DBConnection.getConnection()` y usa try-with-resources para asegurar el cierre.
- Se usan `PreparedStatement` para las operaciones con parámetros (INSERT/UPDATE) y `Statement` simple para consultas sin parámetros.

Método `listAll()`:
- Objetivo: devolver una `List<Videojuego>` con todos los videojuegos publicados y su empresa asociada.
- Pasos:
  1. Crea una lista vacía `list`.
  2. Abre conexión: `try (Connection conn = DBConnection.getConnection())`.
  3. Ejecuta la consulta SQL que hace `JOIN` con `Empresa` para obtener el nombre de la empresa.
  4. Itera el `ResultSet` con `while (rs.next())` y por cada fila:
    - Crea `Videojuego v = new Videojuego()` y asigna campos con los getters de `ResultSet` (`getInt`, `getString`, `getBigDecimal`).
    - Añade `v` a la lista.
  5. Retorna la lista.

Consideraciones:
- Usar `Statement` para consultas simples está bien, pero si la consulta fuera dinámica o recibiera parámetros, se debería usar `PreparedStatement`.

Método `create(Videojuego data)`:
- Objetivo: insertar un nuevo videojuego y devolver el `id` generado.
- Pasos:
  1. Abre conexión en try-with-resources.
  2. Prepara un `PreparedStatement` con `RETURN_GENERATED_KEYS` para recuperar la PK.
  3. Asigna parámetros: nombre, descripción, `empresa_id` (por defecto `1` si no se proporciona), precio (se asegura `BigDecimal` no nulo), estado (se fija `PUBLICADO`) y clasificación de edad.
  4. Ejecuta `ps.executeUpdate()`.
  5. Recupera `ResultSet rs = ps.getGeneratedKeys()` y si hay fila retorna la clave generada.

Consideraciones:
- Se fuerza `empresa_id = 1` como valor por defecto; si en el futuro necesitas usar el `empresa_id` real del modelo, cambia el `ps.setInt(3, ...)` por `data.getEmpresaId()`.
- Se trata el `precio` nulo convirtiéndolo a `0` para evitar errores.

Método `update(Integer id, Videojuego data)`:
- Objetivo: actualizar campos editables de un videojuego existente.
- Pasos:
  1. Abre conexión y prepara `UPDATE Videojuego SET nombre=?, descripcion=?, precio=?, edad_clasificacion=? WHERE id=?`.
  2. Asigna parámetros desde `data` y el `id` al final.
  3. Ejecuta `ps.executeUpdate()` y retorna `true` si filas afectadas > 0.

Consideraciones:
- No actualiza `empresa_id`, `fecha_lanzamiento` ni `estado` en este método; si necesitas esos cambios añade más parámetros.

Método `setForSale(Integer id, boolean forSale)`:
- Objetivo: cambiar el estado del videojuego entre `PUBLICADO` y `SUSPENDIDO`.
- Implementación:
  - Prepara `UPDATE Videojuego SET estado = ? WHERE id = ?` y asigna `PUBLICADO` si `forSale` es `true` o `SUSPENDIDO` si es `false`.
  - Ejecuta y devuelve `true` si se actualizó al menos una fila.

Consideraciones generales y mejoras propuestas:
- Validaciones de entrada: los métodos asumen que los datos son correctos. Se recomienda validar `data` antes de llamar al DAO.
- Manejo de transacciones: estos métodos usan cada uno su propia conexión/autocommit. Si necesitas operaciones compuestas (ej.: insertar videojuego y asignar recursos relacionados), coordina transacción desde la capa servicio y pásale la `Connection` o agrupa en la misma transacción.
- Manejo de excepciones: los métodos lanzan `Exception`. Para producción es recomendable lanzar excepciones más específicas o envolver `SQLException` en una excepción de repositorio personalizada.
- Pool de conexiones: mejora el rendimiento usando HikariCP en `DBConnection`.

Con esto, el `JdbcVideojuegoDAO` queda incluido y explicado en este README. Si querés, puedo:
- añadir ejemplos de llamadas desde un `Service` o `Servlet`,
- crear tests de integración para estos métodos usando H2,
- refactorizar `create` para aceptar `empresa_id` desde el modelo.


---

Si querés, puedo generar:
- un ejemplo de test de integración para `JdbcVideojuegoDAO` usando H2, o
- un diagrama de secuencia textual que muestre la interacción `Controller -> Service -> DAO -> DB`.
