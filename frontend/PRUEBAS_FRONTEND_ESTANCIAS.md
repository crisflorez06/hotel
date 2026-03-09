# Evidencia de pruebas frontend (Estancias)

Fecha: 2026-03-04

## Objetivo

Validar en frontend el comportamiento funcional asociado a los escenarios de creacion de estancias cubiertos en backend, incluyendo casos exitosos y de error.

## Entorno y metodologia

- Frontend: `http://localhost:4200`
- Backend para preparacion de escenarios: `http://localhost:8080`
- Ejecucion UI: Chrome MCP (flujo real de usuario)
- Verificacion adicional: requests de red (status HTTP + payload/mensaje)

## Preparacion de escenarios por backend

Para acelerar y hacer reproducibles los casos de conflicto:

1. Se creo una reserva directa para `APT-301` con `POST /reservas` en `localhost:8080`.
2. Se confirmo conflicto al intentar crear estancia sobre esa unidad con `POST /estancias`:
   - HTTP `409`
   - Mensaje: `No se puede crear la estancia: existe una reserva para las habitaciones con codigo: APT-301-H1, APT-301-H2, APT-301-H3`

## Resultados de pruebas

### 1) Exito creando estancia nueva APARTAMENTO

- Escenario UI: crear estancia en `APT-101`.
- Resultado:
  - `POST /backend/estancias` -> HTTP `201` (reqid 62)
  - En recepcion, `APT-101` queda en `OCUPADO` con ocupacion `3/3`.
  - Accion disponible cambia a `Ver estancia`.
- Estado: **OK**

### 2) Exito creando estancia nueva APARTAESTUDIO

- Escenario UI: crear estancia en `AE-202`.
- Resultado:
  - `POST /backend/estancias` -> HTTP `201` (reqid 75)
  - Mensaje UI: `Estancia registrada con exito.`
  - En recepcion, `AE-202` queda `OCUPADO`.
- Estado: **OK**

### 3) Exito creando estancia nueva HABITACION

- Escenario UI: crear estancia en `APT-201-H1`.
- Resultado:
  - `POST /backend/estancias` -> HTTP `201` (reqid 82)
  - Mensaje UI de registro exitoso.
  - En recepcion, unidad padre `APT-201` queda `PARCIALMENTE` con ocupacion `1/3`.
- Estado: **OK**

### 4) Exito creando estancia sin acompanantes

- Escenario UI: creaciones realizadas solo con cliente, sin `idAcompanantes`.
- Resultado:
  - Las creaciones fueron aceptadas sin error.
  - Request body sin acompanantes y respuesta exitosa.
- Estado: **OK**

### 5) Fallo por fecha de entrada superior a ahora

- Escenario UI: intentar crear estancia con entrada futura.
- Resultado:
  - `POST /backend/estancias` -> HTTP `400` (reqid 61)
  - Mensaje UI/backend:
    `La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura`
- Estado: **OK**

### 6) Fallo por reserva existente (preparado desde backend)

- Escenario UI: intentar crear estancia en `APT-301` (con reserva previa).
- Resultado:
  - Mensaje UI:
    `No se puede crear la estancia: existe una reserva para las habitaciones con codigo: APT-301-H1, APT-301-H2, APT-301-H3`
  - Verificacion directa backend `POST /estancias`:
    - HTTP `409`
    - Mensaje coincide con UI.
- Estado: **OK**

### 7) Fallo por estancia existente

- Escenario UI: intentar crear nueva estancia en `APT-101` ya ocupado.
- Resultado:
  - `POST /backend/estancias` -> HTTP `409` (reqid 273)
  - Mensaje UI/backend:
    `No se puede crear la estancia: existe una estancia para las habitaciones con codigo: APT-101-H1, APT-101-H2, APT-101-H3`
- Estado: **OK**

### 8) Fallo por salida anterior a entrada

- Escenario UI: en `AE-301`, entrada `2026-03-06T12:00` y salida `2026-03-03T12:00`.
- Resultado:
  - `POST /backend/estancias` -> HTTP `400` (reqid 216)
  - Mensaje UI/backend:
    `La fecha de salida debe ser posterior a la fecha de entrada`
- Estado: **OK**

### 9) Exito con estado EXCEDIDA

- Escenario UI: en `AE-301`, entrada historica `2026-03-01T12:00`, salida `2026-03-02T12:00`.
- Resultado:
  - `POST /backend/estancias` -> HTTP `201` (reqid 217)
  - Folio creado: `EST-42902E`
  - En tabla de estancias, la estancia aparece con estado `Excedida`.
- Estado: **OK**

## Resumen final

- Total escenarios validados: **9**
- Exitos: **9**
- Fallos: **0**
- Conclusiones:
  - El frontend refleja correctamente los mensajes de negocio del backend.
  - Los codigos HTTP esperados (`201`, `400`, `409`) se respetan.
  - La recepcion y tabla de estancias se actualizan de forma consistente con los cambios de estado.
