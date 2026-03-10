# Literalura — Alura Challenge (Consola + Spring Boot + JPA)

Proyecto de consola desarrollado con **Spring Boot** que permite **buscar libros en la API pública de Gutendex (Project Gutenberg)** y **registrarlos en una base de datos PostgreSQL**. Además, permite consultar los libros/autores almacenados y filtrar por idioma o por autores vivos en un año determinado.

La aplicación inicia como un proceso de consola (no expone endpoints HTTP) y muestra un menú interactivo para ejecutar las opciones.

---

## Objetivo del proyecto

Este proyecto fue construido como parte del **Challenge “Literalura” (Alura Latam)** para practicar:

- Consumo de una API REST (Gutendex) usando `HttpClient`.
- Parseo de JSON a objetos Java con **Jackson**.
- Persistencia de datos con **Spring Data JPA**.
- Modelado de entidades y relaciones (Autor–Libro).
- Consultas derivadas y consultas personalizadas con `@Query`.
- Uso de variables de entorno para configuración de base de datos.

---

## ¿Qué hace la aplicación?

Cuando ejecutas la app, se muestra un menú con estas opciones:

1. **Buscar libro por título** (consulta a Gutendex, toma el primer resultado, y lo guarda en la BD si no existe).
2. **Listar libros registrados** (consulta la BD).
3. **Listar autores registrados** (consulta la BD).
4. **Listar autores vivos en un año** (consulta la BD con filtro por fechas de nacimiento/fallecimiento).
5. **Listar libros por idioma** (consulta la BD por idioma, por ejemplo `en`, `es`, `fr`, `pt`).
0. **Salir**.

> Nota: Al buscar un libro, la app guarda **un autor** (el primero de la lista de autores del resultado) y **un idioma** (el primero de la lista de idiomas del resultado), siguiendo la lógica implementada.

---

## Tecnologías utilizadas

- **Java 17**
- **Spring Boot 4.0.3**
- **Spring Data JPA**
- **PostgreSQL** (driver runtime)
- **Jackson Databind** (JSON → objetos)
- **Maven Wrapper** (`mvnw` / `mvnw.cmd`)

---

## Estructura del proyecto

Paquetes principales dentro de `src/main/java/com/aluracursos/literalura/`:

- `LiteraluraApplication`: clase principal Spring Boot que implementa `CommandLineRunner` y lanza el menú.
- `principal/Principal`: contiene el menú de consola y la lógica de cada opción.
- `model/`: entidades JPA (`Autor`, `Libro`) y DTOs/records para mapear la API (`DatosLibro`, `DatosAutor`, `GutendexResponse`).
- `repository/`: repositorios Spring Data JPA (`Repository` para `Libro` y `AutorRep` para `Autor`).
- `service/`: consumo HTTP (`ConsumoAPI`) y conversión JSON (`ConvierteDatos`).

Recursos:

- `src/main/resources/application.properties`: configuración de Spring + datasource.

---

## Modelo de datos (Entidades)

### Autor

Se persiste en la tabla `autores`.

Campos relevantes:

- `id` (autogenerado)
- `nombre` (**único**)
- `fechaNacimiento` (año)
- `fechaFallecimiento` (año, puede ser `null`)
- relación `@OneToMany` con libros

### Libro

Se persiste en la tabla `libros`.

Campos relevantes:

- `id` (autogenerado)
- `titulo`
- `resumen` (hasta 1500 chars)
- `idioma`
- `numeroDescargas`
- relación `@ManyToOne` hacia `Autor`

### Relación

Un **Autor** puede tener muchos **Libros**.
Un **Libro** pertenece a un **Autor**.

---

## Integración con Gutendex (API)

La API base usada es:

```
https://gutendex.com/books/?search=<titulo>
```

Flujo de la búsqueda:

1. El usuario ingresa un título.
2. `ConsumoAPI` construye la URL (reemplaza espacios por `%20`) y hace el `GET`.
3. `ConvierteDatos` usa Jackson (`ObjectMapper`) para convertir el JSON en un `GutendexResponse`.
4. Se toma el **primer** elemento de `results` como libro candidato.
5. Se valida si el libro ya existe en BD por `(titulo, autor.nombre)`.
6. Si el autor no existe, se crea.
7. Se guarda el libro asociado al autor.

---

## Repositorios y consultas

### `Repository` (Libros)

Incluye consultas como:

- `findByTituloAndAutor_Nombre(titulo, nombreAutor)` para evitar duplicados.
- `findByIdiomaIgnoreCase(idioma)` para filtrar por idioma.
- `findDistinctIdiomas()` (consulta `@Query`) para obtener idiomas distintos (si lo necesitas en el futuro).

### `AutorRep` (Autores)

- `findByNombre(nombre)` para reutilizar autor existente.
- `findAutoresVivosEnAnio(anio)` (consulta `@Query`) para obtener autores vivos en un año:
	- Nacimiento `<= anio`
	- y (fallecimiento `IS NULL` o fallecimiento `>= anio`)

---

## Configuración de base de datos

La app usa PostgreSQL y toma los parámetros desde variables de entorno (con valores por defecto). Esto permite correr el proyecto sin modificar el archivo de propiedades, si lo deseas.

Archivo: `src/main/resources/application.properties`

Propiedades principales:

- URL:
	- `spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:literalura}`
- Usuario:
	- `spring.datasource.username=${DB_USER:postgres}`
- Password:
	- `spring.datasource.password=${DB_PASSWORD:postgres}`
- JPA:
	- `spring.jpa.hibernate.ddl-auto=update` (crea/actualiza tablas automáticamente)
	- `spring.jpa.show-sql=true` (muestra SQL en consola)

### Crear la base de datos

Antes de ejecutar, crea una base de datos llamada `literalura` (o cambia `DB_NAME`). Por ejemplo en psql:

```sql
CREATE DATABASE literalura;
```

---

## Cómo ejecutar el proyecto

### Requisitos

- **Java 17** instalado.
- **PostgreSQL** corriendo y accesible.
- Base de datos creada (`literalura` por defecto).

### Ejecutar con Maven Wrapper

En Windows (PowerShell o CMD) desde la raíz del proyecto:

```bash
./mvnw.cmd spring-boot:run
```

O si prefieres empaquetar primero:

```bash
./mvnw.cmd clean package
java -jar target/alura-challenge-literalura-0.0.1-SNAPSHOT.jar
```

### Variables de entorno (opcional)

Si tu PostgreSQL no usa los valores por defecto, puedes configurar:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

Ejemplo (PowerShell):

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "literalura"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "postgres"
./mvnw.cmd spring-boot:run
```

---

## Guía de uso (menú)

Al iniciar, verás un menú similar a:

```
1 - Buscar Libro por su Título
2 - Listar Libros Registrados
3 - Listar Autores Registrados
4 - Listar Autores vivos en un determinado Año
5 - Listar Libros por Idioma
0 - Salir
```

### 1) Buscar libro por título

- Escribe el título o parte del título.
- La app consulta Gutendex y registra el primer resultado.
- Si el libro ya fue guardado previamente (mismo título y mismo autor), lo informa y no duplica.

### 2) Listar libros registrados

- Muestra los libros guardados en la BD.

### 3) Listar autores registrados

- Muestra los autores y también una lista de títulos asociados.

### 4) Autores vivos en un año

- Ingresa un año (por ejemplo `1850`).
- Devuelve autores cuyo rango de vida incluye ese año.

### 5) Listar libros por idioma

- Ingresa el código de idioma (por ejemplo `en`, `es`, `fr`, `pt`).
- Lista los libros que coincidan.

---

## Notas importantes / Limitaciones actuales

- La búsqueda guarda **solo el primer libro** devuelto por Gutendex.
- De ese libro, se guarda **solo el primer autor** y **solo el primer idioma**.
- El resumen puede venir vacío si Gutendex no trae `summaries`.
- El proyecto imprime el JSON completo de la respuesta en consola durante la búsqueda (útil para debug).

---

## Solución de problemas

### Error de conexión a PostgreSQL

- Verifica que PostgreSQL esté corriendo.
- Verifica host/puerto/credenciales.
- Revisa variables `DB_*` y que la base exista.

### Tablas no aparecen

- La app crea/actualiza tablas al iniciar gracias a `spring.jpa.hibernate.ddl-auto=update`.
- Asegúrate de que el usuario tenga permisos de creación.

---

## Autoría

Proyecto realizado como parte del **Challenge Literalura** (Alura Latam).
