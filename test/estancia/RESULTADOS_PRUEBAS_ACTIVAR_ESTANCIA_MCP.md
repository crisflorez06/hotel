# Resultados de pruebas frontend - activarEstancia (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `activarEstancia` (`PUT /estancias/activar`) consumido por frontend.
- Casos basados en validaciones y dependencias de `EstanciaService.activarEstancia`.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/EstanciaService.java`
- `src/main/java/com/hotel/controllers/EstanciaController.java`
- `src/main/java/com/hotel/dtos/estancia/ActivarEstanciaDTO.java`
- `frontend/src/app/services/estancia.service.ts`
- `frontend/src/app/pages/estancia-nueva/estancia-nueva.component.ts`
- `frontend/src/app/pages/estancia-nueva/estancia-nueva.component.html`

## Resumen

- Suite API-driven desde navegador: `12 pruebas` -> `12 PASS`, `0 FAIL`.
- Smoke UI en `/estancias/nueva`: `2 pruebas` -> `2 PASS`.
- Total ejecutado: `14 pruebas` -> `14 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id: `48086`
- Unidad objetivo principal: `APT-201-H1` (`tipoUnidad=HABITACION`)
- Cliente QA: `id=11`
- Estado previo del ambiente: se limpio data de estancias activas/excedidas conflictivas para aislar escenarios de activacion.

## Resultados detallados (suite API)

### TC01 - Activacion exitosa desde reserva confirmada
- Objetivo: validar flujo feliz de activacion con reserva `CONFIRMADA` y estancia `RESERVADA` asociada.
- Preparacion: crear reserva valida para `APT-201-H1`, generar estancia reservada y verificar disponibilidad del rango.
- Ejecucion: `PUT /backend/estancias/activar` con payload valido (`idReserva`, `entradaReal`, `salidaEstimada`, ocupantes validos).
- Esperado: `200` y estancia en estado operativo activo; reserva pasa a `COMPLETADA`.
- Obtenido: `200`.
- Evidencia: respuesta OK con actualizacion de estado de estancia y reserva asociada.
- Estado: PASS.

### TC02 - Rechazo por reserva inexistente
- Objetivo: validar control de existencia de reserva antes de activar.
- Ejecucion: `PUT /backend/estancias/activar` con `idReserva` inexistente.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: mensaje de reserva no encontrada.
- Estado: PASS.

### TC03 - Rechazo por reserva en estado no confirmada
- Objetivo: validar regla de negocio que exige reserva `CONFIRMADA`.
- Preparacion: usar reserva en estado distinto de `CONFIRMADA`.
- Ejecucion: `PUT /backend/estancias/activar` con esa reserva.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: mensaje indicando que solo se puede activar desde reserva confirmada.
- Estado: PASS.

### TC04 - Rechazo por estancia asociada inexistente
- Objetivo: validar que la reserva tenga estancia vinculada antes de activar.
- Ejecucion: `PUT /backend/estancias/activar` para reserva sin estancia asociada.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: mensaje de estancia no encontrada para la reserva.
- Estado: PASS.

### TC05 - Rechazo por estancia en estado diferente a RESERVADA
- Objetivo: validar precondicion de estado de estancia.
- Preparacion: usar una estancia asociada en estado distinto de `RESERVADA`.
- Ejecucion: `PUT /backend/estancias/activar`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: mensaje de conflicto por estado invalido para activacion.
- Estado: PASS.

### TC06 - Rechazo por fecha de salida menor o igual a entrada
- Objetivo: validar `salidaEstimada > entradaReal` durante activacion.
- Ejecucion: `PUT` con `salidaEstimada <= entradaReal`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: mensaje de validacion de fecha de salida.
- Estado: PASS.

### TC07 - Rechazo por entrada futura
- Objetivo: validar regla que impide activar estancia con `entradaReal` futura.
- Ejecucion: `PUT` con `entradaReal > now`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura`.
- Estado: PASS.

### TC08 - Rechazo por conflicto de disponibilidad
- Objetivo: validar `disponibilidadService.verificarDisponibilidadModificar` en activacion.
- Preparacion: provocar solapamiento con estancia activa/reservada de la misma unidad.
- Ejecucion: `PUT /backend/estancias/activar` en rango conflictivo.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: mensaje de conflicto por disponibilidad de habitacion/unidad.
- Estado: PASS.

### TC09 - Rechazo por idCliente invalido en activacion
- Objetivo: validar resolucion de ocupantes en activacion.
- Ejecucion: `PUT` con `idCliente` inexistente.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: mensaje `Ocupante no encontrado` para cliente.
- Estado: PASS.

### TC10 - Rechazo por acompanante invalido
- Objetivo: validar error cuando se envia acompanante inexistente.
- Ejecucion: `PUT` con `idAcompanantes` invalidos.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: mensaje `Ocupante no encontrado` para acompanante.
- Estado: PASS.

### TC11 - Validacion DTO en activarEstancia
- Objetivo: validar respuesta de `@Valid` para campos obligatorios del request.
- Ejecucion: `PUT` omitiendo campo requerido en `ActivarEstanciaDTO`.
- Esperado: `400` con `VALIDATION_ERROR`.
- Obtenido: `400`.
- Evidencia: detalle de campo obligatorio faltante en body de error.
- Estado: PASS.

### TC12 - Registro de evento y transicion de estados
- Objetivo: validar efectos colaterales de activacion (evento + cambios de estado).
- Ejecucion: activar una estancia valida y consultar trazabilidad/resultados asociados.
- Esperado:
- se registra evento `ACTIVACION_ESTANCIA`;
- reserva transita a `COMPLETADA`;
- estancia queda en `ACTIVA` o `EXCEDIDA` segun fechas.
- Obtenido: comportamiento observado segun esperado.
- Evidencia: evento de activacion presente y estados actualizados en respuestas.
- Estado: PASS.

## Smoke UI (/estancias/nueva)

### UI-01 Activacion exitosa desde flujo con idReserva
- Flujo:
- abrir `/estancias/nueva?...idReserva=48` con unidad y datos precargados;
- diligenciar fechas/notas y activar estancia;
- confirmar notificacion y redireccion.
- Resultado observado:
- request `PUT /backend/estancias/activar` exitoso;
- mensaje visible `Estancia activada con exito.`;
- redireccion a `/recepcion`.
- Estado: PASS.

### UI-02 Error de negocio mostrado en UI (entrada futura)
- Flujo:
- abrir `/estancias/nueva?...idReserva=49`;
- enviar activacion con `entradaReal` futura.
- Resultado observado:
- request `PUT /backend/estancias/activar` con `400`;
- mensaje visible con texto de validacion de entrada futura.
- Estado: PASS.

## Observaciones

- El ambiente presentaba alta carga de estancias activas/excedidas; para aislar pruebas de activacion en `APT-201-H1` fue necesario limpiar casos previos con `PUT /backend/estancias/eliminar/{id}`.
- El frontend propaga correctamente mensajes de negocio del backend en flujo de activacion.

## Conclusion

- El servicio `activarEstancia` y su consumo desde frontend quedaron validados para camino feliz, precondiciones de estado, reglas de fecha, disponibilidad, validacion DTO y resolucion de ocupantes.
- Estado final: `14 pruebas ejecutadas`, `14 aprobadas`, `0 fallidas`.
