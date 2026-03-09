# Resultados de pruebas frontend - crearReserva (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Flujo `crearReserva` consumido desde frontend (`POST /backend/reservas`).
- Casos derivados de `ReservaService.crearReserva` y métodos internos/dependientes.
- Ejecucion realizada con Chrome DevTools MCP sobre la app en `http://localhost:4200`.

Referencias de codigo analizado:
- `src/main/java/com/hotel/services/ReservaService.java`
- `src/main/java/com/hotel/services/DisponibilidadService.java`
- `src/main/java/com/hotel/resolvers/UnidadHabitacionResolver.java`
- `src/main/java/com/hotel/services/OcupanteService.java`
- `src/main/java/com/hotel/controllers/advice/ApiExceptionHandler.java`

## Resumen de ejecucion

- Suite API desde contexto frontend (fetch en navegador): 10/10 exitosos.
- Smoke UI en `reservas/nueva`: 2/2 exitosos.

Datos usados en ejecucion:
- Cliente QA principal: `id=8` (tipo `CLIENTE`)
- Cliente QA secundario: `id=9` (tipo `CLIENTE`)
- Ocupante QA acompanante: `id=10` (tipo `ACOMPANANTE`)
- Unidad apartamento disponible: `APT-301`
- Unidad apartaestudio disponible: `AE-202`
- Habitacion disponible: `APT-201-H1`
- Referencia de estancia activa para conflicto: `AE-302` (`APARTAESTUDIO`)

## Resultados detallados (suite API ejecutada desde navegador)

Run id: `62169`

### TC01 - Creacion exitosa para APARTAMENTO
- Objetivo: validar el flujo feliz con `tipoUnidad=APARTAMENTO` y unidad disponible.
- Preparacion: se selecciono unidad disponible `APT-301` y cliente QA `id=8`.
- Ejecucion: `POST /backend/reservas` desde navegador con fechas futuras (`entrada < salida`), `canalReserva=MOSTRADOR`, `numeroPersonas=2`.
- Resultado esperado: `201 Created` con `codigoReserva` generado.
- Resultado obtenido: `201 Created`.
- Evidencia: `codigoReserva=RES-72080V`.
- Estado: PASS.

### TC02 - Creacion exitosa para APARTAESTUDIO
- Objetivo: validar flujo feliz para `tipoUnidad=APARTAESTUDIO`.
- Preparacion: se uso unidad disponible `AE-202` y cliente QA `id=8`.
- Ejecucion: `POST /backend/reservas` con rango de fechas futuro no solapado.
- Resultado esperado: `201 Created`.
- Resultado obtenido: `201 Created`.
- Evidencia: `codigoReserva=RES-59980F`.
- Estado: PASS.

### TC03 - Creacion exitosa para HABITACION
- Objetivo: validar que el resolver por habitacion funcione cuando `tipoUnidad=HABITACION`.
- Preparacion: se uso habitacion disponible `APT-201-H1` y cliente QA `id=8`.
- Ejecucion: `POST /backend/reservas` con fechas futuras y payload valido.
- Resultado esperado: `201 Created`.
- Resultado obtenido: `201 Created`.
- Evidencia: `codigoReserva=RES-36464S`.
- Estado: PASS.

### TC04 - Rechazo por fecha de entrada en el pasado
- Objetivo: validar regla `entradaEstimada >= hoy`.
- Preparacion: unidad valida y cliente valido, pero `entradaEstimada` en fecha pasada.
- Ejecucion: `POST /backend/reservas` con `entradaEstimada` menor a la fecha actual.
- Resultado esperado: `400 Bad Request` con mensaje de validacion de fecha de entrada.
- Resultado obtenido: `400 Bad Request`.
- Evidencia: `La fecha de entrada debe ser posterior a la fecha actual`.
- Estado: PASS.

### TC05 - Rechazo por salida menor que entrada
- Objetivo: validar regla `salidaEstimada >= entradaEstimada`.
- Preparacion: unidad y cliente validos.
- Ejecucion: `POST /backend/reservas` con `salidaEstimada` menor a `entradaEstimada`.
- Resultado esperado: `400 Bad Request` con mensaje de validacion de fecha de salida.
- Resultado obtenido: `400 Bad Request`.
- Evidencia: `La fecha de salida estimada no puede ser anterior a la fecha de entrada`.
- Estado: PASS.

### TC06 - Rechazo por solapamiento con reserva existente
- Objetivo: validar control de disponibilidad contra reservas existentes.
- Preparacion: misma unidad `APT-301` para dos intentos consecutivos con fechas traslapadas.
- Ejecucion:
- Intento 1: `POST /backend/reservas` con rango A (creacion base).
- Intento 2: `POST /backend/reservas` con rango B que cruza parcialmente con A.
- Resultado esperado: primer intento `201`, segundo intento `409 Conflict`.
- Resultado obtenido: `201/409`.
- Evidencia: `No se puede crear la reserva: existe una reserva para las habitaciones con codigo: APT-301-H1, APT-301-H2, APT-301-H3`.
- Estado: PASS.

### TC07 - Rechazo por estancia activa/excedida
- Objetivo: validar bloqueo cuando ya existe una estancia activa en la misma unidad/habitaciones.
- Preparacion: se tomo como referencia una estancia activa real (`AE-302`, `APARTAESTUDIO`).
- Ejecucion: `POST /backend/reservas` para esa misma referencia en rango conflictivo.
- Resultado esperado: `409 Conflict` con mensaje de estancia existente.
- Resultado obtenido: `409 Conflict`.
- Evidencia: `No se puede crear la reserva: existe una estancia para las habitaciones con codigo: AE-302`.
- Estado: PASS.

### TC08 - Rechazo cuando idOcupante no corresponde a CLIENTE
- Objetivo: validar `OcupanteService.buscarCliente` cuando el ocupante existe pero no es tipo `CLIENTE`.
- Preparacion: se uso ocupante QA `id=10` con tipo `ACOMPANANTE`.
- Ejecucion: `POST /backend/reservas` con payload completo, cambiando solo `idOcupante`.
- Resultado esperado: `404 Not Found`.
- Resultado obtenido: `404 Not Found`.
- Evidencia: `Cliente no encontrado con id: 10`.
- Estado: PASS.

### TC09 - Rechazo por codigo invalido para tipo HABITACION
- Objetivo: validar error cuando el codigo no existe para el tipo enviado.
- Preparacion: `tipoUnidad=HABITACION` con codigo inexistente `NO-EXISTE-62169`.
- Ejecucion: `POST /backend/reservas` con payload valido en el resto de campos.
- Resultado esperado: `404 Not Found` por habitacion inexistente.
- Resultado obtenido: `404 Not Found`.
- Evidencia: `Habitacion no encontrada con codigo: NO-EXISTE-62169`.
- Estado: PASS.

### TC10 - Rechazo por validacion DTO (campo obligatorio faltante)
- Objetivo: validar respuesta de `ApiExceptionHandler` para errores de validacion `@Valid`.
- Preparacion: payload sin `tipoUnidad` (campo obligatorio), manteniendo el resto de campos validos.
- Ejecucion: `POST /backend/reservas` con JSON incompleto.
- Resultado esperado: `400 Bad Request` con `error=VALIDATION_ERROR` y detalle por campo.
- Resultado obtenido: `400 Bad Request`.
- Evidencia: `tipoUnidad es obligatorio`.
- Estado: PASS.

## Smoke UI (reservas/nueva)

### UI-01 Crear reserva exitosa desde formulario
- Flujo: seleccionar tipo/codigo, asignar cliente, definir fechas, guardar.
- Resultado: PASS.
- Evidencia visual MCP: snapshot con mensaje `Reserva registrada con exito.` (nodo `uid=9_0`).

### UI-02 Error de negocio mostrado en frontend
- Flujo: enviar con fecha de entrada anterior a hoy.
- Resultado: PASS.
- Evidencia visual MCP: snapshot con mensaje `La fecha de entrada debe ser posterior a la fecha actual` (nodo `uid=12_0`).
- Evidencia red MCP: request `POST /backend/reservas` con estado `400`.

## Observaciones

- Durante una corrida inicial, algunas unidades no disponibles produjeron `409` por estado inconsistente. Se ajusto la data de prueba para usar alojamientos en estado `DISPONIBLE` y aislar correctamente los escenarios.
- El frontend muestra correctamente mensajes provenientes de backend via `extractBackendErrorMessage`.

## Conclusion

- El flujo `crearReserva` y sus validaciones principales quedaron cubiertos en frontend usando MCP Chrome.
- Estado final: `12 pruebas ejecutadas`, `12 aprobadas`, `0 fallidas`.
