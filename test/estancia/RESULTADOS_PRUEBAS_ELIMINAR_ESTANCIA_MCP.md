# Resultados de pruebas frontend - eliminarEstancia (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `eliminarEstancia` (`PUT /estancias/eliminar/{id}`) consumido por frontend.
- Casos construidos desde reglas de `EstanciaService.eliminarEstancia` y efectos sobre pagos/unidad.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/EstanciaService.java`
- `src/main/java/com/hotel/services/PagoService.java`
- `src/main/java/com/hotel/controllers/EstanciaController.java`
- `src/main/java/com/hotel/controllers/advice/ApiExceptionHandler.java`
- `frontend/src/app/services/estancia.service.ts`
- `frontend/src/app/pages/estancias/estancias.component.ts`
- `frontend/src/app/pages/recepcion-panel/recepcion-panel.component.ts`

## Resumen

- Suite API-driven desde navegador: `12 pruebas` -> `12 PASS`, `0 FAIL`.
- Smoke UI de eliminacion: `2 pruebas` -> `2 PASS`.
- Total ejecutado: `14 pruebas` -> `14 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id suite API: `55483`.
- Estancia objetivo suite API: `id=4`, `codigoEstancia=EST-56362K`, `codigoUnidad=APT-201-H2`, `tipoUnidad=HABITACION`, estado inicial `ACTIVA`.
- Referencia para estado no permitido: `id=49` (`FINALIZADA`).

## Resultados detallados (suite API)

### TC01 - Error por idEstancia inexistente
- Objetivo: validar manejo de id no existente.
- Ejecucion: `PUT /backend/estancias/eliminar/99999999`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `Estancia no encontrada con id: 99999999`.
- Estado: PASS.

### TC02 - Error por estado no permitido (FINALIZADA)
- Objetivo: validar regla de negocio (solo `ACTIVA` o `EXCEDIDA`).
- Preparacion: usar estancia `id=49` en `FINALIZADA`.
- Ejecucion: `PUT /backend/estancias/eliminar/49`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `Solo se pueden eliminar estancias en estado ACTIVA o EXCEDIDA. Estado actual: FINALIZADA`.
- Estado: PASS.

### TC03 - Creacion de pago previo para estancia activa
- Objetivo: preparar evidencia de efecto colateral sobre pagos al eliminar.
- Ejecucion: `POST /backend/pagos?idEstancia=4` con `tipoPago=ANTICIPO_ESTANCIA`, `monto=45000`, `estado=COMPLETADO`.
- Esperado: `200/201`.
- Obtenido: `200`.
- Evidencia: pago creado `id=14`, `codigoEstancia=EST-56362K`.
- Estado: PASS.

### TC04 - Verificacion de pago activo antes de eliminar
- Objetivo: confirmar existencia de pago asociado antes de borrar la estancia.
- Ejecucion: `GET /backend/pagos?codigoEstancia=EST-56362K`.
- Esperado: pago existente en estado distinto de `ELIMINADO`.
- Obtenido: `total=1`, estado `COMPLETADO`.
- Evidencia: registro de pago visible para `EST-56362K`.
- Estado: PASS.

### TC05 - Eliminacion exitosa de estancia activa
- Objetivo: validar flujo feliz de eliminacion.
- Ejecucion: `PUT /backend/estancias/eliminar/4`.
- Esperado: `204`.
- Obtenido: `204`.
- Evidencia: respuesta `No Content`.
- Estado: PASS.

### TC06 - Reintento sobre estancia CANCELADA
- Objetivo: validar que no se pueda eliminar dos veces.
- Ejecucion: repetir `PUT /backend/estancias/eliminar/4`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `Solo se pueden eliminar estancias en estado ACTIVA o EXCEDIDA. Estado actual: CANCELADA`.
- Estado: PASS.

### TC07 - Verificacion de estado CANCELADA en tabla
- Objetivo: validar cambio de estado de la estancia eliminada.
- Ejecucion: `GET /backend/estancias/tabla?estados=CANCELADA` y buscar `id=4`.
- Esperado: estancia presente en `CANCELADA`.
- Obtenido: encontrada.
- Evidencia: `foundCancelada=true`.
- Estado: PASS.

### TC08 - Verificacion de unidad liberada (DISPONIBLE)
- Objetivo: validar actualización operativa de la unidad/habitacion al eliminar.
- Ejecucion: `GET /backend/recepcion/habitaciones?estado=DISPONIBLE` y buscar `APT-201-H2`.
- Esperado: unidad disponible.
- Obtenido: disponible.
- Evidencia: `disponible=true` para `APT-201-H2`.
- Estado: PASS.

### TC09 - Consulta de estancia activa tras eliminar
- Objetivo: validar que ya no exista estancia activa/excedida para la unidad.
- Ejecucion: `GET /backend/estancias/activa?codigo=APT-201-H2&tipoUnidad=HABITACION`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `No se encontró una estancia activa o excedida para la unidad con codigo: APT-201-H2`.
- Estado: PASS.

### TC10 - Verificacion de pagos marcados como ELIMINADO
- Objetivo: validar efecto de `pagoService.eliminarTodoLosPagos` durante eliminacion.
- Ejecucion: `GET /backend/pagos?codigoEstancia=EST-56362K` tras eliminar.
- Esperado: pagos asociados en estado `ELIMINADO`.
- Obtenido: `total=1`, estado `ELIMINADO`.
- Evidencia: pago `id=14` con `estado=ELIMINADO`.
- Estado: PASS.

### TC11 - Path variable no numerica
- Objetivo: documentar comportamiento del endpoint ante id no numerico.
- Ejecucion: `PUT /backend/estancias/eliminar/abc`.
- Esperado: `500` (comportamiento actual observado).
- Obtenido: `500`.
- Evidencia: `Ocurrió un error inesperado.`.
- Estado: PASS.

### TC12 - Consistencia de regla en estado FINALIZADA
- Objetivo: revalidar bloqueo para estados no permitidos.
- Ejecucion: nuevo `PUT /backend/estancias/eliminar/49`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: mismo mensaje de restriccion por estado.
- Estado: PASS.

## Smoke UI (pantalla de estancias)

### UI-01 Eliminacion exitosa desde lista de estancias
- Flujo:
- abrir `/estancias`;
- seleccionar estancia activa `EST-91651J`;
- click en `Eliminar` y confirmar dialogo.
- Resultado observado:
- request `PUT /backend/estancias/eliminar/5` con `204` (reqid `504`);
- recarga de tabla con mensaje visible `Estancia eliminada correctamente.`.
- Estado: PASS.

### UI-02 Bloqueo de eliminacion para estado no permitido
- Flujo:
- en `/estancias`, seleccionar una estancia `CANCELADA`.
- Resultado observado:
- boton `Eliminar` queda deshabilitado;
- tooltip/mensaje visible: `No se puede eliminar una estancia finalizada o cancelada.`;
- no se genera request `PUT` para este intento bloqueado.
- Estado: PASS.

## Observaciones

- `eliminarEstancia` no borra fisicamente los pagos: los marca como `ELIMINADO`, lo cual es consistente con auditoria y trazabilidad.
- El caso de path no numerico (`/eliminar/abc`) responde `500` via fallback global; es un hallazgo de manejo de errores que podria normalizarse a `400`.

## Conclusion

- El flujo `eliminarEstancia` quedo validado en frontend para camino feliz, reglas de estado, efectos en disponibilidad de unidad y tratamiento de pagos asociados.
- Estado final: `14 pruebas ejecutadas`, `14 aprobadas`, `0 fallidas`.
