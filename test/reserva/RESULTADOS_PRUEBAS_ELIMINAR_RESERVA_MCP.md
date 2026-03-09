# Resultados de pruebas frontend - eliminarReserva (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Flujo `eliminarReserva` consumido por frontend (`PUT /backend/reservas/eliminar/{id}`).
- Casos derivados de `ReservaService.eliminarReserva` y efectos colaterales esperados (estado de reserva, pagos y auditoria).
- Ejecucion en navegador con Chrome DevTools MCP sobre `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/ReservaService.java`
- `src/main/java/com/hotel/services/PagoService.java`
- `src/main/java/com/hotel/controllers/ReservaController.java`
- `src/main/java/com/hotel/controllers/advice/ApiExceptionHandler.java`
- `frontend/src/app/pages/reservas/reservas.component.ts`
- `frontend/src/app/services/reserva.service.ts`

## Resumen

- Suite API-driven desde navegador (fetch en contexto frontend): `9/9` PASS.
- Smoke UI de eliminacion en pantalla real: `2/2` PASS.
- Estado final: `11 pruebas ejecutadas`, `11 aprobadas`, `0 fallidas`.

Datos usados:
- Cliente QA principal: `id=8` (`CLIENTE`)
- Cliente QA secundario: `id=9` (`CLIENTE`)
- Ocupante QA acompanante: `id=10` (`ACOMPANANTE`)
- Unidad apartamento disponible: `APT-301`
- Unidad apartaestudio disponible: `AE-202`
- Run id suite API: `77908`

## Resultados detallados (suite API desde navegador)

### TC01 - Eliminacion exitosa de reserva CONFIRMADA
- Objetivo: validar caso feliz de cancelacion de reserva.
- Preparacion: crear reserva valida en unidad disponible.
- Ejecucion: `PUT /backend/reservas/eliminar/{id}`.
- Esperado: `201` (creacion), `204` (eliminacion) y reserva en estado `CANCELADA`.
- Obtenido: `201/204/CANCELADA`.
- Evidencia: `codigoReserva=RES-23321C`.
- Estado: PASS.

### TC02 - Error al eliminar reserva inexistente
- Objetivo: validar manejo de `id` no existente.
- Preparacion: usar id inexistente `99999999`.
- Ejecucion: `PUT /backend/reservas/eliminar/99999999`.
- Esperado: `400` con mensaje de reserva no encontrada.
- Obtenido: `400`.
- Evidencia: `Reserva no encontrada con id: 99999999`.
- Estado: PASS.

### TC03 - No permite eliminar dos veces la misma reserva
- Objetivo: validar regla de estado (`solo CONFIRMADA`).
- Preparacion: crear reserva valida.
- Ejecucion: eliminar una vez (`204`) y volver a eliminar la misma (`409`).
- Esperado: `201/204/409`.
- Obtenido: `201/204/409`.
- Evidencia: `Solo se puede eliminar una reserva en estado CONFIRMADA`.
- Estado: PASS.

### TC04 - Eliminacion de reserva con estancia asociada
- Objetivo: validar que una reserva con estancia asociada tambien puede cancelarse correctamente.
- Preparacion: crear reserva con estancia asociada (normal en crear reserva).
- Ejecucion: `PUT /backend/reservas/eliminar/{id}` y verificacion en tabla de reservas por codigo.
- Esperado: `201/204` y `estadoReserva=CANCELADA`.
- Obtenido: `201/204/CANCELADA/null`.
- Evidencia: `codigoReserva=RES-15532Y`.
- Nota: el valor `estadoEstancia` llego `null` en este endpoint/DTO de tabla para este caso; la validacion se tomo sobre estado de reserva, codigo HTTP y consistencia de flujo.
- Estado: PASS.

### TC05 - Eliminacion marca pagos asociados en ELIMINADO
- Objetivo: validar efecto colateral de `pagoService.eliminarTodoLosPagos` al cancelar reserva.
- Preparacion:
- crear reserva;
- crear pago `ANTICIPO_RESERVA` sobre la estancia asociada.
- Ejecucion:
- cancelar reserva;
- consultar `GET /backend/pagos` filtrando por `codigoReserva` y `estados=ELIMINADO`.
- Esperado: al menos un pago asociado en estado `ELIMINADO`.
- Obtenido: `201/200/204/1` (1 pago eliminado).
- Evidencia: `codigoReserva=RES-34226D`.
- Estado: PASS.

### TC06 - No permite eliminar reservas ya CANCELADAS
- Objetivo: validar regla de negocio sobre estados no editables/no eliminables.
- Preparacion: tomar una reserva ya `CANCELADA` de la tabla.
- Ejecucion: `PUT /backend/reservas/eliminar/{idCancelada}`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `Solo se puede eliminar una reserva en estado CONFIRMADA`.
- Estado: PASS.

### TC07 - Precondicion de cliente invalido en creacion (control de datos)
- Objetivo: asegurar que no se generan reservas con `idOcupante` no cliente para luego eliminar.
- Preparacion: intento de crear reserva con `idOcupante=10` (`ACOMPANANTE`).
- Ejecucion: `POST /backend/reservas`.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Cliente no encontrado con id: 10`.
- Estado: PASS.

### TC08 - Eliminacion con id negativo
- Objetivo: validar robustez para ids invalidos.
- Preparacion: id `-1`.
- Ejecucion: `PUT /backend/reservas/eliminar/-1`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `Reserva no encontrada con id: -1`.
- Estado: PASS.

### TC09 - Registro de auditoria por eliminacion
- Objetivo: validar creacion de evento `ELIMINACION_RESERVA`.
- Preparacion: crear y luego cancelar una reserva valida.
- Ejecucion:
- `PUT /backend/reservas/eliminar/{id}`;
- `GET /backend/eventos` filtrando por `tiposEvento=ELIMINACION_RESERVA` y `codigoReserva`.
- Esperado: evento encontrado en auditoria.
- Obtenido: `201/204/200/true`.
- Evidencia: `codigoReserva=RES-94419S`.
- Estado: PASS.

## Smoke UI (pantalla de reservas)

### UI-01 Eliminacion exitosa desde tabla y modal
- Flujo ejecutado:
- filtrar por `codigoReserva=RES-71302P`;
- seleccionar la reserva `CONFIRMADA`;
- click `Eliminar`;
- confirmar en modal `Eliminar reserva`.
- Resultado observado:
- mensaje de exito visible: `Reserva cancelada correctamente.`;
- la fila queda con estado `Cancelada` y acciones de gestion deshabilitadas.
- Evidencia de red MCP: request `PUT /backend/reservas/eliminar/29` con estado `204` (reqid `86`).
- Estado: PASS.

### UI-02 Bloqueo de eliminacion para reserva CANCELADA
- Flujo ejecutado: con la misma reserva ya cancelada seleccionada en tabla.
- Resultado observado:
- boton `Eliminar` deshabilitado;
- mensaje de UI: `No se puede eliminar una reserva completada o cancelada.`;
- no se dispara un nuevo `PUT` de eliminacion al backend.
- Estado: PASS.

## Conclusiones

- El endpoint `eliminarReserva` cumple las reglas principales: solo elimina reservas `CONFIRMADA`, retorna errores correctos para ids invalidos/estados no permitidos y registra auditoria.
- El frontend consume correctamente el servicio y refleja el resultado en UI (modal, mensaje de exito y estado final en tabla).
