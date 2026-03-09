# Resultados de pruebas frontend - crearEstanciaNueva (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `crearEstanciaNueva` (`POST /estancias`) consumido por frontend.
- Casos basados en validaciones y dependencias de `EstanciaService.crearEstanciaNueva`.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/EstanciaService.java`
- `src/main/java/com/hotel/controllers/EstanciaController.java`
- `src/main/java/com/hotel/dtos/estancia/EstanciaRequestDTO.java`
- `frontend/src/app/services/estancia.service.ts`
- `frontend/src/app/pages/estancia-nueva/estancia-nueva.component.ts`
- `frontend/src/app/pages/estancia-nueva/estancia-nueva.component.html`

## Resumen

- Suite API-driven desde navegador: `10 pruebas` -> `10 PASS`, `0 FAIL`.
- Smoke UI en `/estancias/nueva`: `2 pruebas` -> `2 PASS`.
- Total ejecutado: `12 pruebas` -> `12 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id: `72182`
- Cliente QA: `id=13` (`QA Estancia72182`, documento `QA-EST-72182`)
- Acompanante QA: creado para pruebas de ocupantes

## Resultados detallados (suite API)

### TC01 - Creacion exitosa de estancia
- Objetivo: validar flujo feliz de creacion con payload completo.
- Preparacion: usar habitacion disponible al momento de la prueba.
- Ejecucion: `POST /backend/estancias` con `tipoUnidad=HABITACION`, cliente QA y rango valido (`entrada <= now`, `salida > entrada`).
- Esperado: `201` y codigo de estancia generado.
- Obtenido: `201`.
- Evidencia: `codigoFolio=EST-49347N`.
- Estado: PASS.

### TC02 - Creacion exitosa adicional (HABITACION)
- Objetivo: confirmar estabilidad del flujo feliz en segunda ejecucion independiente.
- Ejecucion: `POST /backend/estancias` con otra ventana horaria valida.
- Esperado: `201`.
- Obtenido: `201`.
- Evidencia: `codigoFolio=EST-16894K`.
- Estado: PASS.

### TC03 - Rechazo por salida igual o menor a entrada
- Objetivo: validar regla de fechas en `validarFechasEstancia`.
- Ejecucion: `POST` con `salidaEstimada <= entradaReal`.
- Esperado: `400` con mensaje de fecha de salida.
- Obtenido: `400`.
- Evidencia: `La fecha de salida debe ser posterior a la fecha de entrada`.
- Estado: PASS.

### TC04 - Rechazo por entrada futura
- Objetivo: validar regla de negocio: para estancia nueva la entrada no puede ser futura.
- Ejecucion: `POST` con `entradaReal > now`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura`.
- Estado: PASS.

### TC05 - Conflicto de disponibilidad por solapamiento
- Objetivo: validar `disponibilidadService.verificarDisponibilidadNuevo`.
- Preparacion: crear una estancia en una misma habitacion y luego intentar otra solapada.
- Ejecucion: dos `POST` consecutivos con rangos traslapados.
- Esperado: `201` para la primera, `409` para la segunda.
- Obtenido: `201/409`.
- Evidencia: `No se puede crear la estancia: existe una estancia para las habitaciones con codigo: APT-201-H3`.
- Estado: PASS.

### TC06 - Codigo invalido para tipo HABITACION
- Objetivo: validar resolucion de alojamiento (`UnidadHabitacionResolver`).
- Ejecucion: `POST` con `tipoUnidad=HABITACION` y codigo inexistente.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Habitacion no encontrada con codigo: NO-HAB-72182`.
- Estado: PASS.

### TC07 - idCliente inexistente
- Objetivo: validar fallo en carga de ocupantes (`OcupanteService.determinarOcupantes`).
- Ejecucion: `POST` con `idCliente=99999999`.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Ocupante no encontrado con id: 99999999`.
- Estado: PASS.

### TC08 - Acompanante inexistente
- Objetivo: validar error al resolver acompanantes invalidos.
- Ejecucion: `POST` con `idAcompanantes=[99999998]`.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Ocupante no encontrado con id: 99999998`.
- Estado: PASS.

### TC09 - Validacion DTO (campo obligatorio)
- Objetivo: validar `@Valid` en request de creacion.
- Ejecucion: `POST` omitiendo `tipoUnidad`.
- Esperado: `400` con `VALIDATION_ERROR`.
- Obtenido: `400`.
- Evidencia: `tipoUnidad es obligatorio`.
- Estado: PASS.

### TC10 - Cambio de estado operativo a OCUPADO
- Objetivo: validar efecto colateral de `alojamientoResolver.actualizarEstadoAlojamiento(..., OCUPADO)`.
- Ejecucion:
- crear estancia valida en habitacion disponible;
- consultar `GET /backend/recepcion/habitaciones?estado=OCUPADO`.
- Esperado: la habitacion usada aparece como ocupada.
- Obtenido: `201/ocupado:true`.
- Evidencia: habitacion `APT-201-H3` encontrada en listado `OCUPADO`.
- Estado: PASS.

## Smoke UI (/estancias/nueva)

### UI-01 Creacion exitosa desde formulario
- Flujo:
- abrir `estancias/nueva` con unidad y cliente precargados por query params;
- diligenciar `Entrada real`, `Salida estimada`, `Notas`;
- click `Guardar estancia`.
- Resultado observado:
- request de red `POST /backend/estancias` con `201` (reqid `315`);
- flujo finaliza en redireccion a `/recepcion` tras mensaje de exito.
- Estado: PASS.

### UI-02 Error de negocio mostrado en UI (entrada futura)
- Flujo:
- en el mismo formulario, establecer entrada en fecha/hora futura;
- click `Guardar estancia`.
- Resultado observado:
- request de red `POST /backend/estancias` con `400` (reqid `365`);
- mensaje visible: `La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura`.
- Estado: PASS.

## Conclusion

- El servicio `crearEstanciaNueva` y su consumo en frontend quedaron validados para camino feliz, validaciones de fecha, disponibilidad, validacion DTO, manejo de ocupantes y actualizacion de estado operativo del alojamiento.
