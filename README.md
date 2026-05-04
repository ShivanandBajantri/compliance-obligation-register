# Compliance Obligation Register

A compliance management system built with Spring Boot, React, and a Python AI service.

## Architecture

| Layer | Technology |
|-------|-----------|
| Backend API | Spring Boot 3.2, JPA/Hibernate, Flyway |
| Database | PostgreSQL 15 (H2 for tests) |
| Cache | Redis 7 |
| Frontend | React 18, served via Nginx |
| AI Service | Python 3.11 / Flask |
| Auth | JWT (HMAC-SHA256, 24 h expiry) |
| Containerisation | Docker Compose |

---

## Quick Start (Docker Compose)

### Prerequisites
- Docker Desktop with at least 4 GB RAM allocated
- Ports 80, 8080, 5000, 5432, 6379 free

### 1 — Configure environment
```bash
cp .env.example .env
# Edit .env — set JWT_SECRET, email credentials, admin email
```

### 2 — Build and start
```bash
docker-compose up --build
```

### 3 — Access
| Service | URL |
|---------|-----|
| Frontend | http://localhost |
| Backend API | http://localhost:8080 |
| AI Service | http://localhost:5000/health |
| Health check | http://localhost:8080/actuator/health |

### Default credentials (seeded by V7 migration)
| Username | Password | Role |
|----------|----------|------|
| admin | password | ADMIN |
| manager | password | MANAGER |
| viewer | password | VIEWER |

> **Change these passwords before any real deployment.**

---

## API Reference

### Authentication
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | None | Returns JWT |
| POST | `/api/auth/refresh` | Bearer token | Issues new token |

### Compliance Obligations
| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/obligations/all` | Any | Paginated list (full entity) |
| GET | `/api/obligations/all-dto` | Any | Paginated list (DTO, preferred) |
| GET | `/api/obligations/{id}` | Any | Single obligation |
| GET | `/api/obligations/status?status=` | Any | Filter by status |
| GET | `/api/obligations/search?keyword=` | Any | Full-text search (paginated) |
| GET | `/api/obligations/stats` | Any | Dashboard aggregation |
| GET | `/api/obligations/export` | Any | CSV download |
| POST | `/api/obligations` | ADMIN, MANAGER | Create |
| PUT | `/api/obligations/{id}` | ADMIN, MANAGER | Update |
| DELETE | `/api/obligations/{id}` | ADMIN | Delete |

### AI Service
| Method | Path | Description |
|--------|------|-------------|
| POST | `/ai/analyze` | Risk analysis for an obligation |
| POST | `/ai/predict` | Deadline prediction |

---

## Development Setup (without Docker)

```bash
# 1. Copy and configure properties
cp backend/src/main/resources/application.properties.template \
   backend/src/main/resources/application.properties

# 2. Run backend (uses H2 in-memory DB by default)
cd backend && mvn spring-boot:run

# 3. Run frontend dev server
cd frontend && npm install && npm start
```

---

## Running Tests

```bash
cd backend && mvn clean test
# 20 tests pass; IntegrationTest is skipped without Docker
```

---

## Docker Compose Commands

```bash
# Start all services (foreground)
docker-compose up --build

# Start in background
docker-compose up -d --build

# View logs
docker-compose logs -f backend

# Stop everything
docker-compose down

# Full reset (removes volumes / database)
docker-compose down -v
```

---

## Known Issues (P3 — Minor, non-blocking)

These issues are documented and do not affect core functionality for the demo.

### P3-001 — Self-service registration not implemented
`POST /api/auth/register` returns **501 Not Implemented**.  
Users must be created directly in the database or via a future admin UI.  
**Workaround:** Use the three seeded accounts (admin / manager / viewer).

### P3-002 — Role assignment API not implemented
`POST /api/auth/assign-role` returns **501 Not Implemented**.  
Roles are managed via the `user_roles` table in the database.

### P3-003 — JWT stored in localStorage (XSS risk)
The React frontend stores the JWT in `localStorage`, which is accessible to JavaScript.  
**Risk:** Low for an internal demo tool; would need `httpOnly` cookie storage for production.

### P3-004 — No HTTPS in Docker Compose
The Nginx container listens on HTTP port 80 only.  
**Workaround for production:** Terminate TLS at a load balancer or add a Certbot sidecar.

### P3-005 — Scheduler race condition in clustered deployments
`AlertScheduler.checkObligations()` queries obligations with `alertSent = false`, sends emails, then marks them sent. In a multi-instance deployment, two instances could process the same obligation simultaneously.  
**Impact:** Duplicate alert emails. Not relevant for single-instance demo.  
**Fix path:** Add `SELECT ... FOR UPDATE` or a distributed lock (Redisson).

### P3-006 — Search uses LIKE (not full-text index)
`/api/obligations/search?keyword=` uses `LOWER(col) LIKE '%keyword%'` which cannot use a B-tree index.  
**Impact:** Slow on very large datasets (10 000+ rows).  
**Fix path:** Add `pg_trgm` GIN index or integrate Elasticsearch.

### P3-007 — Weekly summary email hardcoded recipient fallback
If `APP_ADMIN_EMAIL` env var is not set, the weekly summary goes to `admin@company.com`.  
**Workaround:** Set `APP_ADMIN_EMAIL` in `.env`.

### P3-008 — CSV export has no row limit
The export endpoint streams all rows in pages of 500. For very large datasets this could be slow.  
**Fix path:** Add a `?maxRows=` parameter or a server-side limit.

---

## Security Notes

- Never commit `.env` or `application.properties` to version control (both are in `.gitignore`)
- Set `JWT_SECRET` to a random 64-character string before any real deployment (`openssl rand -hex 32`)
- Change default user passwords immediately after first login in any non-demo environment
- The `application-docker.properties` profile uses `ddl-auto=validate` — Flyway owns all schema changes

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Backend exits immediately | Missing env vars in `.env` | Check `docker-compose logs backend` |
| 401 on all API calls | JWT_SECRET mismatch between restarts | Restart all services together |
| Flyway migration fails | DB already has schema from old run | Add `spring.flyway.baseline-on-migrate=true` (already set in docker profile) |
| Port conflict | Another service on 80/8080/5432/6379 | Stop conflicting service or change port mapping in `docker-compose.yml` |
| Email not sent | SMTP credentials wrong | Check `SPRING_MAIL_*` vars in `.env`; errors are logged but non-fatal |
