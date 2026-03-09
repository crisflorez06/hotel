# Resultados de pruebas frontend - editarReserva (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Flujo `editarReserva` consumido por frontend (`PUT /backend/reservas/{id}`).
- Casos construidos a partir de `ReservaService.editarReserva` y sus validaciones/dependencias.
- Ejecucion en navegador con Chrome DevTools MCP sobre `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/ReservaService.java`
- `src/main/java/com/hotel/resolvers/UnidadHabitacionResolver.java`
- `src/main/java/com/hotel/services/DisponibilidadService.java`
- `src/main/java/com/hotel/services/OcupanteService.java`
- `src/main/java/com/hotel/controllers/advice/ApiExceptionHandler.java`
- `frontend/src/app/pages/reserva-nueva/reserva-nueva.component.ts`
- `frontend/src/app/services/reserva.service.ts`

## Resumen

- Suite API-driven desde navegador (fetch en contexto frontend): `11/11` PASS.
- Smoke UI de edicion en pantalla real: `2/2` PASS.
- Estado final: `13 pruebas ejecutadas`, `13 aprobadas`, `0 fallidas`.

Datos usados:
- Cliente QA principal: `id=8` (`CLIENTE`)
- Cliente QA secundario: `id=9` (`CLIENTE`)
- Ocupante QA acompanante: `id=10` (`ACOMPANANTE`)
- Unidad apartamento disponible: `APT-301`
- Unidad apartaestudio disponible: `AE-202`
- Referencia estancia activa para conflicto: `AE-302` (`APARTAESTUDIO`)
- Run id suite API: `20162`

## Resultados detallados (suite API desde navegador)

### TC01 - Edicion exitosa de campos generales
- Objetivo: validar actualizacion de `numeroPersonas`, `canalReserva` y `notas` sin cambiar unidad/fechas.
- Preparacion: se crea reserva base en `AE-202` para cliente `id=8`.
- Ejecucion: `PUT /backend/reservas/{id}` con cambios en campos generales.
- Esperado: `201` en creacion base y `204` en edicion.
- Obtenido: `201/204`.
- Evidencia: reserva base `RES-78911L`.
- Estado: PASS.

### TC02 - Edicion exitosa cambiando fechas
- Objetivo: validar `validarEdicionReserva` cuando cambia rango de fechas y no hay conflicto.
- Preparacion: reserva base en `APT-301` con rango futuro.
- Ejecucion: `PUT` moviendo `entradaEstimada/salidaEstimada` a otro rango disponible.
- Esperado: `201/204`.
- Obtenido: `201/204`.
- Evidencia: reserva base `RES-83489J`.
- Estado: PASS.

### TC03 - Edicion exitosa cambiando codigo
- Objetivo: validar cambio de codigo de unidad en reserva confirmada.
- Preparacion: reserva base creada en `APT-301`.
- Ejecucion: `PUT` conservando tipo y fechas, con codigo alterno disponible.
- Esperado: `201/204`.
- Obtenido: `201/204`.
- Evidencia: reserva base `RES-21422J`.
- Estado: PASS.

### TC04 - Error por reserva inexistente
- Objetivo: validar manejo de `idReserva` no existente.
- Preparacion: uso de id inexistente `99999999`.
- Ejecucion: `PUT /backend/reservas/99999999` con payload valido.
- Esperado: `400` con mensaje de reserva no encontrada (segun `IllegalArgumentException`).
- Obtenido: `400`.
- Evidencia: `Reserva no encontrada con id: 99999999`.
- Estado: PASS.

### TC05 - Error por estado no CONFIRMADA
- Objetivo: validar regla de negocio que solo permite editar reservas `CONFIRMADA`.
- Preparacion: crear reserva y luego cancelarla con `PUT /backend/reservas/eliminar/{id}`.
- Ejecucion: intentar `PUT` sobre la reserva ya cancelada.
- Esperado: `201` (creacion) + `204` (cancelacion) + `409` (edicion rechazada).
- Obtenido: `201/204/409`.
- Evidencia: `Solo se puede editar una reserva en estado CONFIRMADA`.
- Estado: PASS.

### TC06 - Error por entrada en fecha pasada
- Objetivo: validar regla de fecha de entrada en edicion.
- Preparacion: crear reserva valida futura.
- Ejecucion: editar con `entradaEstimada` en fecha pasada.
- Esperado: `201/400`.
- Obtenido: `201/400`.
- Evidencia: `La fecha de entrada debe ser posterior a la fecha actual`.
- Estado: PASS.

### TC07 - Error por salida menor a entrada
- Objetivo: validar regla de coherencia de fechas en edicion.
- Preparacion: crear reserva valida.
- Ejecucion: editar con `salidaEstimada < entradaEstimada`.
- Esperado: `201/400`.
- Obtenido: `201/400`.
- Evidencia: `La fecha de salida estimada no puede ser anterior a la fecha de entrada`.
- Estado: PASS.

### TC08 - Error por solapamiento con otra reserva
- Objetivo: validar conflicto de disponibilidad al editar fechas.
- Preparacion: crear reserva de bloqueo y otra reserva base en misma unidad con rango no solapado.
- Ejecucion: editar la base para que choque con la de bloqueo.
- Esperado: `201 + 201 + 409`.
- Obtenido: `201/201/409`.
- Evidencia: `No se puede editar la reserva: existe una reserva para las habitaciones con codigo: APT-301-H1, APT-301-H2, APT-301-H3`.
- Estado: PASS.

### TC09 - Error por conflicto con estancia activa
- Objetivo: validar rechazo al mover reserva a unidad con estancia activa en rango conflictivo.
- Preparacion: crear reserva base en `AE-202`; usar referencia activa real `AE-302`.
- Ejecucion: editar codigo/tipo hacia `AE-302` y fechas que cruzan periodo activo.
- Esperado: `201/409`.
- Obtenido: `201/409`.
- Evidencia: `No se puede editar la reserva: existe una estancia para las habitaciones con codigo: AE-302`.
- Estado: PASS.

### TC10 - Error cuando idOcupante no es CLIENTE
- Objetivo: validar `buscarCliente` durante edicion.
- Preparacion: reserva base con cliente valido; usar `idOcupante=10` (`ACOMPANANTE`).
- Ejecucion: `PUT` manteniendo resto de campos validos.
- Esperado: `201/404`.
- Obtenido: `201/404`.
- Evidencia: `Cliente no encontrado con id: 10`.
- Estado: PASS.

### TC11 - Error de validacion DTO en PUT
- Objetivo: validar `@Valid` y respuesta `VALIDATION_ERROR` en edicion.
- Preparacion: crear reserva base valida.
- Ejecucion: `PUT` omitiendo campo obligatorio `tipoUnidad`.
- Esperado: `201/400` con `error=VALIDATION_ERROR`.
- Obtenido: `201/400`.
- Evidencia: `tipoUnidad es obligatorio`.
- Estado: PASS.

## Smoke UI (pantalla de reservas)

### UI-01 Edicion exitosa desde interfaz
- Flujo ejecutado:
- abrir `Reservas`, seleccionar una reserva `CONFIRMADA`;
- click en `Editar`;
- en `Editar reserva`, modificar `Numero de personas` y `Canal`;
- guardar con `Guardar cambios`.
- Resultado observado:
- request de red `PUT /backend/reservas/1` con estado `204`;
- regreso automatico a tabla de reservas;
- datos reflejados en tabla (ejemplo: `RES-78911L` con `numeroPersonas=4`, `canal=Email`).
- Estado: PASS.

### UI-02 Bloqueo de edicion para reserva cancelada
- Flujo ejecutado:
- en tabla de reservas, seleccionar una reserva con estado `CANCELADA` (`RES-33086H`).
- Resultado observado:
- boton `Editar` permanece deshabilitado;
- mensaje de UI: `No se puede editar una reserva completada o cancelada.`;
- no se dispara request `PUT`.
- Estado: PASS.

## Conclusiones

- El endpoint de edicion cumple reglas clave: estado permitido, validacion de fechas, control de disponibilidad por reservas y estancias, validacion de cliente y validacion DTO.
- El frontend maneja correctamente los resultados del backend en edicion (exito y errores) y aplica bloqueos preventivos en tabla para estados no editables.
