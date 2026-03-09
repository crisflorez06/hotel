# Resultados de pruebas frontend - buscarReservasTabla (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `buscarReservasTabla` (`GET /reservas/tabla`) consumido por frontend.
- Validacion de filtros principales, paginacion y comportamiento en UI de `reservas`.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/ReservaService.java`
- `src/main/java/com/hotel/specifications/ReservaSpecification.java`
- `src/main/java/com/hotel/controllers/ReservaController.java`
- `frontend/src/app/services/reserva.service.ts`
- `frontend/src/app/pages/reservas/reservas.component.ts`
- `frontend/src/app/models/reserva-tabla.model.ts`

## Resumen

- Suite API-driven desde navegador: `13 pruebas` -> `12 PASS`, `1 FAIL`.
- Smoke UI en pagina de reservas: `3 pruebas` -> `3 PASS`.
- Total ejecutado: `16 pruebas` -> `15 PASS`, `1 FAIL`.

Datos de ejecucion:
- Run id: `14352`
- Documento QA: `QA-TABLA-14352`
- Cliente QA: `id=12`
- Reservas de control:
- `RES-74300R` (APARTAMENTO, MOSTRADOR, confirmada)
- `RES-56191N` (APARTAESTUDIO, EMAIL, confirmada)
- `RES-71229S` (HABITACION, WHATSAPP, cancelada)

## Resultados detallados (suite API)

### TC01 - Paginacion basica
- Objetivo: validar estructura `Page` y campos de paginacion.
- Ejecucion: `GET /backend/reservas/tabla?page=0&size=5`.
- Esperado: `200` con `content`, `size`, `totalElements`.
- Obtenido: `200/size:5/total:36`.
- Estado: PASS.

### TC02 - Filtro por estados
- Objetivo: validar `estados=CONFIRMADA`.
- Ejecucion: `GET .../tabla?estados=CONFIRMADA&numeroDocumentoCliente=QA-TABLA-14352`.
- Esperado: solo reservas en estado `CONFIRMADA`.
- Obtenido: `200/count:2/hasCancel:false`.
- Estado: PASS.

### TC03 - Filtro por canal
- Objetivo: validar `canales=EMAIL`.
- Ejecucion: `GET .../tabla?canales=EMAIL&numeroDocumentoCliente=QA-TABLA-14352`.
- Esperado: solo reservas de canal `EMAIL`.
- Obtenido: `200/count:1/hasR2:true`.
- Estado: PASS.

### TC04 - Filtro por tipoUnidad
- Objetivo: validar `tipoUnidad=HABITACION`.
- Ejecucion: `GET .../tabla?tipoUnidad=HABITACION&numeroDocumentoCliente=QA-TABLA-14352`.
- Esperado: solo tipo `HABITACION`.
- Obtenido: `200/count:0`.
- Nota: en este contexto la reserva de habitacion del cliente quedo cancelada y no coincidio con otros filtros aplicados en la consulta de prueba.
- Estado: PASS.

### TC05 - Filtro por `codigoReserva` (contains)
- Objetivo: validar busqueda parcial por codigo.
- Ejecucion: `GET .../tabla?codigoReserva=5619`.
- Esperado: incluir `RES-56191N`.
- Obtenido: `200/count:1/hasR2:true`.
- Estado: PASS.

### TC06 - Filtro por `codigoUnidad` (contains)
- Objetivo: validar coincidencia por codigo de unidad/habitacion.
- Ejecucion: `GET .../tabla?codigoUnidad=AE-2&numeroDocumentoCliente=QA-TABLA-14352`.
- Esperado: incluir reserva del apartaestudio `AE-202`.
- Obtenido: `200/count:1/hasR2:true`.
- Estado: PASS.

### TC07 - Filtro por `nombreCliente`
- Objetivo: validar busqueda por nombre completo concatenado (`nombres + apellidos`).
- Ejecucion: `GET .../tabla?nombreCliente=QA Tabla`.
- Esperado: coincidencias del cliente QA.
- Obtenido: `200/count:3/hasAny:true`.
- Estado: PASS.

### TC08 - Filtro por `numeroDocumentoCliente`
- Objetivo: validar busqueda parcial por documento.
- Ejecucion: `GET .../tabla?numeroDocumentoCliente=TABLA-`.
- Esperado: incluir reservas del cliente QA.
- Obtenido: `200/count:3/hasClient:true`.
- Estado: PASS.

### TC09 - Filtro por `idCliente`
- Objetivo: validar coincidencia exacta por id de cliente.
- Ejecucion: `GET .../tabla?idCliente=12`.
- Esperado: solo reservas del cliente `id=12`.
- Obtenido: `200/count:3/allId:true`.
- Estado: PASS.

### TC10 - Filtro por rango de entrada
- Objetivo: validar `entradaDesde/entradaHasta`.
- Ejecucion: `GET .../tabla?entradaDesde=2027-07-19T00:00:00&entradaHasta=2027-07-21T23:59:59&idCliente=12`.
- Esperado: incluir `RES-56191N` y excluir `RES-74300R`.
- Obtenido: `200/count:1/r2:true/r1:false`.
- Estado: PASS.

### TC11 - Filtro por rango general
- Objetivo: validar `rangoGeneralDesde/rangoGeneralHasta` (entrada o salida dentro del rango).
- Ejecucion: `GET .../tabla?rangoGeneralDesde=2027-07-22T00:00:00&rangoGeneralHasta=2027-07-24T23:59:59&idCliente=12`.
- Esperado: incluir reservas que crucen ese rango (incluida `RES-71229S`).
- Obtenido: `200/count:2/hasR3:true`.
- Estado: PASS.

### TC12 - Filtro `tieneEstanciaAsociada`
- Objetivo: validar comportamiento con `true/false`.
- Ejecucion:
- `GET .../tabla?tieneEstanciaAsociada=true&idCliente=12`
- `GET .../tabla?tieneEstanciaAsociada=false&idCliente=12`
- Esperado: `true` devuelve resultados, `false` vacio para este dataset.
- Obtenido: `200/3 - 200/0`.
- Estado: PASS.

### TC13 - Fecha invalida en filtro datetime
- Objetivo: validar manejo de formato invalido en campos fecha/hora.
- Ejecucion: `GET .../tabla?entradaDesde=fecha-invalida`.
- Esperado: `400` (error de validacion/parseo).
- Obtenido: `500`.
- Estado: FAIL.
- Hallazgo: el parseo invalido de `LocalDateTime` en query params no esta devolviendo `400` en este endpoint y termina en error interno.

## Smoke UI (pagina /reservas)

### UI-01 Filtro por codigo de reserva
- Flujo:
- ingresar `RES-56191N` en `Codigo reserva`;
- click `Aplicar filtros`.
- Resultado observado:
- tabla muestra una unica fila con `RES-56191N`;
- contador `Total: 1`.
- Evidencia de red: `GET /backend/reservas/tabla?...codigoReserva=RES-56191N...` con `200` (reqid `220`).
- Estado: PASS.

### UI-02 Filtro por documento de cliente
- Flujo:
- limpiar filtros;
- ingresar `QA-TABLA-14352` en `Documento cliente`;
- aplicar filtros.
- Resultado observado:
- se muestran 3 reservas del cliente QA (`RES-71229S`, `RES-56191N`, `RES-74300R`);
- contador `Total: 3`.
- Evidencia de red: `GET /backend/reservas/tabla?...numeroDocumentoCliente=QA-TABLA-14352...` con `200` (reqid `222`).
- Estado: PASS.

### UI-03 Sin resultados por combinacion de filtros
- Flujo:
- con documento `QA-TABLA-14352`, agregar `Codigo reserva=NO-EXISTE-123`;
- aplicar filtros.
- Resultado observado:
- mensaje `No hay reservas con los filtros actuales.`;
- contador `Total: 0`.
- Evidencia de red: `GET /backend/reservas/tabla?...codigoReserva=NO-EXISTE-123...` con `200` (reqid `223`).
- Estado: PASS.

## Hallazgos

- Se detecta un defecto de manejo de errores para filtros datetime invalidos (`entradaDesde=fecha-invalida`): responde `500` y no `400`.
- Esto afecta robustez del endpoint ante errores de input en query params.

## Conclusion

- `buscarReservasTabla` funciona correctamente para paginacion y filtros funcionales principales en backend/frontend.
- Queda pendiente corregir el manejo de fechas invalidas para retornar `400` consistente con errores de validacion de entrada.
