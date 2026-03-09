# Resultados de pruebas frontend - buscarReservasPorNumeroDocumento (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `buscarReservasPorNumeroDocumento` expuesto en `GET /reservas/buscar-por-documento?numero=...`.
- Consumo desde frontend (`ReservaService.buscarPorDocumento`) y flujo UI en modal de busqueda del calendario.
- Ejecucion en navegador con Chrome DevTools MCP sobre `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/ReservaService.java` (metodo `buscarReservasPorNumeroDocumento`)
- `src/main/java/com/hotel/controllers/ReservaController.java`
- `frontend/src/app/services/reserva.service.ts`
- `frontend/src/app/pages/calendario/calendario.component.ts`
- `frontend/src/app/pages/calendario/calendario.component.html`

## Resumen

- Suite API-driven desde navegador: `8 pruebas` -> `7 PASS`, `1 FAIL`.
- Smoke UI en calendario (modal buscar reserva): `3 pruebas` -> `3 PASS`.
- Total ejecutado: `11 pruebas` -> `10 PASS`, `1 FAIL`.

Datos de ejecucion:
- Run id: `86187`
- Documento QA creado para pruebas: `QA-DOC-86187`
- Cliente QA creado: `id=11`
- Reserva base APARTAESTUDIO (confirmada): `RES-90025S`

## Resultados detallados (suite API)

### TC01 - Busqueda exacta devuelve solo reservas CONFIRMADAS
- Objetivo: verificar filtro de estado aplicado por servicio (`EstadoReserva.CONFIRMADA`).
- Preparacion:
- crear 2 reservas para el mismo documento;
- cancelar una de ellas.
- Ejecucion: `GET /backend/reservas/buscar-por-documento?numero=QA-DOC-86187`.
- Esperado: incluir confirmadas y excluir canceladas.
- Obtenido: `200/1/aparta:true/hab:false`.
- Estado: PASS.

### TC02 - Busqueda por coincidencia parcial (containing)
- Objetivo: validar `ContainingIgnoreCase`.
- Preparacion: usar fragmento del documento (`OC-861`).
- Ejecucion: `GET ...?numero=OC-861`.
- Esperado: retorno con coincidencias parciales.
- Obtenido: `200/1/has:true`.
- Estado: PASS.

### TC03 - Documento sin coincidencias
- Objetivo: validar respuesta vacia sin error.
- Ejecucion: `GET ...?numero=NO-MATCH-XYZ`.
- Esperado: `200` con lista vacia.
- Obtenido: `200/0`.
- Estado: PASS.

### TC04 - Documento en blanco
- Objetivo: validar guard clause de servicio para `isBlank`.
- Ejecucion: `GET ...?numero=%20%20%20`.
- Esperado: `400` con mensaje `numeroDocumento es obligatorio`.
- Obtenido: `400` con mensaje correcto.
- Estado: PASS.

### TC05 - Parametro `numero` faltante
- Objetivo: validar manejo cuando no se envia query param requerido.
- Ejecucion: `GET /backend/reservas/buscar-por-documento` (sin `numero`).
- Esperado: `400` por parametro requerido.
- Obtenido: `500` con mensaje generico `Ocurrió un error inesperado.`.
- Estado: FAIL.
- Hallazgo: el caso de `MissingServletRequestParameterException` no esta mapeado de forma especifica a `400` en el manejo de errores actual para este endpoint.

### TC06 - Mapeo de tipo/codigo para modo COMPLETO
- Objetivo: validar que `llenarTipoYCodigoUnidad` complete `tipoUnidad` y `codigoUnidad` para reserva no individual.
- Ejecucion: consulta por documento y validacion de item APARTAESTUDIO.
- Esperado: `tipoUnidad=APARTAESTUDIO`, `codigoUnidad=AE-202`.
- Obtenido: `APARTAESTUDIO/AE-202`.
- Estado: PASS.

### TC07 - Mapeo de tipo/codigo para modo INDIVIDUAL
- Objetivo: validar comportamiento para reserva de habitacion individual.
- Preparacion: crear nueva reserva `HABITACION` confirmada para el mismo documento.
- Ejecucion: consulta por documento y validacion de item habitacion.
- Esperado: `tipoUnidad=HABITACION`, `codigoUnidad=<codigo habitacion>`.
- Obtenido: `HABITACION/APT-201-H1`.
- Estado: PASS.

### TC08 - Endpoint de consulta sin efectos colaterales
- Objetivo: confirmar que la busqueda no rompe ni altera flujos relacionados.
- Ejecucion: consultas antes/despues de eventos asociados a la reserva.
- Esperado: endpoint responde correctamente (solo lectura).
- Obtenido: `200/200/200`.
- Estado: PASS.

## Smoke UI (calendario -> Buscar reserva)

### UI-01 Validacion en cliente cuando documento esta vacio
- Flujo:
- abrir calendario (`/calendario`);
- click en `Buscar reserva`;
- click en `Buscar` sin diligenciar documento.
- Resultado observado: mensaje `Ingresa el numero de documento para buscar.`
- Estado: PASS.

### UI-02 Busqueda exitosa por documento
- Flujo:
- en modal, ingresar `QA-DOC-86187`;
- click `Buscar`.
- Resultado observado:
- se listan reservas encontradas (ej. `AE-202` y `APT-201-H1`);
- estado visible `Confirmada`;
- accion `Ver mas` disponible.
- Evidencia de red: `GET /backend/reservas/buscar-por-documento?numero=QA-DOC-86187` con `200` (reqid `159`).
- Estado: PASS.

### UI-03 Busqueda sin resultados
- Flujo:
- en modal, buscar `DOC-NO-EXISTE-XYZ`.
- Resultado observado: `No se encontraron reservas con ese documento.`
- Evidencia de red: `GET ...?numero=DOC-NO-EXISTE-XYZ` con `200` (reqid `160`).
- Estado: PASS.

## Conclusion

- La funcionalidad principal de busqueda por documento funciona y filtra correctamente reservas por estado `CONFIRMADA`.
- Se detecto 1 defecto backend en el caso de parametro faltante (`numero`): actualmente retorna `500` y deberia responder `400`.
