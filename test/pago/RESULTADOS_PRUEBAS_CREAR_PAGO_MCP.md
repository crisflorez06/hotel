# Resultados de pruebas frontend - crearPago (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `crearPago` (`POST /pagos?idEstancia={id}`) consumido por frontend.
- Casos construidos desde reglas de `PagoService.crearPago` y `verificarPago`.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/PagoService.java`
- `src/main/java/com/hotel/controllers/PagoController.java`
- `src/main/java/com/hotel/dtos/pago/PagoNuevoRequestDTO.java`
- `src/main/java/com/hotel/models/enums/TipoPago.java`
- `frontend/src/app/services/pago.service.ts`
- `frontend/src/app/pages/recepcion-panel/recepcion-panel.component.ts`

## Resumen

- Suite API-driven desde navegador: `12 pruebas` -> `12 PASS`, `0 FAIL`.
- Smoke UI en panel de recepcion: `2 pruebas` -> `2 PASS`.
- Total ejecutado: `14 pruebas` -> `14 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id suite API: `34279`.
- Estancias de referencia:
- `ACTIVA`: `id=3`, `codigo=EST-92997F`.
- `EXCEDIDA`: `id=7`, `codigo=EST-42902E`.
- `CANCELADA`: `id=66`, `codigo=EST-53501D`.
- `FINALIZADA`: `id=49`, `codigo=EST-23512M`.
- `RESERVADA` usada para `ANTICIPO_RESERVA`: `id=68` (`codigoReserva=RES-29565C`).
- Limpieza post-suite: pagos creados en la corrida eliminados (`id=20`, `id=21`, `id=22`).

## Resultados detallados (suite API)

### TC01 - Exito ANTICIPO_ESTANCIA en estancia ACTIVA
- Objetivo: validar camino feliz para `tipoPago=ANTICIPO_ESTANCIA` con estado de estancia permitido.
- Ejecucion: `POST /backend/pagos?idEstancia=3`.
- Esperado: `200` y `codigoEstancia` informado.
- Obtenido: `200`.
- Evidencia: `id=20`, `codigoEstancia=EST-92997F`.
- Estado: PASS.

### TC02 - Exito ANTICIPO_ESTANCIA en estancia EXCEDIDA
- Objetivo: validar mismo tipo de pago en estado `EXCEDIDA`.
- Ejecucion: `POST /backend/pagos?idEstancia=7`.
- Esperado: `200`.
- Obtenido: `200`.
- Evidencia: `id=21`, `codigoEstancia=EST-42902E`.
- Estado: PASS.

### TC03 - Exito ANTICIPO_RESERVA en estancia RESERVADA
- Objetivo: validar camino feliz de `ANTICIPO_RESERVA` cuando existe reserva asociada.
- Ejecucion: `POST /backend/pagos?idEstancia=68`.
- Esperado: `200`.
- Obtenido: `200`.
- Evidencia: `id=22`, `codigoReserva=RES-29565C`, `codigoEstancia=null`.
- Estado: PASS.

### TC04 - Error ANTICIPO_RESERVA sin reserva asociada
- Objetivo: validar bloqueo para `ANTICIPO_RESERVA` sin reserva en la estancia.
- Ejecucion: `POST /backend/pagos?idEstancia=3` con `tipoPago=ANTICIPO_RESERVA`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `No se puede crear un pago de tipo ANTICIPO_RESERVA para una estancia que no tiene una reserva asociada`.
- Estado: PASS.

### TC05 - Error ANTICIPO_ESTANCIA en estancia CANCELADA
- Objetivo: validar regla de estado para `ANTICIPO_ESTANCIA`.
- Ejecucion: `POST /backend/pagos?idEstancia=66` con `tipoPago=ANTICIPO_ESTANCIA`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `No se puede crear un pago de tipo ANTICIPO_ESTANCIA para una estancia que no está activada... Estado actual: CANCELADA`.
- Estado: PASS.

### TC06 - Error ESTANCIA_COMPLETADA en estancia ACTIVA
- Objetivo: validar que `ESTANCIA_COMPLETADA` solo aplique a estancias `FINALIZADA`.
- Ejecucion: `POST /backend/pagos?idEstancia=3` con `tipoPago=ESTANCIA_COMPLETADA`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `No se puede crear un pago de tipo ESTANCIA_COMPLETADA para una estancia que no está FINALIZADA...`.
- Estado: PASS.

### TC07 - Error duplicado ESTANCIA_COMPLETADA
- Objetivo: validar bloqueo de duplicados para `ESTANCIA_COMPLETADA`.
- Ejecucion: `POST /backend/pagos?idEstancia=49` con `tipoPago=ESTANCIA_COMPLETADA`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `Ya existe un pago de tipo ESTANCIA_COMPLETADA para esta estancia, no se puede crear otro`.
- Estado: PASS.

### TC08 - Error por tipoPago no valido (CAMBIO_UNIDAD)
- Objetivo: validar rechazo de tipo no permitido en `crearPago`.
- Ejecucion: `POST /backend/pagos?idEstancia=3` con `tipoPago=CAMBIO_UNIDAD`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `El tipo de pago CAMBIO_UNIDAD no es válido para crear un pago`.
- Estado: PASS.

### TC09 - Validation error sin tipoPago
- Objetivo: validar `@Valid` en request DTO para `tipoPago` requerido.
- Ejecucion: `POST /backend/pagos?idEstancia=3` omitiendo `tipoPago`.
- Esperado: `400` con `VALIDATION_ERROR`.
- Obtenido: `400`.
- Evidencia: `fields.tipoPago = tipoPago es obligatorio`.
- Estado: PASS.

### TC10 - Validation error sin monto
- Objetivo: validar `@Valid` para `monto` requerido.
- Ejecucion: `POST /backend/pagos?idEstancia=3` omitiendo `monto`.
- Esperado: `400` con `VALIDATION_ERROR`.
- Obtenido: `400`.
- Evidencia: `fields.monto = monto es obligatorio`.
- Estado: PASS.

### TC11 - Error por idEstancia faltante en query param
- Objetivo: documentar comportamiento del endpoint cuando falta `idEstancia`.
- Ejecucion: `POST /backend/pagos` sin query param.
- Esperado: `500` (comportamiento actual observado).
- Obtenido: `500`.
- Evidencia: `Ocurrió un error inesperado.`.
- Estado: PASS.

### TC12 - Verificacion de mapeo de respuesta ANTICIPO_RESERVA
- Objetivo: validar mapeo de `PagoDTO` para estancias no activadas con reserva.
- Ejecucion: verificar respuesta de TC03.
- Esperado: `codigoReserva` informado y `codigoEstancia` nulo.
- Obtenido: cumple.
- Evidencia: `codigoReserva=RES-29565C`, `codigoEstancia=null`.
- Estado: PASS.

## Smoke UI (recepcion/panel)

### UI-01 Registro exitoso de pago desde modal
- Flujo:
- abrir `/recepcion/panel?codigo=AE-302&tipo=APARTAESTUDIO`;
- click `Agregar pago`;
- guardar con valores diligenciados del modal.
- Resultado observado:
- request `POST /backend/pagos?idEstancia=3` con `200` (reqid `884`);
- tabla financiera actualiza (`TOTAL PAGOS` y `ESTANCIA` en `COP1,930,000`).
- Estado: PASS.

### UI-02 Validacion frontend por campos obligatorios de pago
- Flujo:
- abrir modal `Agregar pago`;
- dejar monto en `0` e intentar guardar.
- Resultado observado:
- mensaje visible: `Completa los campos de pago obligatorios.`;
- no se dispara un nuevo `POST /backend/pagos?idEstancia=3` en ese intento de guardado.
- Estado: PASS.

## Observaciones

- Se confirma comportamiento especial de mapeo para `ANTICIPO_RESERVA`: cuando la estancia sigue en `RESERVADA`, el DTO de respuesta expone `codigoReserva` y deja `codigoEstancia` en `null`.
- Persisten respuestas `500` para faltantes de query param (`idEstancia`) en vez de un `400` mas especifico.

## Conclusion

- El metodo `crearPago` quedo validado en frontend/backend para caminos felices de `ANTICIPO_ESTANCIA` y `ANTICIPO_RESERVA`, reglas de estado por tipo de pago, validaciones del DTO y consumo UI real desde recepcion.
- Estado final: `14 pruebas ejecutadas`, `14 aprobadas`, `0 fallidas`.
