# Resultados de pruebas frontend - editarEstancia (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `editarEstancia` (`PUT /estancias/{id}`) consumido por frontend.
- Casos construidos desde las validaciones y dependencias de `EstanciaService.editarEstancia`.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/EstanciaService.java`
- `src/main/java/com/hotel/controllers/EstanciaController.java`
- `src/main/java/com/hotel/dtos/estancia/EstanciaRequestDTO.java`
- `src/main/java/com/hotel/resolvers/UnidadHabitacionResolver.java`
- `src/main/java/com/hotel/controllers/advice/ApiExceptionHandler.java`
- `frontend/src/app/services/estancia.service.ts`
- `frontend/src/app/pages/estancia-nueva/estancia-nueva.component.ts`
- `frontend/src/app/pages/estancias/estancias.component.ts`

## Resumen

- Suite API-driven desde navegador: `12 pruebas` -> `12 PASS`, `0 FAIL`.
- Smoke UI en `/estancias/nueva` (modo edicion): `2 pruebas` -> `2 PASS`.
- Total ejecutado: `14 pruebas` -> `14 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id suite API: `30963`
- Estancia objetivo principal: `id=49`, unidad `APT-201-H1`, tipo `HABITACION`, cliente `id=13`.
- Estancia de referencia para conflicto: unidad `APT-301` (`APARTAMENTO`, activa).
- Estancia de referencia para estado no editable: `id=2` (`FINALIZADA`).

## Resultados detallados (suite API)

### TC01 - Edicion exitosa de campos generales
- Objetivo: validar actualizacion exitosa sin cambio de estado invalido.
- Preparacion: estancia activa `id=49` en `APT-201-H3/H1` con cliente valido.
- Ejecucion: `PUT /backend/estancias/49` cambiando notas y manteniendo datos validos.
- Esperado: `204`.
- Obtenido: `204`.
- Evidencia: request respondio sin cuerpo (No Content).
- Estado: PASS.

### TC02 - Edicion exitosa cambiando fechas
- Objetivo: validar `validarEdicionEstancia` cuando se modifican fechas con rango valido.
- Ejecucion: `PUT` con `entradaReal` y `salidaEstimada` nuevas, coherentes y no futuras.
- Esperado: `204`.
- Obtenido: `204`.
- Evidencia: fechas aplicadas en edicion (`entrada=2026-03-04T13:03:59`, `salida=2026-03-04T22:03:59`).
- Estado: PASS.

### TC03 - Edicion exitosa cambiando codigo de unidad
- Objetivo: validar cambio de codigo/tipo cuando hay disponibilidad.
- Preparacion: habia habitacion disponible alterna `APT-201-H1`.
- Ejecucion: `PUT` cambiando `codigo` desde `APT-201-H3` hacia `APT-201-H1` con tipo `HABITACION`.
- Esperado: `204`.
- Obtenido: `204`.
- Evidencia: cambio de unidad aceptado (`codigoAntes=APT-201-H3`, `codigoDespues=APT-201-H1`).
- Estado: PASS.

### TC04 - Error por id de estancia inexistente
- Objetivo: validar manejo de `idEstancia` invalido.
- Ejecucion: `PUT /backend/estancias/99999999` con payload valido.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `Estancia no encontrada con id: 99999999`.
- Estado: PASS.

### TC05 - Error por estado no editable
- Objetivo: validar regla de negocio: solo se edita estado `ACTIVA` o `EXCEDIDA`.
- Preparacion: usar estancia `id=2` en estado `FINALIZADA`.
- Ejecucion: `PUT /backend/estancias/2`.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `Solo se pueden editar estancias en estado ACTIVA o EXCEDIDA. Estado actual: FINALIZADA`.
- Estado: PASS.

### TC06 - Error por salida menor o igual a entrada
- Objetivo: validar `validarFechasEstancia` para `salidaEstimada <= entradaReal`.
- Ejecucion: `PUT` con ambas fechas iguales.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `La fecha de salida debe ser posterior a la fecha de entrada`.
- Estado: PASS.

### TC07 - Error por entrada futura
- Objetivo: validar bloqueo de fechas futuras en edicion.
- Ejecucion: `PUT` con `entradaReal > now`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura`.
- Estado: PASS.

### TC08 - Error por conflicto de disponibilidad
- Objetivo: validar `disponibilidadService.verificarDisponibilidadEditar` en cambio de unidad/fechas.
- Preparacion: existencia de estancia activa en `APT-301`.
- Ejecucion: `PUT` intentando mover `id=49` a `APT-301` en rango conflictivo.
- Esperado: `409`.
- Obtenido: `409`.
- Evidencia: `No se puede editar la estancia: existe una estancia para las habitaciones con codigo: APT-301-H1, APT-301-H2, APT-301-H3`.
- Estado: PASS.

### TC09 - Error por codigo invalido para HABITACION
- Objetivo: validar resolucion de unidad/habitacion con codigo inexistente.
- Ejecucion: `PUT` con `tipoUnidad=HABITACION` y `codigo=NO-HAB-30963`.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Habitacion no encontrada con codigo: NO-HAB-30963`.
- Estado: PASS.

### TC10 - Error por idCliente inexistente
- Objetivo: validar resolucion de ocupantes en edicion.
- Ejecucion: `PUT` con `idCliente=99999999`.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Ocupante no encontrado con id: 99999999`.
- Estado: PASS.

### TC11 - Error por acompanante inexistente
- Objetivo: validar rechazo de acompanantes invalidos.
- Ejecucion: `PUT` con `idAcompanantes=[99999998]`.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Ocupante no encontrado con id: 99999998`.
- Estado: PASS.

### TC12 - Payload incompleto sin tipoUnidad
- Objetivo: validar comportamiento del endpoint ante request incompleto en edicion.
- Ejecucion: `PUT` omitiendo `tipoUnidad`.
- Esperado: `500` (comportamiento actual observado por falta de `@Valid` en controlador de edicion).
- Obtenido: `500`.
- Evidencia: `Ocurrió un error inesperado.`.
- Estado: PASS.

## Smoke UI (/estancias/nueva en modo edicion)

### UI-01 Edicion exitosa desde formulario
- Flujo:
- abrir `/estancias/nueva?codigo=APT-201-H1&tipo=HABITACION&editar=true&estanciaId=49`;
- actualizar notas y guardar con `Actualizar estancia`.
- Resultado observado:
- request de red `PUT /backend/estancias/49` con `204` (reqid `137`);
- redireccion a `/recepcion` al finalizar.
- Estado: PASS.

### UI-02 Error de negocio mostrado en UI (entrada futura)
- Flujo:
- abrir el mismo formulario en modo edicion;
- establecer `Entrada real` y `Salida estimada` en fecha futura;
- guardar con `Actualizar estancia`.
- Resultado observado:
- request de red `PUT /backend/estancias/49` con `400` (reqid `235`);
- mensaje visible: `La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura`.
- Estado: PASS.

## Observaciones

- En una corrida inicial de la suite API hubo falsos fallos por formato temporal UTC (`toISOString`) que el backend interpreto como hora local futura; se corrigio el generador de fechas para enviar hora local (`YYYY-MM-DDTHH:mm:ss`).
- Para `PUT /estancias/{id}`, el controlador no usa `@Valid` en `EstanciaRequestDTO`; por eso payloads incompletos no devuelven `VALIDATION_ERROR` y pueden terminar en `500`.

## Conclusion

- El flujo `editarEstancia` quedo validado en frontend para camino feliz, cambios de fechas/unidad, estado permitido, disponibilidad, validaciones de fechas y resolucion de ocupantes.
- Se identifico hallazgo tecnico en validacion de request de edicion (caso TC12) que conviene normalizar para responder `400` en lugar de `500`.
