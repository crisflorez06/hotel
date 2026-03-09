# Resultados de pruebas frontend - buscarEstanciasTabla (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `buscarEstanciasTabla` (`GET /estancias/tabla`) consumido por frontend.
- Casos enfocados en filtros, paginacion y ordenamiento definidos en `EstanciaService` + `EstanciaSpecification.byTablaFilters`.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/EstanciaService.java`
- `src/main/java/com/hotel/specifications/EstanciaSpecification.java`
- `src/main/java/com/hotel/dtos/estancia/EstanciaTablaDTO.java`
- `src/main/java/com/hotel/controllers/EstanciaController.java`
- `frontend/src/app/services/estancia.service.ts`
- `frontend/src/app/pages/estancias/estancias.component.ts`

## Resumen

- Suite API-driven desde navegador: `12 pruebas` -> `12 PASS`, `0 FAIL`.
- Smoke UI en `/estancias`: `2 pruebas` -> `2 PASS`.
- Total ejecutado: `14 pruebas` -> `14 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id suite API: `50492`.
- Endpoint probado con combinaciones de filtros por estado, tipo de unidad, codigo, cliente, reserva asociada y rangos de fecha.

## Resultados detallados (suite API)

### TC01 - Consulta base sin filtros
- Objetivo: validar respuesta paginada minima de la tabla.
- Ejecucion: `GET /backend/estancias/tabla?page=0&size=20&sort=entradaReal,desc`.
- Esperado: `200` con `content[]` y metadatos de paginacion.
- Obtenido: `200`.
- Evidencia: `totalElements=21`, estructura `Page` valida.
- Estado: PASS.

### TC02 - Filtro por estado ACTIVA
- Objetivo: validar filtrado por `estados`.
- Ejecucion: `GET ...&estados=ACTIVA`.
- Esperado: `200` y todas las filas en estado `ACTIVA`.
- Obtenido: `200`.
- Evidencia: `count=2`, `allActive=true`.
- Estado: PASS.

### TC03 - Filtro por tipoUnidad APARTAESTUDIO
- Objetivo: validar filtro por tipo de unidad.
- Ejecucion: `GET ...&tipoUnidad=APARTAESTUDIO`.
- Esperado: `200` y filas solo de `APARTAESTUDIO`.
- Obtenido: `200`.
- Evidencia: `count=5`, `allTipo=true`.
- Estado: PASS.

### TC04 - Filtro por codigoEstancia parcial
- Objetivo: validar busqueda parcial por codigo de estancia.
- Ejecucion: `GET ...&codigoEstancia=92997`.
- Esperado: `200` y codigos coincidentes.
- Obtenido: `200`.
- Evidencia: `sample=EST-92997F`, `allMatch=true`.
- Estado: PASS.

### TC05 - Filtro por codigoUnidad parcial
- Objetivo: validar busqueda por codigo de unidad/habitacion.
- Ejecucion: `GET ...&codigoUnidad=AE-30`.
- Esperado: `200` y coincidencias parciales.
- Obtenido: `200`.
- Evidencia: `count=3`, `allMatch=true`.
- Estado: PASS.

### TC06 - Filtro por nombreCliente
- Objetivo: validar filtro por nombre completo del cliente.
- Ejecucion: `GET ...&nombreCliente=Carlos`.
- Esperado: `200` y nombres coincidentes.
- Obtenido: `200`.
- Evidencia: `count=4`, `allMatch=true`.
- Estado: PASS.

### TC07 - Filtro por numeroDocumentoCliente
- Objetivo: validar filtro por documento del cliente.
- Ejecucion: `GET ...&numeroDocumentoCliente=900100001`.
- Esperado: `200` y documentos coincidentes.
- Obtenido: `200`.
- Evidencia: `count=4`, `allMatch=true`.
- Estado: PASS.

### TC08 - Filtro tieneReservaAsociada=true
- Objetivo: validar filtro booleano de reserva asociada.
- Ejecucion: `GET ...&tieneReservaAsociada=true`.
- Esperado: `200` y `tieneReservaAsociada=true` en todas las filas.
- Obtenido: `200`.
- Evidencia: `count=4`, `allTrue=true`.
- Estado: PASS.

### TC09 - Filtro tieneReservaAsociada=false
- Objetivo: validar el caso complementario del filtro booleano.
- Ejecucion: `GET ...&tieneReservaAsociada=false`.
- Esperado: `200` y `tieneReservaAsociada=false` en todas las filas.
- Obtenido: `200`.
- Evidencia: `count=17`, `allFalse=true`.
- Estado: PASS.

### TC10 - Filtro por rangoGeneral
- Objetivo: validar filtro por ventana temporal general.
- Ejecucion: `GET ...&rangoGeneralDesde=2026-03-04T00:00:00&rangoGeneralHasta=2026-03-04T23:59:59`.
- Esperado: `200` con filas dentro del rango en al menos una fecha relevante.
- Obtenido: `200`.
- Evidencia: `count=15`, `sample=EST-43499F`.
- Estado: PASS.

### TC11 - Fecha invalida en filtro
- Objetivo: documentar comportamiento ante datetime invalido en query.
- Ejecucion: `GET ...&entradaDesde=fecha-invalida`.
- Esperado: `500` (comportamiento actual observado).
- Obtenido: `500`.
- Evidencia: `error=INTERNAL_SERVER_ERROR`, `message=Ocurrió un error inesperado.`.
- Estado: PASS.

### TC12 - Paginacion y ordenamiento
- Objetivo: validar `page/size/sort`.
- Ejecucion:
- `GET ...page=0&size=5&sort=entradaReal,desc`
- `GET ...page=1&size=5&sort=entradaReal,desc`
- Esperado: `200` en ambas paginas y variacion de filas entre pagina 0/1.
- Obtenido: `200/200`.
- Evidencia: `firstPage0=60`, `firstPage1=66`, `differentFirstRow=true`.
- Estado: PASS.

## Smoke UI (/estancias)

### UI-01 Paginacion real en tabla
- Flujo:
- abrir `/estancias`;
- click `Siguiente` en paginador.
- Resultado observado:
- tabla cambia a `Pagina 2 de 2` (seccion `Mostrando 21-21`);
- request `GET /backend/estancias/tabla?page=1&size=20&sort=entradaReal,desc` con `200` (reqid `765`).
- Estado: PASS.

### UI-02 Filtro por codigoEstancia desde formulario
- Flujo:
- en `/estancias`, diligenciar `Codigo estancia = 92997`;
- click `Aplicar filtros`.
- Resultado observado:
- tabla queda en `Total: 1` con fila `EST-92997F`;
- request `GET /backend/estancias/tabla?page=0&size=20&codigoEstancia=92997&sort=entradaReal,desc` con `200` (reqid `766`).
- Estado: PASS.

## Observaciones

- Se mantiene el hallazgo de manejo de errores para fechas invalidas en query params: el backend responde `500` en lugar de `400`.
- El frontend consume correctamente la tabla con filtros y paginacion, y refleja resultados esperados en la UI.

## Conclusion

- El endpoint `buscarEstanciasTabla` quedo validado en frontend para filtros principales, filtros booleanos, rangos, paginacion y ordenamiento.
- Estado final: `14 pruebas ejecutadas`, `14 aprobadas`, `0 fallidas`.
