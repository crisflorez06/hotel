# Resultados de pruebas frontend - buscarPagos (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `buscarPagos` (`GET /pagos`) consumido por frontend.
- Casos construidos desde `PagoService.buscarPagos` y `PagoSpecification.byFilters`.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/PagoService.java`
- `src/main/java/com/hotel/specifications/PagoSpecification.java`
- `src/main/java/com/hotel/controllers/PagoController.java`
- `src/main/java/com/hotel/dtos/pago/PagoDTO.java`
- `frontend/src/app/services/pago.service.ts`
- `frontend/src/app/pages/pagos/pagos.component.ts`

## Resumen

- Suite API-driven desde navegador: `12 pruebas` -> `12 PASS`, `0 FAIL`.
- Smoke UI en `/pagos`: `2 pruebas` -> `2 PASS`.
- Total ejecutado: `14 pruebas` -> `14 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id suite API: `34957`.
- Fixtures API creados para validar filtros:
- `id=24` (`ANTICIPO_ESTANCIA`, `COMPLETADO`, `EFECTIVO`, `monto=11111`).
- `id=25` (`ANTICIPO_ESTANCIA`, `COMPLETADO`, `TARJETA_CREDITO`, `monto=22222`).
- `id=26` (`ANTICIPO_RESERVA`, `COMPLETADO`, `TRANSFERENCIA_BANCARIA`, `monto=33333`).
- Limpieza post-suite: `PUT /pagos/eliminar/{id}` sobre `24`, `25`, `26` (todos `204`).

## Resultados detallados (suite API)

### TC01 - Consulta base y exclusion de CAMBIO_UNIDAD
- Objetivo: validar respuesta paginada y regla de exclusion de pagos internos `CAMBIO_UNIDAD`.
- Ejecucion: `GET /backend/pagos?page=0&size=100&sort=fecha,desc`.
- Esperado: `200`, estructura de pagina valida y sin `CAMBIO_UNIDAD`.
- Obtenido: `200`.
- Evidencia: `hasPage=true`, `noCambioUnidad=true`.
- Estado: PASS.

### TC02 - Filtro por estado COMPLETADO
- Objetivo: validar `estados=COMPLETADO`.
- Ejecucion: `GET ...&estados=COMPLETADO`.
- Esperado: `200` y filas solo `COMPLETADO`.
- Obtenido: `200`.
- Evidencia: `allCompleted=true`, fixture `id=24` presente.
- Estado: PASS.

### TC03 - Filtro por estado PENDIENTE
- Objetivo: validar `estados=PENDIENTE`.
- Ejecucion: `GET ...&estados=PENDIENTE`.
- Esperado: `200` y filas solo `PENDIENTE`.
- Obtenido: `200`.
- Evidencia: `allPending=true`.
- Estado: PASS.

### TC04 - Filtro por medio de pago EFECTIVO
- Objetivo: validar `mediosPago=EFECTIVO`.
- Ejecucion: `GET ...&mediosPago=EFECTIVO`.
- Esperado: `200` y filas con medio `EFECTIVO`.
- Obtenido: `200`.
- Evidencia: `allEfectivo=true`, fixture `id=24` presente.
- Estado: PASS.

### TC05 - Filtro por medio TARJETA_CREDITO
- Objetivo: validar `mediosPago=TARJETA_CREDITO`.
- Ejecucion: `GET ...&mediosPago=TARJETA_CREDITO`.
- Esperado: `200` y filas con medio `TARJETA_CREDITO`.
- Obtenido: `200`.
- Evidencia: `allTarjetaCredito=true`, fixture `id=25` presente.
- Estado: PASS.

### TC06 - Filtro por tipo de pago ANTICIPO_RESERVA
- Objetivo: validar `tipoPago=ANTICIPO_RESERVA`.
- Ejecucion: `GET ...&tipoPago=ANTICIPO_RESERVA`.
- Esperado: `200` y filas solo de ese tipo.
- Obtenido: `200`.
- Evidencia: `allAnticipoReserva=true`, fixture `id=26` presente.
- Estado: PASS.

### TC07 - Filtro por codigoEstancia
- Objetivo: validar `codigoEstancia` parcial/exacto.
- Ejecucion: `GET ...&codigoEstancia=EST-92997F`.
- Esperado: `200` y filas con `codigoEstancia` coincidente.
- Obtenido: `200`.
- Evidencia: `allCodigoEstanciaMatch=true`, fixture `id=24` presente.
- Estado: PASS.

### TC08 - Filtro por codigoReserva
- Objetivo: validar `codigoReserva` y regla adicional de `ANTICIPO_RESERVA`.
- Ejecucion: `GET ...&codigoReserva=RES-29565C`.
- Esperado: `200`, filas `ANTICIPO_RESERVA` con codigo de reserva coincidente.
- Obtenido: `200`.
- Evidencia: `allAnticipoReserva=true`, `allCodigoReservaMatch=true`, fixture `id=26` presente.
- Estado: PASS.

### TC09 - Filtro por fechaDesde
- Objetivo: validar limite inferior de fecha.
- Ejecucion: `GET ...&fechaDesde=2026-03-04T14:16:06`.
- Esperado: `200` y resultados desde esa fecha en adelante.
- Obtenido: `200`.
- Evidencia: fixture `id=25` e `id=26` incluidos; fixture `id=24` excluido.
- Estado: PASS.

### TC10 - Filtro por fechaHasta
- Objetivo: validar limite superior de fecha.
- Ejecucion: `GET ...&fechaHasta=2026-03-04T14:16:06`.
- Esperado: `200` y resultados hasta esa fecha.
- Obtenido: `200`.
- Evidencia: fixture `id=24` incluido; fixture `id=25` excluido.
- Estado: PASS.

### TC11 - Filtro combinado (tipo + estado + medio)
- Objetivo: validar interseccion de filtros.
- Ejecucion: `GET ...&tipoPago=ANTICIPO_ESTANCIA&estados=COMPLETADO&mediosPago=EFECTIVO`.
- Esperado: `200` y filas que cumplan las 3 condiciones.
- Obtenido: `200`.
- Evidencia: `allMatch=true`, fixture `id=24` presente.
- Estado: PASS.

### TC12 - Fecha invalida en query
- Objetivo: documentar comportamiento con fecha mal formada.
- Ejecucion: `GET ...&fechaDesde=fecha-invalida`.
- Esperado: `500` (comportamiento actual observado).
- Obtenido: `500`.
- Evidencia: `error=INTERNAL_SERVER_ERROR`, `message=Ocurrió un error inesperado.`.
- Estado: PASS.

## Smoke UI (/pagos)

### UI-01 Carga con filtro por query param codigoEstancia
- Flujo:
- abrir `/pagos?codigoEstancia=EST-92997F`.
- Resultado observado:
- el filtro `Codigo estancia` se precarga con `EST-92997F`;
- tabla muestra solo pagos de esa estancia (`Total: 5` en la corrida);
- request `GET /backend/pagos?page=0&size=10&sort=fecha,desc&codigoEstancia=EST-92997F` con `200` (reqid `944`).
- Estado: PASS.

### UI-02 Aplicar filtro de estado desde "Mas filtros"
- Flujo:
- en `/pagos?codigoEstancia=EST-92997F`, abrir `Mas filtros`;
- seleccionar estado `Eliminado` y aplicar.
- Resultado observado:
- request incluye `estados=ELIMINADO` y mantiene `codigoEstancia`;
- request `GET /backend/pagos?page=0&size=10&sort=fecha,desc&estados=ELIMINADO&codigoEstancia=EST-92997F` con `200` (reqid `946`);
- resultados listados en estado `Eliminado`.
- Estado: PASS.

## Observaciones

- La especificacion excluye siempre `CAMBIO_UNIDAD` de la tabla, aun sin filtros; se validó explicitamente.
- El filtro por fechas en frontend transforma a ISO (`toISOString`), y backend responde correctamente para fechas validas.
- Para fecha invalida en query, el comportamiento actual sigue siendo `500`.

## Conclusion

- El metodo `buscarPagos` quedó validado en frontend/backend para filtros de estado, medio, tipo, codigos, rango de fecha, combinaciones de filtros, paginacion y regla de exclusion de `CAMBIO_UNIDAD`.
- Estado final: `14 pruebas ejecutadas`, `14 aprobadas`, `0 fallidas`.
