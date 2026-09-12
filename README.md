_This project was created by yehara, tmuranak, kofujita, and yonuma as part of the 42 curriculum._

# Online Workspace (ft_transcendence)

[Japanese version](README.ja.md)

## Overview

Online Workspace is a collaboration platform for working with other people online. Users can create, search for, and join work rooms, check who is present, and communicate through real-time chat.

The project is designed to let logged-in users join work rooms for a shared purpose without requiring a friendship relationship. It focuses on a secure REST API, WebSocket-based real-time communication, and an observable containerized environment.

### Key Features

- User registration, login, and logout
- Profile editing, avatar image upload, user search, and visibility settings
- Room creation, listing, joining, and leaving
- Chat
- Friend management and display of friends' online status

## Getting Started

```bash
docker compose up --build
```

Access the development proxy at the following URL:

- HTTPS application: `https://localhost:8443`

The proxy uses a self-signed certificate for development. Your browser may require a certificate exception when starting the environment.

### Starting the Observability Environment

The observability services are defined in `compose.observability.yaml`. Set the required variables and run:

```bash
MANAGEMENT_API_KEY="change-me" \
GRAFANA_ADMIN_PASSWORD="change-me" \
GRAFANA_ALERT_DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/..." \
docker compose -f compose.yaml -f compose.observability.yaml up prometheus grafana
```

## References

### Project Documentation

- [Requirements](docs/requirements.md)
- [API contract](docs/openapi.yaml)
- [API development guide](docs/api_development.md)
- [WebSocket event contract](docs/websocket_events.md)
- [Database schema](docs/db_schema.md)
- [Web security policy](docs/web_security.md)
- [Architecture](docs/architecture.md)
- [42 subject](docs/ft_transcendence.pdf)

### External References

- [Spring Boot documentation](https://spring.io/projects/spring-boot)
- [Gleam documentation](https://gleam.run/documentation/)
- [PostgreSQL documentation](https://www.postgresql.org/docs/)
- [Prometheus documentation](https://prometheus.io/docs/)
- [Grafana documentation](https://grafana.com/docs/)

### AI Usage

AI tools such as ChatGPT and GitHub Copilot were used as assistants in this project. The main uses were as follows.

Coding was primarily implemented using an AI coding agent, but every change was reviewed by a person to ensure that it did not diverge from the architecture. AI was also used to connect the backend and frontend.

- Assistance with the initial setup of Nginx configuration and Dockerfiles
- Ideas for troubleshooting the integration of the ELK stack with the Spring Boot backend
- Generation of boilerplate code for backend unit tests

AI-generated content was reviewed, tested, and modified by the team. Adopted code was used only after the team understood its contents.

## Team Information

The team consists of the following four members. Their roles and primary responsibilities are listed below.

| Member | Role | Responsibilities |
| --- | --- | --- |
| `yehara` | Product Owner, Developer | Requirements definition, API, security, infrastructure, and monitoring |
| `tmuranak` | Tech Lead, Developer | Gleam/Lustre frontend and WebSocket client |
| `kofujita` | Project Manager, Developer | Development environment, database, backend review, and team support |
| `yonuma` | Developer | Authentication, profile, friends, rooms, and real-time presence updates |

## Project Management

- Communication: Discord
- Regular meetings: Weekly meetings
- Task management tools: GitHub Issues and GitHub Projects
- Code review process: Changes to the `main` branch are made through pull requests and receive peer review from at least one person
- Branching: [Branch naming convention](docs/branch_naming_convention.md)

## Technology Stack

### Frontend

- Gleam
- Lustre
- Tailwind CSS
- JavaScript FFI for browser APIs and STOMP communication

Gleam is used to build a type-safe functional frontend. Lustre provides the application structure, and Tailwind CSS is used for styling.

### Backend

- Java 25
- Spring Boot 4
- Spring Security
- Spring Web MVC
- Spring WebSocket / STOMP
- Bean Validation
- MyBatis
- Gradle

The HTTP and WebSocket servers are built with Spring Boot. Spring Security handles session authentication, CSRF protection, API keys, access control, and security events. MyBatis is used for SQL mapping and database access.

### Database and Infrastructure

- PostgreSQL 16
- Flyway
- Docker / Docker Compose
- Nginx
- Prometheus / Grafana
- Elasticsearch / Logstash / Kibana

Docker Compose provides a reproducible development environment, and Nginx is used as an HTTPS reverse proxy.

## Database Schema

The relationships between the main entities are as follows.

```text
users
├── user_profiles
├── room_memberships ── rooms ── room_categories
├── chat_messages
├── friendships
└── account status / withdrawal data
```

The main tables are as follows.

- `users`: Account and authentication information
- `user_profiles`: Name, profile, work category, visibility settings, and avatar information
- `rooms`: Room settings, participant limit, work style, status, and creator
- `room_categories`: Room categories configured by administrators
- `room_memberships`: Relationships between users and rooms
- `chat_messages`: Chat history within rooms
- `friendships`: Friendship and removed-friend relationships

The detailed schema, including constraints, is described in [docs/db_schema.md](docs/db_schema.md). Database changes are applied through Flyway migrations.

## Feature List

| Feature | Description | Owner |
| --- | --- | --- |
| Authentication | Registration, login, logout, sessions, and CSRF tokens | `yehara`, `yonuma`, `kofujita` |
| Profile management | Visibility settings, bio, work categories, and avatar management | `yehara`, `yonuma`, `kofujita` |
| User search | Paginated name search and public profiles | `yonuma`, `kofujita` |
| Room management | Creation, listing, filtering, details, updating, and closing | `yehara`, `yonuma`, `kofujita` |
| Room participation | Joining, leaving, participant list, capacity limits, and access control | `yehara`, `yonuma`, `kofujita` |
| Chat | Paginated history and message validation | `yehara`, `tmuranak`, `kofujita` |
| Real-time updates | Chat, presence, participant count, room creation, and friend status | `yehara`, `tmuranak`, `yonuma`, `kofujita` |
| Friend management | Adding, listing, removing, and online status | `yehara`, `yonuma`, `kofujita` |
| Public API | OpenAPI-defined REST API with session/API key authentication and rate limiting | `yehara`, `kofujita` |
| Observability | Health checks, Prometheus, Grafana, alerting, and ELK logs | `yehara`, `kofujita` |

## Modules

The current module plan totals 14 points. Owners are listed in each row.
Refer to [this spreadsheet](https://docs.google.com/spreadsheets/d/1ScHkTosDOwcBnFoCIuz26hpp9bQ9IQu2MThSVHGN9mE/edit?pli=1&gid=0#gid=0) for the module requirements checklist.

| Module | Category | Implementation | Owner |
| --- | ---: | --- | --- |
| Frontend and backend frameworks | Major, 2 points | Gleam/Lustre frontend and Spring Boot backend | `yehara`, `tmuranak` |
| Real-time features | Major, 2 points | Chat and presence through STOMP over WebSocket | `yehara`, `tmuranak`, `yonuma` |
| User-to-user features | Major, 2 points | Profiles, friends, room participation, and chat | `yehara`, `yonuma` |
| Public API | Major, 2 points | OpenAPI REST API with API keys and rate limiting | `yehara` |
| Log management infrastructure | Major, 2 points | Elasticsearch, Logstash, and Kibana | `yehara` |
| Monitoring system | Major, 2 points | Prometheus, Grafana, dashboards, and alerts | `yehara` |
| File upload and management | Minor, 1 point | Avatar upload, storage, retrieval, and deletion | `yehara`, `yonuma` |
| Health checks and status | Minor, 1 point | Protected Actuator health / Prometheus endpoints | `yehara` |
| **Total** | **14 points** |  |  |

### Rationale for Module Selection

- Web frameworks: Gleam/Lustre and Spring Boot provide a type-safe UI and robust APIs.
- Real-time features: WebSocket distributes changes to chat, presence, and participant counts to connected users.
- User-to-user features: Profiles, friends, rooms, and chat allow users to interact with their work partners.
- Public API: The project provides a REST API with OpenAPI, API keys, and rate limiting.
- Log management and monitoring: ELK, Prometheus, Grafana, and alerting make the operational state observable.
- File management and health checks: The project provides avatar image management and protected operational endpoints.

## Individual Contributions

The main contributions of each member and the challenges they addressed are listed below.

### tmuranak

- Designed the frontend architecture using Gleam.
- Implemented the real-time WebSocket client logic (`ws.js`).
- *Challenge faced:*
  - Anticipating the difficulty of connecting to the server-side API, actively separated functions that might be needed from the design stage.

### yonuma

- Implemented user authentication features, including user registration, login/logout, and session authentication.
- Implemented friend search, adding, and removal, as well as profile and My Page displays, friend status, and avatar displays.
- Implemented room listing, details, joining, and leaving; handling for page refreshes and tab closing; and real-time participant count updates through WebSocket.
- Resolved issues involving double-counting participant counts from API and WebSocket notifications, friend status on private profiles, and external avatar display.
- Having never worked with Spring Security before, initially found it difficult to understand what was happening. Consulted yehara, a team member with practical experience in this area, to resolve questions.

### kofujita

- Reviewed the development environment.
- Reviewed the database and backend.
- Helped wherever additional support was needed.
- Refer to the commits for details.
- *Challenge faced:*
  - Since it was necessary to balance personal tasks with reviewing other members' work within a limited timeframe, secured time to carry out the reviews.

### yehara

- Defined the requirements, system architecture, database schema, and OpenAPI contract, and established the development foundation using Flyway, Swagger UI, and OpenAPI lint.
- Built the Spring Boot API foundation and implemented APIs and integration tests for the common error format, room creation/listing/joining/leaving, chat, friend management, and other features.
- Implemented login, logout, and session management, and established CSRF protection, API key authentication, rate limiting, account-status revalidation, and security audit logging.
- Implemented real-time chat and presence through WebSocket and connected authentication, room, and friend features to the Gleam frontend.
- Set up Docker Compose, Nginx, automatic generation of local HTTPS certificates, CI, and the frontend build environment.
- Built the ELK, Actuator, Prometheus, and Grafana monitoring foundation, and automated the configuration of dashboards, incident alerts, and Discord notifications.
- In line with the remaining time and final requirements, organized and removed out-of-scope features such as administration, blocking, timers, and work history, adjusting the implementation scope to a completable size.
- *Challenge faced:*
  - With limited time remaining for the initially planned features, removed unnecessary features while ensuring that the requirements were met.
