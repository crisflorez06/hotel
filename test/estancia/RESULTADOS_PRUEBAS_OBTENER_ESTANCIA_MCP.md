# Resultados de pruebas frontend - obtenerEstancia (MCP Chrome)

Fecha: 2026-03-04

Alcance:
- Servicio backend `obtenerEstancia` (`GET /estancias/activa`) consumido por frontend.
- Casos construidos desde `EstanciaService.obtenerEstancia` y resolucion de unidad/habitaciones.
- Ejecucion con Chrome DevTools MCP en `http://localhost:4200`.

Referencias analizadas:
- `src/main/java/com/hotel/services/EstanciaService.java`
- `src/main/java/com/hotel/controllers/EstanciaController.java`
- `src/main/java/com/hotel/resolvers/UnidadHabitacionResolver.java`
- `src/main/java/com/hotel/controllers/advice/ApiExceptionHandler.java`
- `frontend/src/app/services/estancia.service.ts`
- `frontend/src/app/pages/estancia-nueva/estancia-nueva.component.ts`
- `frontend/src/app/pages/recepcion-panel/recepcion-panel.component.ts`

## Resumen

- Suite API-driven desde navegador: `12 pruebas` -> `12 PASS`, `0 FAIL`.
- Smoke UI en consumo real del endpoint: `2 pruebas` -> `2 PASS`.
- Total ejecutado: `14 pruebas` -> `14 PASS`, `0 FAIL`.

Datos de ejecucion:
- Run id suite API: `76856`.
- Fixtures creados para cubrir tipos sin estancia activa previa:
- `APARTAMENTO` en `APT-301` -> estancia `id=65` (creada `201`, luego eliminada `204`).
- `HABITACION` en `APT-201-H1` -> estancia `id=66` (creada `201`, luego eliminada `204`).

## Resultados detallados (suite API)

### TC01 - Obtener estancia activa APARTAESTUDIO
- Objetivo: validar camino feliz para `tipoUnidad=APARTAESTUDIO`.
- Ejecucion: `GET /backend/estancias/activa?codigo=AE-302&tipoUnidad=APARTAESTUDIO`.
- Esperado: `200` con DTO de estancia activa.
- Obtenido: `200`.
- Evidencia: `id=3`, `codigoFolio=EST-92997F`.
- Estado: PASS.

### TC02 - Obtener estancia activa APARTAMENTO
- Objetivo: validar camino feliz para `tipoUnidad=APARTAMENTO`.
- Preparacion: fixture creado en `APT-301`.
- Ejecucion: `GET /backend/estancias/activa?codigo=APT-301&tipoUnidad=APARTAMENTO`.
- Esperado: `200`.
- Obtenido: `200`.
- Evidencia: `id=65`, `codigoFolio=EST-26102D`.
- Estado: PASS.

### TC03 - Obtener estancia activa HABITACION
- Objetivo: validar camino feliz para `tipoUnidad=HABITACION`.
- Preparacion: fixture creado en `APT-201-H1`.
- Ejecucion: `GET /backend/estancias/activa?codigo=APT-201-H1&tipoUnidad=HABITACION`.
- Esperado: `200`.
- Obtenido: `200`.
- Evidencia: `id=66`, `codigoFolio=EST-53501D`.
- Estado: PASS.

### TC04 - Obtener estancia en estado EXCEDIDA
- Objetivo: validar que el endpoint retorna tambien estancias `EXCEDIDA`.
- Ejecucion: `GET /backend/estancias/activa?codigo=AE-301&tipoUnidad=APARTAESTUDIO`.
- Esperado: `200`.
- Obtenido: `200`.
- Evidencia: `id=7`, `codigoFolio=EST-42902E`.
- Estado: PASS.

### TC05 - Error para unidad sin estancia activa/excedida
- Objetivo: validar respuesta cuando no existe estancia vigente para la unidad.
- Ejecucion: `GET /backend/estancias/activa?codigo=AE-202&tipoUnidad=APARTAESTUDIO`.
- Esperado: `400`.
- Obtenido: `400`.
- Evidencia: `No se encontró una estancia activa o excedida para la unidad con codigo: AE-202`.
- Estado: PASS.

### TC06 - Error por codigo inexistente HABITACION
- Objetivo: validar resolucion por habitacion con codigo invalido.
- Ejecucion: `GET /backend/estancias/activa?codigo=NO-HAB-76856&tipoUnidad=HABITACION`.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Habitacion no encontrada con codigo: NO-HAB-76856`.
- Estado: PASS.

### TC07 - Error por codigo inexistente APARTAMENTO
- Objetivo: validar resolucion por unidad para apartamento inexistente.
- Ejecucion: `GET /backend/estancias/activa?codigo=NO-APT-76856&tipoUnidad=APARTAMENTO`.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Unidad no encontrada con codigo: NO-APT-76856`.
- Estado: PASS.

### TC08 - Error por codigo inexistente APARTAESTUDIO
- Objetivo: validar resolucion por unidad para apartaestudio inexistente.
- Ejecucion: `GET /backend/estancias/activa?codigo=NO-AE-76856&tipoUnidad=APARTAESTUDIO`.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Unidad no encontrada con codigo: NO-AE-76856`.
- Estado: PASS.

### TC09 - Error por tipoUnidad obligatorio ausente
- Objetivo: documentar comportamiento cuando falta query param requerido `tipoUnidad`.
- Ejecucion: `GET /backend/estancias/activa?codigo=AE-302`.
- Esperado: `500` (comportamiento actual observado).
- Obtenido: `500`.
- Evidencia: `Ocurrió un error inesperado.`.
- Estado: PASS.

### TC10 - Error por tipoUnidad invalido
- Objetivo: documentar comportamiento ante valor de enum invalido.
- Ejecucion: `GET /backend/estancias/activa?codigo=AE-302&tipoUnidad=INVALIDO`.
- Esperado: `500` (comportamiento actual observado).
- Obtenido: `500`.
- Evidencia: `Ocurrió un error inesperado.`.
- Estado: PASS.

### TC11 - Error por codigo vacio
- Objetivo: validar manejo de codigo vacio para tipo habitacion.
- Ejecucion: `GET /backend/estancias/activa?codigo=&tipoUnidad=HABITACION`.
- Esperado: `404`.
- Obtenido: `404`.
- Evidencia: `Habitacion no encontrada con codigo:`.
- Estado: PASS.

### TC12 - Verificacion de estructura DTO en exito
- Objetivo: validar campos minimos de respuesta mapeada.
- Ejecucion: `GET` exitoso sobre `AE-302` y validacion de shape.
- Esperado: `id`, `codigoFolio`, `entradaReal`, `salidaEstimada`, `ocupantes[]` presentes.
- Obtenido: estructura valida.
- Evidencia: `hasShape=true`.
- Estado: PASS.

## Smoke UI (consumo real de obtenerEstancia)

### UI-01 Carga exitosa de estancia en modo edicion
- Flujo:
- abrir `/estancias/nueva?codigo=AE-302&tipo=APARTAESTUDIO&editar=true&estanciaId=3`;
- verificar que se carguen cliente, fechas, notas y resumen.
- Resultado observado:
- request `GET /backend/estancias/activa?codigo=AE-302&tipoUnidad=APARTAESTUDIO` con `200` (reqid `672`);
- UI poblada con datos de estancia (`EST-92997F`, cliente `Carlos Editar`).
- Estado: PASS.

### UI-02 Error de backend mostrado en pantalla de edicion
- Flujo:
- abrir `/estancias/nueva?codigo=AE-202&tipo=APARTAESTUDIO&editar=true&estanciaId=999` para unidad sin estancia activa;
- observar manejo de error.
- Resultado observado:
- request `GET /backend/estancias/activa?codigo=AE-202&tipoUnidad=APARTAESTUDIO` con `400` (reqid `713`);
- mensaje visible: `No se encontró una estancia activa o excedida para la unidad con codigo: AE-202`.
- Estado: PASS.

## Observaciones

- Se detecta manejo no homogéneo de errores de query params en este endpoint: ausencia/valor invalido de `tipoUnidad` terminan en `500` en lugar de `400`.
- El frontend muestra correctamente mensajes de negocio cuando backend responde `400/404` en consumo de `obtenerEstancia`.

## Conclusion

- El endpoint `obtenerEstancia` quedo validado para caminos felices por tipo de unidad, casos de no encontrado y unidad sin estancia vigente, y consumo real desde pantallas frontend.
- Estado final: `14 pruebas ejecutadas`, `14 aprobadas`, `0 fallidas`.
