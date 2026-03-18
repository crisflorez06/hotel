# AGENTS Guide for Hotel Repository
This guide is for coding agents operating in this repository.
It captures build/test commands and practical style conventions from the current codebase.

## Repository Layout
- Backend app: `src/main/java/com/hotel`
- Backend resources: `src/main/resources`
- Backend tests: `src/test/java/com/hotel`, `src/test/resources`
- Frontend app: `frontend/`
- Frontend source: `frontend/src/app`
- Frontend env config: `frontend/src/environments`
- Local infra: `docker-compose.yml`

## Toolchain Snapshot
- Java 21 (`pom.xml`)
- Spring Boot 3.4.x
- Maven wrapper: `./mvnw`
- Local DB: MySQL (`localhost:3311`)
- Test DB: H2 in-memory profile
- Angular + TypeScript strict mode (`frontend/tsconfig.json`)

## Build, Run, Test Commands
### Backend (run from repo root)
- Compile: `./mvnw clean compile`
- Package: `./mvnw clean package`
- Package fast (skip tests): `./mvnw clean package -DskipTests`
- Run app: `./mvnw spring-boot:run`
- Run app with profile: `./mvnw spring-boot:run -Dspring-boot.run.profiles=test`
- Run all tests: `./mvnw test`
- Verify lifecycle: `./mvnw verify`

Single-test execution (important):
- One class: `./mvnw -Dtest=ReservaServiceIT test`
- One method: `./mvnw -Dtest=ReservaServiceIT#exitoCreandoReservaNuevaApartamento_test test`
- Multiple methods: `./mvnw -Dtest=ReservaServiceIT#method1+method2 test`

### Frontend (run inside `frontend/`)
- Install dependencies: `npm install`
- Start dev server: `npm run start`
- Start dev server on all interfaces: `npm run start -- --host 0.0.0.0`
- Build production bundle: `npm run build`
- Build in watch mode: `npm run watch`
- Run tests: `npm run test`

Single-test execution for Angular/Karma:
- One spec file: `npx ng test --include='src/app/path/to/file.spec.ts'`
- By test name pattern: `npx ng test -- --testNamePattern='should do X'` (runner dependent)

## Lint and Format Status
- Backend: no explicit lint plugin configured in `pom.xml`
- Frontend: no `lint` script in `frontend/package.json`, no lint target in `frontend/angular.json`
- Prettier config exists in `frontend/package.json`
- Check frontend formatting: `npx prettier --check .`
- Apply frontend formatting: `npx prettier --write .`

## Local Environment and Services
- Start services: `docker compose up -d`
- Stop services: `docker compose down`
- MySQL endpoint: `localhost:3311`
- phpMyAdmin: `http://localhost:8082`
- Backend port: `8080`
- Frontend API base path: `/backend` (`frontend/src/environments/environment.ts`)

## Backend Style Guidelines (Java/Spring)
### Structure and layering
- Keep packages under `com.hotel`
- Preserve existing layers: `controllers`, `services`, `repositories`, `models`, `dtos`, `mappers`, `specifications`, `security`, `jobs`
- Keep controllers thin; put business logic in services

### Naming conventions
- Classes: PascalCase (`ReservaService`, `ApiExceptionHandler`)
- Methods and fields: camelCase (`crearReserva`, `buscarReservasTabla`)
- Repositories: `*Repository`
- DTOs: `*DTO`, `*RequestDTO`
- Enums: uppercase constants

### Imports and formatting
- 4-space indentation
- Prefer explicit imports in production code
- Wildcard imports are used in some tests; do not introduce new wildcard imports unless surrounding file already uses them
- Keep long signatures wrapped for readability

### Types and API design
- Prefer concrete domain types over raw `Object`
- Keep DTO <-> entity mapping in mapper classes
- Keep query filtering logic in specification classes
- Use `Pageable` for table/list endpoints when already supported

### Dependency injection and transactions
- Prefer constructor injection with `final` fields
- Use `@Transactional` in service write operations
- Keep transaction boundaries in services, not controllers

### Error handling and logging
- `IllegalArgumentException`: invalid input
- `IllegalStateException`: business-rule conflicts
- `EntityNotFoundException`: missing entities
- Let `ApiExceptionHandler` map exceptions to HTTP responses
- Use structured SLF4J logging with operation context
- Do not swallow exceptions silently

## Frontend Style Guidelines (Angular/TypeScript)
### Formatting and strictness
- Follow `frontend/.editorconfig`: 2 spaces, final newline, trimmed whitespace
- Use single quotes in TypeScript
- Keep strict TypeScript compatibility (`strict`, `noImplicitReturns`, `strictTemplates`)

### Naming and file organization
- Use kebab-case filenames (`reserva-nueva.component.ts`, `reserva.service.ts`)
- Component classes end with `Component`
- Service classes end with `Service`
- Keep API logic in services, not components

### Imports, types, and reactive code
- Order imports as framework -> third-party -> local
- Prefer explicit typing for public members and method returns
- Prefer `unknown` over `any` for error payloads, then narrow safely
- Use RxJS operators (`map`, `concatMap`) for request pipelines
- Keep `subscribe` handlers explicit with `next` and `error`

### Frontend error handling
- Reuse `extractBackendErrorMessage` from `frontend/src/app/core/utils/http-error.util.ts`
- Return clear fallback messages for user-visible failures
- Validate and trim form input before API calls
- Keep date/time normalization consistent with backend contracts

## Testing Conventions
- Backend tests are Spring Boot integration-style tests with `@ActiveProfiles("test")`
- Reuse fixtures from `src/test/java/com/hotel/testdata`
- Reuse assertions from `src/test/java/com/hotel/testutils`
- Place Angular specs as colocated `*.spec.ts` files
- Add/update tests when changing business logic, filtering, or contracts

## Core Domain Rule: Units and Rooms
- Business units are `APARTAMENTO` and `APARTAESTUDIO`; operational flows (availability, occupancy, stay/reservation assignment) are managed at room level (`Habitacion`)
- `APARTAMENTO` has 3 rooms and supports both occupancy modes:
  - `COMPLETO`: whole apartment
  - `INDIVIDUAL`: room-by-room rental
- `APARTAESTUDIO` has 1 room and is handled as `COMPLETO`
- `HABITACION` as a unit type is a relative/special search mode used mainly to query apartment rooms individually; it is not treated as a separate physical unit model beyond that search/use-case

## Cursor and Copilot Rules Audit
- Found existing agent file: `frontend/AGENTS.md`
- `.cursorrules`: not present
- `.cursor/rules/`: not present
- `.github/copilot-instructions.md`: not present
If Cursor or Copilot rule files are added later, mirror critical constraints in this file.

## Agent Workflow Tips
- Identify target area first (backend root vs `frontend/`)
- Run narrow tests first, then broader suites
- Keep diffs scoped; avoid unrelated refactors
- Preserve existing Spanish domain terms used in APIs and DTOs
- Before finishing, run at least one verification command for the touched area
