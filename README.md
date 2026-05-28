# Zenora

Aplikasi keuangan pribadi berbasis JavaFX 21 + Spring Boot 3 + H2 + JPA + Spring Security.

## Menjalankan

```bash
# Jalankan via Maven (Spring Boot + JavaFX)
mvn clean javafx:run

# atau build jar lalu run
mvn clean package
java -jar target/zenora-1.0.0.jar
```

REST API: `http://localhost:8080`
H2 Console (dev only): `http://localhost:8080/h2-console`

## Default user (profil dev)

| username | password  | role       |
|----------|-----------|------------|
| admin    | admin123  | ROLE_ADMIN |
| user     | user123   | ROLE_USER  |

> Akun default hanya dibuat saat profil aktif bukan `prod`.

## Profil

```bash
# Development (default)
mvn javafx:run

# Production
java -Dspring.profiles.active=prod -jar target/zenora-1.0.0.jar
```

## Variabel environment (prod)

- `PORT` — port server (default 8080)
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `DATABASE_DRIVER`
- `ZENORA_API_BASE` — base URL API untuk klien JavaFX
