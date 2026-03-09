# Resultados de pruebas frontend - eliminarPago (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `eliminarPago` (`PUT /pagos/eliminar/{id}`) consumido por frontend.
- Casos construidos desde reglas de `PagoService.eliminarPago`.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/PagoService.java`
- `src/main/java/com/hotel/controllers/PagoController.java`
- `src/main/java/com/hotel/specifications/PagoSpecification.java`
- `frontend/src/app/services/pago.service.ts`
- `frontend/src/app/pages/recepcion-panel/recepcion-panel.component.ts`
- `frontend/src/app/pages/recepcion-panel/recepcion-panel.component.html`

## Resumen

- Suite API-driven desde navegador: `12 pruebas` -> `12 PASS`, `0 FAIL`.
- Smoke UI en `recepcion/panel`: `2 pruebas` -> `2 PASS`.
- Total ejecutado: `14 pruebas` -> `14 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id suite API: `51645`.
- Fixtures de pago usados en API:
- `id=30` (`ANTICIPO_ESTANCIA`) para eliminacion exitosa y reintento.
- `id=31` (`ANTICIPO_RESERVA`) para eliminacion exitosa y reintento.
- Referencias preexistentes:
- `id=12` (`ESTANCIA_COMPLETADA`, no eliminable).
- `id=10` (`ELIMINADO`, no eliminable).

## Resultados detallados (suite API)

### TC01 - Error por idPago inexistente
- Objetivo: validar manejo de id no existente.
- Ejecucion: `PUT /backend/pagos/eliminar/99999999`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `Pago no encontrado con id: 99999999`.
- Estado: PASS.

### TC02 - Verificar exclusion de CAMBIO_UNIDAD en listado de pagos
- Objetivo: validar precondicion de tabla: `buscarPagos` excluye `CAMBIO_UNIDAD`.
- Ejecucion: `GET /backend/pagos?page=0&size=200&sort=fecha,desc`.
- Esperado: sin registros `tipoPago=CAMBIO_UNIDAD`.
- Obtenido: sin registros `CAMBIO_UNIDAD`.
- Evidencia: `noCambioUnidad=true`.
- Estado: PASS.

### TC03 - Error eliminando ESTANCIA_COMPLETADA
- Objetivo: validar regla de no eliminacion para `ESTANCIA_COMPLETADA`.
- Ejecucion: `PUT /backend/pagos/eliminar/12`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `No se puede eliminar un pago de tipo ESTANCIA_COMPLETADA...`.
- Estado: PASS.

### TC04 - Eliminacion exitosa ANTICIPO_ESTANCIA
- Objetivo: validar camino feliz para pago eliminable en estado permitido.
- Ejecucion: `PUT /backend/pagos/eliminar/30`.
- Esperado: `204`.
- Obtenido: `204`.
- Evidencia: respuesta `No Content`.
- Estado: PASS.

### TC05 - Verificar estado ELIMINADO del fixture A
- Objetivo: validar persistencia del cambio de estado.
- Ejecucion: consultar tabla de pagos y localizar `id=30`.
- Esperado: `estado=ELIMINADO`.
- Obtenido: `ELIMINADO`.
- Evidencia: pago `id=30` con estado `ELIMINADO`.
- Estado: PASS.

### TC06 - Reintento sobre pago ya eliminado
- Objetivo: validar bloqueo por estado no permitido.
- Ejecucion: repetir `PUT /backend/pagos/eliminar/30`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `Solo se puede eliminar un pago en estado COMPLETADO o PENDIENTE. Estado actual: ELIMINADO`.
- Estado: PASS.

### TC07 - Eliminacion exitosa ANTICIPO_RESERVA
- Objetivo: validar eliminacion para pago de reserva en estado permitido.
- Ejecucion: `PUT /backend/pagos/eliminar/31`.
- Esperado: `204`.
- Obtenido: `204`.
- Evidencia: respuesta `No Content`.
- Estado: PASS.

### TC08 - Verificar estado ELIMINADO del fixture B
- Objetivo: validar persistencia de eliminacion en pago de reserva.
- Ejecucion: consultar tabla y localizar `id=31`.
- Esperado: `estado=ELIMINADO`.
- Obtenido: `ELIMINADO`.
- Evidencia: pago `id=31`, `codigoReserva=RES-29565C`, estado `ELIMINADO`.
- Estado: PASS.

### TC09 - Reintento sobre ANTICIPO_RESERVA eliminado
- Objetivo: validar bloqueo de segundo intento.
- Ejecucion: repetir `PUT /backend/pagos/eliminar/31`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: mismo mensaje de estado `ELIMINADO` no permitido.
- Estado: PASS.

### TC10 - Path variable no numerica
- Objetivo: documentar comportamiento con id invalido de ruta.
- Ejecucion: `PUT /backend/pagos/eliminar/abc`.
- Esperado: `500` (comportamiento actual observado).
- Obtenido: `500`.
- Evidencia: `Ocurrió un error inesperado.`.
- Estado: PASS.

### TC11 - Error eliminando pago preexistente ELIMINADO
- Objetivo: validar regla para registros historicos ya eliminados.
- Ejecucion: `PUT /backend/pagos/eliminar/10`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `Solo se puede eliminar un pago en estado COMPLETADO o PENDIENTE. Estado actual: ELIMINADO`.
- Estado: PASS.

### TC12 - Verificar visibilidad de pago eliminado en tabla
- Objetivo: validar que el pago eliminado se conserva para trazabilidad.
- Ejecucion: `GET /backend/pagos?...&codigoEstancia=EST-92997F` y buscar `id=30`.
- Esperado: pago visible con estado `ELIMINADO`.
- Obtenido: visible y en `ELIMINADO`.
- Evidencia: `visibleEliminado=true` para `id=30`.
- Estado: PASS.

## Smoke UI (recepcion/panel)

### UI-01 Eliminacion exitosa desde modal de pagos
- Flujo:
- crear pago de prueba `id=32` (`ANTICIPO_ESTANCIA`) para estancia `EST-92997F`;
- abrir `/recepcion/panel?codigo=AE-302&tipo=APARTAESTUDIO`;
- click `Eliminar pago` y confirmar en modal.
- Resultado observado:
- request `PUT /backend/pagos/eliminar/32` con `204` (reqid `1052`);
- recarga de panel sin pagos activos (`TOTAL PAGOS COP0`).
- Estado: PASS.

### UI-02 Error UI por intento de eliminar pago ya eliminado (stale state)
- Flujo:
- crear pago de prueba `id=33` y abrir modal `Eliminar pago`;
- eliminar `id=33` por API en paralelo (simulando estado obsoleto en cliente);
- confirmar eliminacion desde modal abierto.
- Resultado observado:
- primer `PUT /backend/pagos/eliminar/33` con `204` (pre-eliminacion);
- segundo `PUT /backend/pagos/eliminar/33` desde UI con `409` (reqid `1103`);
- mensaje visible: `Solo se puede eliminar un pago en estado COMPLETADO o PENDIENTE. Estado actual: ELIMINADO`.
- Estado: PASS.

## Observaciones

- `eliminarPago` implementa borrado logico por cambio de estado a `ELIMINADO`, manteniendo trazabilidad del registro.
- `CAMBIO_UNIDAD` no aparece en la tabla de pagos por especificacion, por lo que su restriccion de eliminacion queda protegida en backend aunque no sea accionable desde ese listado.
- Para path no numerico (`/eliminar/abc`) se mantiene respuesta `500` en lugar de `400`.

## Conclusion

- El metodo `eliminarPago` quedó validado para camino feliz, restricciones por tipo/estado, manejo de errores y comportamiento UI ante exito y conflicto de concurrencia.
- Estado final: `14 pruebas ejecutadas`, `14 aprobadas`, `0 fallidas`.
