# Resultados de pruebas frontend - obtenerEstimacionPago (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `obtenerEstimacionPago` (`POST /pagos/total`) consumido por frontend.
- Casos construidos desde `PagoService.obtenerEstimacionPago` y `PagoResolver.calcularEstimacionPago`.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/PagoService.java`
- `src/main/java/com/hotel/resolvers/PagoResolver.java`
- `src/main/java/com/hotel/dtos/pago/CalcularPagoDTO.java`
- `src/main/java/com/hotel/controllers/PagoController.java`
- `frontend/src/app/services/pago.service.ts`
- `frontend/src/app/pages/recepcion-panel/recepcion-panel.component.ts`

## Resumen

- Suite API-driven desde navegador: `12 pruebas` -> `12 PASS`, `0 FAIL`.
- Smoke UI en `recepcion/panel`: `2 pruebas` -> `2 PASS`.
- Total ejecutado: `14 pruebas` -> `14 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id suite API: `34732`.
- Fixtures de apoyo (idEstancia `3`):
- `id=38` (`ANTICIPO_ESTANCIA`, monto `40000`) para validar descuento en estimacion.
- `id=39` (`ANTICIPO_ESTANCIA`, request con estado `PENDIENTE`) para validar estado persistido y efecto en estimacion.
- Limpieza post-suite: `PUT /pagos/eliminar/38` y `PUT /pagos/eliminar/39` (`204`).

## Resultados detallados (suite API)

### TC01 - Exito base sin idEstancia
- Objetivo: validar estimacion simple con datos minimos validos.
- Ejecucion: `POST /backend/pagos/total` con `tipoUnidad=APARTAESTUDIO`, `numeroPersonas=2`, rango de 1 dia.
- Esperado: `200`.
- Obtenido: `200`.
- Evidencia: total estimado `230000`.
- Estado: PASS.

### TC02 - Personas adicionales incrementan estimacion
- Objetivo: validar recargo por personas adicionales (>2).
- Ejecucion: mismo request de TC01 pero `numeroPersonas=3`.
- Esperado: total mayor al caso base.
- Obtenido: incremento correcto.
- Evidencia: `230000` -> `315000`.
- Estado: PASS.

### TC03 - Error por fechaEntrada no anterior a fechaSalida
- Objetivo: validar regla temporal de rango valido.
- Ejecucion: `fechaEntrada == fechaSalida`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `La fecha de entrada debe ser anterior a la salida`.
- Estado: PASS.

### TC04 - Error por tipoUnidad faltante
- Objetivo: documentar comportamiento actual sin `tipoUnidad`.
- Ejecucion: request sin `tipoUnidad`.
- Esperado: `404` (comportamiento actual observado).
- Obtenido: `404`.
- Evidencia: `TarifaBase no encontrada para tipo unidad: null`.
- Estado: PASS.

### TC05 - Error por fechaEntrada faltante
- Objetivo: documentar manejo actual cuando falta `fechaEntrada`.
- Ejecucion: request sin `fechaEntrada`.
- Esperado: `500` (comportamiento actual observado).
- Obtenido: `500`.
- Evidencia: `Ocurrió un error inesperado.`.
- Estado: PASS.

### TC06 - Error por idEstancia inexistente
- Objetivo: validar resolucion de estancia en `idEstancia` invalido.
- Ejecucion: request valido con `idEstancia=99999999`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `No se encontró una estancia con id: 99999999`.
- Estado: PASS.

### TC07 - Anticipo COMPLETADO reduce estimacion (idEstancia=3)
- Objetivo: validar descuento de anticipos completados en la estimacion.
- Ejecucion:
- calcular total con `idEstancia=3`;
- crear pago `ANTICIPO_ESTANCIA` completado por `40000`;
- recalcular total.
- Esperado: nueva estimacion menor en aprox `40000`.
- Obtenido: descuento correcto.
- Evidencia: `900000` -> `860000` (`delta=40000`).
- Estado: PASS.

### TC08 - Request con estado PENDIENTE en ANTICIPO_ESTANCIA se persiste como COMPLETADO
- Objetivo: validar comportamiento real de `crearPago` usado como precondicion de estimacion.
- Ejecucion:
- crear `ANTICIPO_ESTANCIA` enviando `estado=PENDIENTE` (monto `60000`);
- recalcular estimacion con `idEstancia=3`;
- verificar estado persistido del pago creado.
- Esperado: pago queda `COMPLETADO` y afecta estimacion.
- Obtenido: cumple.
- Evidencia: estimacion `860000` -> `800000` (`delta=60000`), pago `id=39` con `estadoPersistido=COMPLETADO`.
- Estado: PASS.

### TC09 - Error por tipoUnidad invalido
- Objetivo: documentar comportamiento ante enum invalido.
- Ejecucion: `tipoUnidad=INVALIDO`.
- Esperado: `500` (comportamiento actual observado).
- Obtenido: `500`.
- Evidencia: `Ocurrió un error inesperado.`.
- Estado: PASS.

### TC10 - Error por fecha invalida
- Objetivo: documentar manejo de formato temporal invalido.
- Ejecucion: `fechaEntrada=fecha-invalida`.
- Esperado: `500`.
- Obtenido: `500`.
- Evidencia: `Ocurrió un error inesperado.`.
- Estado: PASS.

### TC11 - Rango en el mismo dia retorna 0
- Objetivo: validar calculo por dias completos (`ChronoUnit.DAYS` sobre fecha).
- Ejecucion: entrada/salida en la misma fecha con horas distintas.
- Esperado: total `0`.
- Obtenido: `0`.
- Evidencia: respuesta `200` con `0`.
- Estado: PASS.

### TC12 - Mayor rango de dias incrementa total
- Objetivo: validar proporcionalidad por cantidad de dias.
- Ejecucion: comparar estimacion para 1 dia vs 2 dias.
- Esperado: total de 2 dias mayor al de 1 dia.
- Obtenido: incremento correcto.
- Evidencia: `230000` -> `460000`.
- Estado: PASS.

## Smoke UI (recepcion/panel)

### UI-01 Calculo exitoso en modal "Agregar pago"
- Flujo:
- abrir `/recepcion/panel?codigo=AE-302&tipo=APARTAESTUDIO`;
- click `Agregar pago` para abrir modal.
- Resultado observado:
- request `POST /backend/pagos/total` con `200` (reqid `1217`);
- bloque `TOTAL CALCULADO` visible en modal con `1,930,000`.
- Estado: PASS.

### UI-02 Recalculo en UI tras registrar un anticipo adicional
- Flujo:
- registrar por API un anticipo completado de `100000` para `idEstancia=3` (`idPago=40`);
- reabrir modal `Agregar pago`.
- Resultado observado:
- nuevo request `POST /backend/pagos/total` con `200` (reqid `1219`);
- `TOTAL CALCULADO` baja de `1,930,000` a `1,830,000` (delta `100000`).
- Estado: PASS.

## Observaciones

- El endpoint no usa `@Valid` en controlador (`/pagos/total`), por lo que faltantes/formato invalido terminan en errores no uniformes (`404`/`500`) en vez de respuestas de validacion estandarizadas.
- Para `ANTICIPO_ESTANCIA`, el estado enviado en request no se conserva: el mapper lo fija en `COMPLETADO`; esto impacta la estimacion al descontar esos pagos.
- El calculo de dias usa diferencia por fecha (no horas), por eso rangos dentro del mismo dia retornan `0`.

## Conclusion

- El metodo `obtenerEstimacionPago` quedó validado para caminos felices, sensibilidad por personas/dias, efecto de anticipos sobre estimacion y consumo UI real en modal de pagos.
- Estado final: `14 pruebas ejecutadas`, `14 aprobadas`, `0 fallidas`.
