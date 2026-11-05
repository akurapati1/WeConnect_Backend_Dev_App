# WeConnect — Backend (LinkedIn-style networking platform)

The backend for **WeConnect**, a professional-networking platform. The repository is organised as a set of Spring Boot microservices; the first service, **`user-service`**, is implemented end to end: member profiles, search, and a LinkedIn-style **connection workflow** (request → accept / reject → remove).

| Service | Status | Responsibility |
|---|---|---|
| `user-service` | ✅ implemented | Profiles, search, connections |
| auth / gateway | planned | Authentication (JWT) and identity propagation |
| posts / feed | planned | Posts, reactions, news feed |
| notifications | planned | Connection and activity notifications |

## user-service

**Stack:** Java 21 · Spring Boot 3.4 · Spring Data JPA · Bean Validation · PostgreSQL (H2 for local/test) · springdoc OpenAPI · JUnit 5 + MockMvc

### Features
- Register members (BCrypt-hashed passwords, unique case-insensitive e-mail, validated input)
- Read, partially update (`PATCH`) and delete profiles; paginated name/headline search
- Connection requests with business rules: no self-connections, no duplicates in either direction, only the receiver may accept/reject, only participants may remove
- RFC 7807 `application/problem+json` error responses (400 / 403 / 404 / 409)

### API
| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/users` | Register |
| GET | `/api/v1/users/{id}` | Get a profile |
| GET | `/api/v1/users?q=&page=&size=` | Search members (page size ≤ 100) |
| PATCH | `/api/v1/users/{id}` | Update profile fields |
| DELETE | `/api/v1/users/{id}` | Delete account |
| POST | `/api/v1/connections` | Send request `{"targetUserId": 2}` |
| POST | `/api/v1/connections/{id}/accept` · `/reject` | Respond (receiver only) |
| DELETE | `/api/v1/connections/{id}` | Remove (participants only) |
| GET | `/api/v1/users/{id}/connections` | Accepted connections |
| GET | `/api/v1/users/{id}/connections/pending` | Incoming pending requests |
| GET | `/api/v1/users/{id}/connections/count` | Connection count |

Endpoints that act on behalf of a user read the caller from the **`X-User-Id`** header. This is a stand-in for identity injected by the planned gateway/auth service — **do not expose `user-service` directly to the internet.**

Swagger UI: `http://localhost:8080/swagger-ui.html` · Health: `/actuator/health`

### Run locally
```bash
cd user-service
./mvnw spring-boot:run                      # in-memory H2, zero setup
```
With PostgreSQL:
```bash
docker compose up -d                         # from the repository root
DB_PASSWORD=postgres SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

### Example
```bash
curl -s -X POST localhost:8080/api/v1/users -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"correct-horse","firstName":"Ada","lastName":"Lovelace","headline":"Mathematician"}'
curl -s -X POST localhost:8080/api/v1/connections -H 'X-User-Id: 1' -H 'Content-Type: application/json' -d '{"targetUserId":2}'
```

### Tests
```bash
./mvnw verify      # integration tests run against in-memory H2
```

### Configuration
| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | – | `postgres` for PostgreSQL |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | local Postgres | `postgres` profile only |
| `DDL_AUTO` | `update` | Hibernate schema strategy (`postgres` profile) |

## License
Apache 2.0 — see [LICENSE](LICENSE).
