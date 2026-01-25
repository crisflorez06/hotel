# Repository Guidelines

## Project Structure & Module Organization
- Backend Spring Boot lives under `src/main/java` with controllers, services, repositories, DTOs, and shared mappers. Resources (Liquibase/config/templates) stay in `src/main/resources`, while integration/unit tests mirror the package tree in `src/test/java`.
- The Angular frontend is in `frontend/`, with source at `frontend/src/app`, environments under `frontend/src/environments`, and shared UI pieces under `frontend/src/app/shared`. Static assets land in `frontend/public`.
- Operational artifacts reside at the root: `Dockerfile`, `docker-compose.yml`, and SQL helpers in `sql/`. Keep new scripts in these dedicated folders.

## Build, Test, and Development Commands
- `mvn -DskipTests package`: builds the backend jar quickly for API or deployment verification.
- `mvn test`: runs the full Spring/JUnit suite; keep it clean before branching or pushing.
- `npm install` followed by `npm run start -- --host 0.0.0.0` (inside `frontend/`): installs dependencies and launches the Angular dev server.
- `npm run test`: executes Angular unit tests through Karma/Jest (per current config).

## Coding Style & Naming Conventions
- Java: 4-space indentation, Spring defaults, Lombok for boilerplate, constructor injection by default. Controllers remain thin; business rules live in services. Run `mvn fmt:format` (or IDE format) before committing.
- Angular/TypeScript: follow Angular style guide, use 2-space indentation, components in `kebab-case` (`ventas.component.ts`), services ending in `.service.ts`. Lint via `npm run lint` when available.
- Favor descriptive names (e.g., `ProductoService`, `IngresarStockComponent`) and keep environment URLs in `frontend/src/environments`.

## Testing Guidelines
- Backend tests use JUnit 5 + Spring Boot slices. Place tests under matching packages (e.g., `src/test/java/com/SICOIL/services/ProductoServiceTest`). Aim for meaningful coverage around services and inventory flows.
- Frontend uses Angular `.spec.ts` files colocated with their components/services. Add tests for shared services and pipes, and ensure `npm run test` succeeds prior to PRs.

## Commit & Pull Request Guidelines
- Commit messages follow short imperative format (`Add venta cancellation flow`). Group related changes; avoid mixing backend and frontend edits unless required.
- PRs should describe motivation, summarize major changes, cite affected modules, and link Jira/GitHub issues. Include screenshots for UI tweaks and note manual verification steps or remaining TODOs.

## Agent Tips
- Prefer `rg` for searches (`rg -n "productos" src/`), and avoid destructive git commands unless requested.
- Reference environment files (`frontend/src/environments`) for API hosts, and keep secrets out of the repo.
