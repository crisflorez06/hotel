# Resultados de pruebas frontend - finalizarEstancia (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `finalizarEstancia` (`PUT /estancias/finalizar`) consumido por frontend.
- Casos construidos desde reglas y validaciones de `EstanciaService.finalizarEstancia` y `PagoService`.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/EstanciaService.java`
- `src/main/java/com/hotel/services/PagoService.java`
- `src/main/java/com/hotel/controllers/EstanciaController.java`
- `src/main/java/com/hotel/dtos/estancia/SalidaEstanciaDTO.java`
- `src/main/java/com/hotel/dtos/pago/PagoNuevoRequestDTO.java`
- `frontend/src/app/services/estancia.service.ts`
- `frontend/src/app/pages/estancia-salida/estancia-salida.component.ts`
- `frontend/src/app/pages/recepcion-panel/recepcion-panel.component.ts`

## Resumen

- Suite API-driven desde navegador: `12 pruebas` -> `12 PASS`, `0 FAIL`.
- Smoke UI en flujo de salida de estancia: `2 pruebas` -> `2 PASS`.
- Total ejecutado: `14 pruebas` -> `14 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id suite API: `74252`.
- Estancia objetivo suite API: `id=49`, unidad `APT-201-H1`, estado inicial `ACTIVA`.
- Referencia para estado no editable: `id=2` en estado `FINALIZADA`.
- Flujo UI de exito: estancia `id=45` (unidad `APT-301`).

## Resultados detallados (suite API)

### TC01 - Error por idEstancia inexistente
- Objetivo: validar manejo de estancia no existente.
- Ejecucion: `PUT /backend/estancias/finalizar` con `idEstancia=99999999` y pago valido.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `Estancia no encontrada con id: 99999999`.
- Estado: PASS.

### TC02 - Error por estado no permitido para finalizar
- Objetivo: validar regla de negocio de estado (`ACTIVA` o `EXCEDIDA` solamente).
- Preparacion: usar estancia `id=2` ya `FINALIZADA`.
- Ejecucion: `PUT /backend/estancias/finalizar`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `Solo se pueden finalizar estancias en estado ACTIVA o EXCEDIDA. Estado actual: FINALIZADA`.
- Estado: PASS.

### TC03 - Error por tipoPago distinto de ESTANCIA_COMPLETADA
- Objetivo: validar restriccion de tipo de pago durante finalizacion.
- Ejecucion: `PUT` con `pagoEstancia.tipoPago=ANTICIPO_ESTANCIA`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `El tipo de pago para finalizar una estancia debe ser ESTANCIA_COMPLETADA`.
- Estado: PASS.

### TC04 - Error por estado de pago invalido
- Objetivo: validar `validarEstadoPagoEstanciaCompletada`.
- Ejecucion: `PUT` con `pagoEstancia.estado=ELIMINADO`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `El estado del pago de tipo ESTANCIA_COMPLETADA debe ser COMPLETADO o PENDIENTE`.
- Estado: PASS.

### TC05 - Error por estado de pago faltante
- Objetivo: validar obligatoriedad de `pagoEstancia.estado` para `ESTANCIA_COMPLETADA`.
- Ejecucion: `PUT` omitiendo `pagoEstancia.estado`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `El estado del pago es obligatorio para crear un pago de tipo ESTANCIA_COMPLETADA`.
- Estado: PASS.

### TC06 - Error por pagoEstancia obligatorio
- Objetivo: validar `@Valid` en `SalidaEstanciaDTO` para objeto `pagoEstancia`.
- Ejecucion: `PUT` omitiendo `pagoEstancia`.
- Esperado: `400` con `VALIDATION_ERROR`.
- Obtenido: `400`.
- Evidencia: `fields.pagoEstancia = pagoEstancia es obligatorio`.
- Estado: PASS.

### TC07 - Error por fechaSalidaReal obligatoria
- Objetivo: validar `@Valid` para campo requerido `fechaSalidaReal`.
- Ejecucion: `PUT` omitiendo `fechaSalidaReal`.
- Esperado: `400` con `VALIDATION_ERROR`.
- Obtenido: `400`.
- Evidencia: `fields.fechaSalidaReal = fechaSalidaReal es obligatoria`.
- Estado: PASS.

### TC08 - Error por idEstancia obligatorio
- Objetivo: validar `@Valid` para campo requerido `idEstancia`.
- Ejecucion: `PUT` omitiendo `idEstancia`.
- Esperado: `400` con `VALIDATION_ERROR`.
- Obtenido: `400`.
- Evidencia: `fields.idEstancia = idEstancia es obligatorio`.
- Estado: PASS.

### TC09 - Error por tipoPago faltante
- Objetivo: validar validacion de negocio cuando falta `pagoEstancia.tipoPago`.
- Ejecucion: `PUT` con `pagoEstancia` sin `tipoPago`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `El tipo de pago es obligatorio para crear un pago`.
- Estado: PASS.

### TC10 - Finalizacion exitosa
- Objetivo: validar flujo feliz de salida de estancia con pago valido.
- Ejecucion: `PUT /backend/estancias/finalizar` para `idEstancia=49` con `tipoPago=ESTANCIA_COMPLETADA`, `estado=PENDIENTE`, `medioPago=EFECTIVO`, `monto=120000`.
- Esperado: `204`.
- Obtenido: `204`.
- Evidencia: finalizacion aceptada sin errores.
- Estado: PASS.

### TC11 - Reintento sobre estancia ya finalizada
- Objetivo: validar que no se permita finalizar dos veces la misma estancia.
- Ejecucion: repetir `PUT /backend/estancias/finalizar` sobre `idEstancia=49`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `Solo se pueden finalizar estancias en estado ACTIVA o EXCEDIDA. Estado actual: FINALIZADA`.
- Estado: PASS.

### TC12 - Verificacion de efectos colaterales
- Objetivo: validar que finalizacion cambie estado y libere unidad.
- Ejecucion:
- consultar tabla de estancias `FINALIZADA` tras TC10;
- consultar habitaciones `DISPONIBLE`.
- Esperado: `id=49` aparece en `FINALIZADA` y unidad `APT-201-H1` queda disponible.
- Obtenido: verificacion positiva.
- Evidencia: `estaFinalizada=true`, `disponible=true`.
- Estado: PASS.

## Smoke UI (flujo de salida)

### UI-01 Finalizacion exitosa desde recepcion/panel
- Flujo:
- abrir `recepcion/panel?codigo=APT-301&tipo=APARTAMENTO`;
- click `Finalizar estancia`;
- diligenciar monto, fecha de pago y notas;
- click `Finalizar estancia`.
- Resultado observado:
- request `PUT /backend/estancias/finalizar` con `204` (reqid `302`);
- redireccion a `/recepcion` al finalizar.
- Estado: PASS.

### UI-02 Validacion de formulario en salida (sin datos de pago)
- Flujo:
- abrir `recepcion/panel?codigo=AE-202&tipo=APARTAESTUDIO` y entrar a `Finalizar estancia`;
- intentar guardar sin completar `Monto` ni `Fecha pago`.
- Resultado observado:
- mensaje visible: `Completa los datos del pago.`;
- no se dispara request `PUT /backend/estancias/finalizar` en ese intento.
- Estado: PASS.

## Observaciones

- En finalizacion, la validacion de pago mezcla controles de DTO (`@Valid`) y reglas de negocio en `PagoService`; se verificaron ambos caminos.
- La evidencia de red confirma que el formulario frontend evita llamadas al backend cuando faltan datos de pago obligatorios (caso UI-02).

## Conclusion

- El flujo `finalizarEstancia` quedo validado en frontend para camino feliz, estados permitidos, validaciones de request, reglas de pago y efectos colaterales (estado final y liberacion de unidad).
- Estado final: `14 pruebas ejecutadas`, `14 aprobadas`, `0 fallidas`.
