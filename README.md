_This project has been created as part of the 42 curriculum by yehara, tmuranak, kofujita, yonuma._

# Online Workspace

## Description

Online Workspace is a web application for people who want to work together online. Users can create and join shared work rooms, see who is currently present, and communicate through real-time chat.

The project focuses on a secure REST API, a WebSocket-based real-time experience, and an observable containerized environment.

### Key features

- User registration, login, logout, session management, and account withdrawal
- Profile editing, avatar upload, user search, and profile visibility
- Room creation, filtering, pagination, joining, leaving, updating, and closing
- Room categories, work styles, capacity limits, and member presence
- Persistent room chat with real-time WebSocket delivery
- Friend management and friend online-presence updates
- CSRF protection, API-key authentication, rate limiting, and security audit logging
- Prometheus metrics, Grafana dashboards, alerting, and ELK-based log collection

## Instructions

### Prerequisites

| Tool | Requirement |
| --- | --- |
| Docker | Docker Engine and Docker Compose |
| Java | JDK 25 for running the backend outside Docker |
| Node.js and npm | Required for frontend tooling outside Docker |
| Gleam | Required for frontend development outside Docker |

The default Docker Compose setup provides the frontend, backend, PostgreSQL database, and reverse proxy.

### Start the development environment

```bash
docker compose up --build
```

Open the application through the development proxy:

- HTTPS application: `https://localhost:8443`
- HTTP redirect endpoint: `http://localhost:8088`
- Backend direct access: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

The proxy uses a local self-signed certificate. Your browser may require an exception for the certificate during development.

### Run the backend directly

Start PostgreSQL first, then run:

```bash
SESSION_COOKIE_SECURE=false ./gradlew bootRun
```

The backend uses these defaults when the corresponding environment variables are not set:

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `password` |
| `MAIL_HOST` | `localhost` |
| `MAIL_PORT` | `1025` |
| `SESSION_COOKIE_HTTP_ONLY` | `true` |
| `SESSION_COOKIE_SECURE` | `true` |
| `SESSION_COOKIE_SAME_SITE` | `lax` |

For local HTTP access, set `SESSION_COOKIE_SECURE=false`. Use `true` when accessing the backend through HTTPS.

### Start the observability stack

The observability services are defined in `compose.observability.yaml`. Set the required variables and run:

```bash
MANAGEMENT_API_KEY="change-me" \
GRAFANA_ADMIN_PASSWORD="change-me" \
GRAFANA_ALERT_DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/..." \
docker compose -f compose.yaml -f compose.observability.yaml up prometheus grafana
```

Available services include:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`

### Test the project

```bash
./gradlew test
```

Validate the REST API contract with:

```bash
npx --yes @redocly/cli@2.43.2 lint online-workspace@v1
```

## Resources

### Project documentation

- [Requirements](docs/requirements.md)
- [API contract](docs/openapi.yaml)
- [API development guide](docs/api_development.md)
- [WebSocket event contract](docs/websocket_events.md)
- [Database schema](docs/db_schema.md)
- [Web security policy](docs/web_security.md)
- [Account withdrawal data policy](docs/account_withdrawal_data_policy.md)
- [Architecture](docs/architecture.md)
- [42 subject](en.subject.pdf)

### External references

- [Example ft_transcendence README](https://github.com/team-cinnamoroll/ft_transcendence)
- [42 Eval Hub: ft_transcendence](https://www.42evalhub.com/common/fttranscendence)
- [Spring Boot documentation](https://spring.io/projects/spring-boot)
- [Gleam documentation](https://gleam.run/documentation/)
- [PostgreSQL documentation](https://www.postgresql.org/docs/)
- [Prometheus documentation](https://prometheus.io/docs/)
- [Grafana documentation](https://grafana.com/docs/)

### AI usage

TODO: describe which AI tools were used, for which tasks, and how the generated output was reviewed and tested by the team.

## Team Information

The project team consists of four members. Roles and responsibilities are intentionally left for the team to complete.

| Member | Role(s) | Responsibilities |
| --- | --- | --- |
| `yehara` | TODO | TODO |
| `tmuranak` | TODO | TODO |
| `kofujita` | TODO | TODO |
| `yonuma` | TODO | TODO |

## Project Management

- Communication channel: TODO
- Recurring meetings: TODO
- Task management tool: GitHub Issues and GitHub Projects
- Code review process: TODO
- Branching and release process: see [branch naming conventions](docs/branch_naming_convention.md)

## Technical Stack

### Frontend

- Gleam
- Lustre
- Tailwind CSS
- JavaScript FFI for browser APIs and SockJS/STOMP communication

Gleam was selected for a typed functional frontend. Lustre provides the application architecture, while Tailwind CSS is used for styling.

### Backend

- Java 25
- Spring Boot 4
- Spring Security
- Spring Web MVC
- Spring WebSocket and STOMP
- Bean Validation
- MyBatis
- Gradle

Spring Boot provides the HTTP and WebSocket servers. Spring Security handles session authentication, CSRF protection, API keys, access control, and security events. MyBatis is used for explicit SQL mapping and database access.

### Database and infrastructure

- PostgreSQL 16
- Flyway
- Docker and Docker Compose
- Nginx
- Prometheus and Grafana
- Elasticsearch, Logstash, and Kibana

Docker Compose keeps the development environment reproducible. Nginx provides the HTTPS reverse proxy and routes browser requests to the frontend and backend.

## Database Schema

The main relationships are:

```text
users
├── user_profiles
├── room_memberships ── rooms ── room_categories
├── chat_messages
├── friendships
└── account status / withdrawal data
```

Important entities include:

- `users`: account identity and authentication-related data
- `user_profiles`: names, biography, work category, visibility, and avatar references
- `rooms`: room settings, capacity, work style, status, and creator
- `room_categories`: administrator-defined room categories
- `room_memberships`: users currently or previously associated with rooms
- `chat_messages`: messages stored for room history
- `friendships`: active and removed friend relationships

The complete schema and constraints are documented in [docs/db_schema.md](docs/db_schema.md), and database changes are applied through Flyway migrations.

## Features List

| Feature | Description | Contributor(s) |
| --- | --- | --- |
| Authentication | Registration, login, logout, sessions, and CSRF token handling | TODO |
| Profile management | Profile visibility, biography, work category, and avatar management | TODO |
| User search | Paginated name-based user search and public profiles | TODO |
| Room management | Create, list, filter, inspect, update, and close rooms | TODO |
| Room membership | Join, leave, member listing, capacity checks, and access control | TODO |
| Chat | Persistent paginated chat history and message validation | TODO |
| Real-time updates | Chat, room presence, member counts, room creation, and friend presence | TODO |
| Friend management | Add, list, remove, and observe friends | TODO |
| Public API | OpenAPI-described endpoints with session/API-key authentication and rate limiting | TODO |
| Observability | Health checks, Prometheus metrics, Grafana dashboards, alerts, and ELK logs | TODO |

## Modules

The current module plan targets 14 points. Final ownership and evaluation notes remain TODO.

| Module | Weight | Implementation | Owner |
| --- | ---: | --- | --- |
| Frontend and backend frameworks | Major, 2 pts | Gleam/Lustre frontend and Spring Boot backend | TODO |
| Real-time features | Major, 2 pts | STOMP over SockJS/WebSocket for chat and presence | TODO |
| User interaction | Major, 2 pts | Profiles, friends, room membership, and chat | TODO |
| Public API | Major, 2 pts | Secured OpenAPI REST API with API key and rate limiting | TODO |
| Log management infrastructure | Major, 2 pts | Elasticsearch, Logstash, and Kibana | TODO |
| Monitoring system | Major, 2 pts | Prometheus, Grafana, dashboards, and alerts | TODO |
| File upload and management | Minor, 1 pt | Avatar upload, storage, retrieval, and deletion | TODO |
| Health check and status page | Minor, 1 pt | Protected health and Prometheus actuator endpoints | TODO |
| **Total** | **14 pts** |  |  |

## Individual Contributions

The following sections are intentionally left as TODO for the team to complete.

### `yehara`

- Role(s): TODO
- Contributions: TODO
- Challenges and solutions: TODO

### `tmuranak`

- Role(s): TODO
- Contributions: TODO
- Challenges and solutions: TODO

### `kofujita`

- Role(s): TODO
- Contributions: TODO
- Challenges and solutions: TODO

### `yonuma`

- Role(s): TODO
- Contributions: TODO
- Challenges and solutions: TODO

## Known Limitations

- The local development proxy uses a self-signed certificate.
- Presence state is maintained per backend instance and is not persisted in the database.
- The default rate-limit and login-failure counters are in-memory and intended for a single application instance.
- TODO: document any additional known limitations before evaluation.

## License

TODO: add the project license and credit information.
