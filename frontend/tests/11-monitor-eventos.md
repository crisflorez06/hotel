# Informe de pruebas - Vista Monitor de Eventos

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/monitor-eventos`
- Metodo: pruebas manuales E2E con Chrome MCP (solo desktop)
- Flujos cubiertos: filtros por chips/codigos/fechas, eventos criticos, detalle de evento, navegacion cruzada y paginacion
- Mobile: no aplicado por indicacion del usuario
- Base de datos: no modificada manualmente

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| ME-01 | Carga inicial de `monitor-eventos` | OK | Renderiza encabezado y bloque de filtros |
| ME-02 | Tabla inicial de eventos | OK | Muestra columnas `Entidad`, `Tipo evento`, codigos, detalle y fecha |
| ME-03 | Paginacion inicial | OK | `Total: 121`, `Mostrando 1-20`, `Pagina 1 de 7` |
| ME-04 | Boton `Anterior` en pagina inicial | OK | Aparece disabled en pagina 1 |
| ME-05 | Filtro por chip `Creacion Reserva` | OK | Al aplicar, tabla queda en eventos de creacion reserva |
| ME-06 | Resultado con `Creacion Reserva` | OK | Total baja a `28` y se observan solo filas de reserva/creacion |
| ME-07 | Filtro combinado `Creacion Reserva + Eliminacion Reserva` | OK | Al aplicar se incluyen ambos tipos de evento |
| ME-08 | Resultado con combinacion de tipos de reserva | OK | Total sube a `36` con mezcla de creacion/eliminacion |
| ME-09 | Filtro adicional por entidad `Pago` sobre tipos de reserva | OK | Backend responde y tabla se actualiza |
| ME-10 | Comportamiento de combinacion compleja de chips | OK | Se observan resultados mixtos; validado como comportamiento actual |
| ME-11 | `Limpiar` tras combinacion de chips | OK | Restablece listado general sin criterios |
| ME-12 | Filtro por entidad `Pago` (solo) | OK | Tabla muestra solo eventos de entidad pago |
| ME-13 | Resultado de entidad `Pago` | OK | Total `37` con filas de pagos |
| ME-14 | Intento de agregar chip `Creacion Pago` sobre entidad `Pago` | OK | No cambia resultado visible en la corrida (misma consulta efectiva) |
| ME-15 | Filtro por `codigoReserva=RES-80320Y` | OK | Campo acepta valor y participa en query |
| ME-16 | Filtro por `codigoEstancia=EST-51728P` | OK | Campo acepta valor y participa en query |
| ME-17 | Filtro por rango fecha exacto `2026-03-14` | OK | Fecha desde/hasta aplicadas correctamente |
| ME-18 | Aplicar filtros combinados (entidad + codigos + fecha) | OK | Resultado acotado a 2 eventos |
| ME-19 | Resultado acotado esperado | OK | `Total: 2`, `Mostrando 1-2` |
| ME-20 | Boton `Ver eventos criticos` con filtros activos | OK | Mantiene criterios activos y actualiza dataset |
| ME-21 | Filtro por codigo inexistente (`NO-EXISTE-XYZ`) | OK | Devuelve estado sin resultados |
| ME-22 | Estado vacio en tabla filtrada | OK | Mensaje `No hay eventos con los filtros actuales.` |
| ME-23 | Paginacion en estado vacio | OK | `Pagina 1 de 1`, ambos botones disabled |
| ME-24 | `Limpiar` desde estado vacio | OK | Restituye listado general |
| ME-25 | Paginacion `Siguiente` en dataset general | OK | Cambia a `Mostrando 21-40`, `Pagina 2 de 7` |
| ME-26 | Paginacion `Anterior` para retorno | OK | Regresa a `Mostrando 1-20`, `Pagina 1 de 7` |
| ME-27 | Apertura de modal `Detalle del evento` | OK | Modal visible con tipo y campos dinamicos |
| ME-28 | Cierre de modal por boton `Cerrar` | OK | Cierra sin alterar contexto de filtros |
| ME-29 | Reapertura de modal de detalle | OK | Se puede abrir nuevamente sin errores |
| ME-30 | Navegacion `Ver en tabla` desde evento de reserva | OK | Redirige a `/reservas?idReserva=28` |
| ME-31 | Carga de modulo destino (`reservas`) | OK | Tabla de reservas se carga con filtro por `idReserva` |
| ME-32 | Retorno a `monitor-eventos` desde menu | OK | Vuelve a vista de eventos operativa |
| ME-33 | `Ver eventos criticos` en estado limpio | OK | Reduce dataset a eventos criticos |
| ME-34 | Dataset critico en estado limpio | OK | `Total: 46`, `Pagina 1 de 3` |
| ME-35 | Rango invertido (`desde > hasta`) | OK | Permite aplicar y devuelve tabla vacia |
| ME-36 | Respuesta UI ante rango invertido | OK | Mensaje de sin resultados, sin bloqueo de vista |
| ME-37 | `Limpiar` tras rango invertido | OK | Recupera dataset general |
| ME-38 | Estabilidad general tras cambios sucesivos | OK | Sin bloqueos de UI durante toda la ejecucion |

## Evidencia de red observada

- `GET /backend/eventos?page=0&size=20&sort=fecha,desc` -> `200` (reqid `542`, `547`, `553`, `555`, `560`, `563`)
- `GET /backend/eventos?...&tiposEvento=CREACION_RESERVA` -> `200` (reqid `544`)
- `GET /backend/eventos?...&tiposEvento=CREACION_RESERVA&tiposEvento=ELIMINACION_RESERVA` -> `200` (reqid `545`)
- `GET /backend/eventos?...&tiposEvento=CREACION_RESERVA&tiposEvento=ELIMINACION_RESERVA&entidades=PAGO` -> `200` (reqid `546`)
- `GET /backend/eventos?...&entidades=PAGO` -> `200` (reqid `548`, `549`)
- `GET /backend/eventos?...&entidades=PAGO&codigoReserva=RES-80320Y&codigoEstancia=EST-51728P&fechaDesde=2026-03-14T00:00:00&fechaHasta=2026-03-14T23:59:59` -> `200` (reqid `550`)
- `GET /backend/eventos?...&codigoReserva=NO-EXISTE-XYZ...` -> `200` (reqid `552`)
- `GET /backend/eventos?page=1&size=20&sort=fecha,desc` -> `200` (reqid `554`)
- `GET /backend/eventos?...&tiposEvento=MODIFICACION_ESTANCIA&tiposEvento=ELIMINACION_ESTANCIA&entidades=PAGO&entidades=TARIFA_BASE&entidades=AJUSTE_TEMPORADA` -> `200` (reqid `551`, `561`)
- `GET /backend/eventos?...&fechaDesde=2026-03-15T00:00:00&fechaHasta=2026-03-14T23:59:59` -> `200` (reqid `562`)
- Navegacion cruzada desde detalle:
  - `GET /backend/reservas/tabla?page=0&size=20&idReserva=28&sort=fechaCreacion,desc` -> `200` (reqid `559`)

## Hallazgos

- Hallazgo funcional (prioridad media): la seleccion de chips no siempre es intuitiva; en combinaciones sucesivas se observan queries con criterios acumulados/no evidentes en la UI.
- Hallazgo de UX (prioridad baja): al usar `Ver eventos criticos`, pueden persistir y combinarse criterios previos, lo que dificulta predecir el resultado si no se limpia antes.
- Hallazgo de validacion (prioridad baja): se permite aplicar rango invertido (`fechaDesde > fechaHasta`) y solo se obtiene estado vacio, sin feedback explicito de validacion.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` (msgid `91`, count: 1).

## Resumen

- Casos ejecutados: **38**
- OK: **38**
- FAIL: **0**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| ME-39 | `Ver eventos criticos` con dataset actual | OK | Reduce listado a `Total: 53`, `Pagina 1 de 3` |
| ME-40 | Combinacion `Creacion Reserva` + entidad `Pago` tras modo critico | FAIL | Resultado incluye entidades/tipos no seleccionados (p.ej. `Ajuste Temporada`, `Tarifa Base`) |
| ME-41 | Trazabilidad de combinacion compleja de chips | FAIL | Query incluye criterios acumulados no evidentes en UI (`MODIFICACION_ESTANCIA`, `ELIMINACION_ESTANCIA`, etc.) |
| ME-42 | Filtro operativo por `codigoReserva=RES-80320Y` + `codigoEstancia=EST-51728P` + fecha exacta | OK | Retorna estado vacio `No hay eventos con los filtros actuales.` (`Total: 0`) |
| ME-43 | `Limpiar` tras estado vacio | OK | Restablece dataset general `Total: 133` y `Pagina 1 de 7` |
| ME-44 | Rango invertido (`2026-03-15` > `2026-03-14`) | OK | Permite aplicar y deja estado vacio sin bloqueo |
| ME-45 | Recuperacion con `Limpiar` tras rango invertido | OK | Vuelve a `Total: 133` |
| ME-46 | Apertura modal `Detalle del evento` (evento de pago) | OK | Modal muestra `Pago | Modificacion Pago` con diffs de `Monto` y `Estado` |
| ME-47 | Cierre modal `Detalle del evento` con `Escape` | FAIL | Modal permanece abierto; no responde a tecla |
| ME-48 | Cierre manual modal `Detalle del evento` | OK | Boton `Cerrar` cierra correctamente |
| ME-49 | Navegacion `Ver en tabla` desde detalle | OK | Redirige a `/pagos?idPago=24` con `Total: 1` |
| ME-50 | Retorno a `monitor-eventos` desde menu | OK | Vista vuelve operativa con listado cargado |
| ME-51 | Paginacion `Siguiente` y `Anterior` en dataset general | OK | Avanza a `Mostrando 21-40` (`Pagina 2 de 7`) y retorna a `1 de 7` |

## Evidencia de red - ronda adicional

- `GET /backend/eventos?page=0&size=20&sort=fecha,desc` -> `200` (reqid `1254`, `1259`, `1261`, `1266`, `1268`).
- `GET /backend/eventos?page=0&size=20&tiposEvento=MODIFICACION_ESTANCIA&tiposEvento=ELIMINACION_ESTANCIA&entidades=PAGO&entidades=TARIFA_BASE&entidades=AJUSTE_TEMPORADA&sort=fecha,desc` -> `200` (reqid `1256`).
- `GET /backend/eventos?page=0&size=20&tiposEvento=MODIFICACION_ESTANCIA&tiposEvento=ELIMINACION_ESTANCIA&tiposEvento=CREACION_RESERVA&entidades=TARIFA_BASE&entidades=AJUSTE_TEMPORADA&sort=fecha,desc` -> `200` (reqid `1257`).
- `GET /backend/eventos?page=0&size=20&tiposEvento=MODIFICACION_ESTANCIA&tiposEvento=ELIMINACION_ESTANCIA&tiposEvento=CREACION_RESERVA&entidades=TARIFA_BASE&entidades=AJUSTE_TEMPORADA&codigoReserva=RES-80320Y&codigoEstancia=EST-51728P&fechaDesde=2026-03-14T00:00:00&fechaHasta=2026-03-14T23:59:59&sort=fecha,desc` -> `200` (reqid `1258`).
- `GET /backend/eventos?page=0&size=20&fechaDesde=2026-03-15T00:00:00&fechaHasta=2026-03-14T23:59:59&sort=fecha,desc` -> `200` (reqid `1260`).
- Navegacion cruzada desde detalle:
  - `GET /backend/pagos?page=0&size=10&sort=fecha,desc&idPago=24` -> `200` (reqid `1265`).
- Paginacion:
  - `GET /backend/eventos?page=1&size=20&sort=fecha,desc` -> `200` (reqid `1267`).

## Hallazgos adicionales

- Hallazgo funcional (prioridad media): persiste acumulacion de criterios no evidentes en UI al combinar `Ver eventos criticos` + chips; el backend recibe filtros distintos a lo esperado por seleccion visible.
- Hallazgo de accesibilidad/UX (prioridad media): modal `Detalle del evento` no cierra con `Escape`.
- Hallazgo de validacion (prioridad baja): rango invertido sigue permitido y solo devuelve estado vacio sin mensaje explicito de validacion.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` (msgid `177`, count: 1).

## Resumen (ronda adicional)

- Casos ejecutados: **13**
- OK: **10**
- FAIL: **3**
- BLOQUEADOS: **0**

## Resumen acumulado

- Casos ejecutados: **51**
- OK: **48**
- FAIL: **3**
- BLOQUEADOS: **0**
