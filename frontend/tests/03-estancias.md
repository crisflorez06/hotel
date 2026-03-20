# Informe de pruebas - Vista Estancias

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/estancias`
- Metodo: pruebas manuales con Chrome MCP (solo desktop)
- Mobile: no aplicado por indicacion del usuario
- Preparacion backend: no requerida para esta ronda
- Base de datos: no utilizada

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| EST-01 | Carga inicial de la vista | OK | Render de encabezado y tabla de estancias |
| EST-02 | Carga de datos iniciales | OK | Total visible `23`, pagina `1 de 2` |
| EST-03 | Apertura de `Mas filtros` | OK | Se despliegan bloques de estado/tipo/reserva y rangos avanzados |
| EST-04 | Cierre de `Mas filtros` | OK | Panel avanzado se colapsa correctamente |
| EST-05 | Filtro rapido `Hoy` (rango general) | OK | Se aplica rango del dia y refresca tabla |
| EST-06 | Limpieza de filtros tras rango rapido | OK | Tabla vuelve a dataset general |
| EST-07 | Filtro por `codigoEstancia=EST-51728P` | OK | Respuesta filtrada por codigo |
| EST-08 | Filtro por codigo sin resultados (`ZZZ`) | OK | Tabla queda en estado vacio (sin filas) |
| EST-09 | Boton `Limpiar` despues de filtro vacio | OK | Recupera listado completo |
| EST-10 | Filtro avanzado combinado (estado+tipo+reserva) | OK | Query con `estados=ACTIVA`, `tipoUnidad=APARTAESTUDIO`, `tieneReservaAsociada=true` |
| EST-11 | Limpiar filtro avanzado combinado | OK | Retorna a listado completo |
| EST-12 | Cambio de tamano de pagina a `10` | OK | Muestra `Mostrando 1-10`, `Pagina 1 de 3` |
| EST-13 | Paginacion `Siguiente` con size 10 | OK | Muestra `Mostrando 11-20`, `Pagina 2 de 3` |
| EST-14 | Paginacion `Anterior` con size 10 | OK | Vuelve a `Mostrando 1-10`, `Pagina 1 de 3` |
| EST-15 | Restaurar tamano de pagina a `20` | OK | Vuelve a `Mostrando 1-20`, `Pagina 1 de 2` |
| EST-16 | Estado inicial de acciones sin seleccion | OK | `Dar salida`, `Editar`, `Eliminar` deshabilitados |
| EST-17 | Seleccion de estancia `ACTIVA` (`EST-49405C`) | OK | Acciones `Dar salida`, `Editar`, `Eliminar` habilitadas |
| EST-18 | Accion `Editar` sobre estancia activa | OK | Navega a `/estancias/nueva?...editar=true&estanciaId=31` |
| EST-19 | Regreso a tabla desde modulo de edicion | OK | Retorna a `/estancias` |
| EST-20 | Accion `Dar salida` sobre estancia activa | OK | Navega a `/estancias/salida` con datos precargados |
| EST-21 | Regreso a tabla desde modulo de salida | OK | Retorna a `/estancias` |
| EST-22 | Apertura modal `Notas de estancia` | OK | Modal visible para `EST-49405C` |
| EST-23 | Cierre modal `Notas de estancia` | OK | Modal cierra por boton `Cerrar` |
| EST-24 | Apertura modal `Eliminar estancia` | OK | Modal confirma accion irreversible |
| EST-25 | Cancelar modal `Eliminar estancia` | OK | Cierra sin ejecutar eliminacion |
| EST-26 | Seleccion de estancia `FINALIZADA` (`EST-50370H`) | OK | Reglas de negocio aplicadas sobre acciones |
| EST-27 | Restriccion de `Dar salida` para `FINALIZADA` | OK | Boton deshabilitado con mensaje explicito |
| EST-28 | Restriccion de `Editar` para `FINALIZADA` | OK | Boton deshabilitado con mensaje explicito |
| EST-29 | Restriccion de `Eliminar` para `FINALIZADA` | OK | Boton deshabilitado con mensaje explicito |
| EST-30 | Navegacion a reservas desde icono de fila | OK | Navega a `/reservas?codigoReserva=RES-15627S` |
| EST-31 | Navegacion de retorno a estancias tras salto a reservas | OK | Retorna a `/estancias` |
| EST-32 | Navegacion a pagos desde icono de fila | OK | Navega a `/pagos?codigoEstancia=EST-49405C` |
| EST-33 | Navegacion de retorno a estancias tras salto a pagos | OK | Retorna a `/estancias` |
| EST-34 | Navegacion a clientes desde icono de fila | OK | Navega a `/ocupantes/tabla-clientes?numeroDocumento=123456789` |
| EST-35 | Navegacion de retorno a estancias tras salto a clientes | OK | Retorna a `/estancias` |
| EST-36 | Revision de issues de accesibilidad reportados por DevTools | OK con observacion | Persisten `No label associated with a form field` (6) |

## Evidencia de red observada

- `GET /backend/estancias/tabla?page=0&size=20&sort=entradaReal,desc` -> `200` (reqid `1129`)
- `GET /backend/estancias/tabla?page=0&size=10&sort=entradaReal,desc` -> `200` (reqid `1135`)
- `GET /backend/estancias/tabla?page=1&size=10&sort=entradaReal,desc` -> `200` (reqid `1164`)
- `GET /backend/estancias/tabla?...codigoEstancia=ZZZ...` -> `200` (reqid `1137`)
- `GET /backend/estancias/tabla?...estados=ACTIVA&tipoUnidad=APARTAESTUDIO...&tieneReservaAsociada=true...` -> `200` (reqid `1140`)
- `GET /backend/estancias/31` -> `200` (reqid `1175`)
- `POST /backend/pagos/total` -> `200` (reqid `1178`)
- `GET /backend/reservas/tabla?...codigoReserva=RES-15627S...` -> `200` (reqid `1182`)
- `GET /backend/pagos?...codigoEstancia=EST-49405C` -> `200` (reqid `1184`)
- `GET /backend/ocupantes/tabla-clientes?...numeroDocumento=123456789` -> `200` (reqid `1180`)

## Hallazgos

- No se detectaron fallos funcionales bloqueantes en `estancias` durante esta ejecucion.
- Observacion de UX/contenido: en modal de notas de `EST-49405C` aparece texto con prefijos `null` antes del contenido historico.
- Observacion de accesibilidad reportada por DevTools: `No label associated with a form field` (6 ocurrencias).

## Resumen

- Casos ejecutados: **36**
- OK: **36**
- FAIL: **0**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| EST-37 | Filtro combinado fuerte (unidad+estado+reserva+documento) | OK | Tabla reducida a 2 filas (`AE-301`, `Finalizada`, doc `123456789`) |
| EST-38 | Entrada de fecha invalida en `Rango general desde` | OK con observacion | Campo queda en estado invalido sin mensaje visible inmediato |
| EST-39 | `Limpiar` tras fecha invalida en rango general | OK con observacion | En una iteracion no limpio de inmediato el estado invalido |
| EST-40 | Secuencia rapida de rangos (`Hoy` -> `Manana` -> `Esta semana` -> `Proximos 7 dias` -> `Este mes`) | OK | Rango final visible del mes (`01/03/2026` a `31/03/2026`) |
| EST-41 | Seleccion de fila `Finalizada` y reglas de acciones | OK | `Dar salida`, `Editar`, `Eliminar` deshabilitados con mensaje |
| EST-42 | Seleccion de fila `Excedida` (`EST-80398P`) | OK | `Dar salida`, `Editar`, `Eliminar` habilitados |
| EST-43 | `Dar salida` sobre fila `Excedida` | OK | Navega a `/estancias/salida` con datos precargados |
| EST-44 | `Volver` desde salida hacia tabla | OK | Retorna a `/estancias` |
| EST-45 | Tamano de pagina `50` | OK | `Mostrando 1-23`, `Pagina 1 de 1` |
| EST-46 | Tamano de pagina `100` | OK | `Mostrando 1-23`, `Pagina 1 de 1` |
| EST-47 | Restaurar tamano de pagina a `20` | OK | Vuelve a `Mostrando 1-20`, `Pagina 1 de 2` |
| EST-48 | Apertura de modal `Notas` (fila `EST-49405C`) | OK con observacion | Contenido incluye prefijo `null | ...` |
| EST-49 | Cierre de modal `Notas` con tecla `Escape` | FAIL | El modal permanece abierto tras `Escape` |
| EST-50 | Cierre de modal `Notas` con boton `Cerrar` | OK | El modal cierra correctamente |
| EST-51 | `Editar` sobre fila `Excedida` (`EST-80398P`) | OK | Navega a `/estancias/nueva?...editar=true&estanciaId=29` |
| EST-52 | `Volver` desde edicion a tabla | OK con observacion | Retorna a `/estancias`, pero pierde seleccion previa |
| EST-53 | Apertura modal `Eliminar estancia` sobre `Excedida` | OK | Modal muestra confirmacion irreversible para `EST-80398P` |
| EST-54 | Cierre de modal `Eliminar estancia` con `Escape` | FAIL | El modal no se cierra con `Escape` |
| EST-55 | Cancelar modal `Eliminar estancia` por boton | OK | Cierra sin ejecutar eliminacion |
| EST-56 | Navegacion a clientes desde fila seleccionada y retorno por menu | OK | Navega a `/ocupantes/tabla-clientes?numeroDocumento=123456789` y vuelve a `/estancias` |
| EST-57 | Recarga dura (`reload` sin cache) | OK | La vista recarga y mantiene operatividad general |
| EST-58 | Revision final de red y consola | OK con observacion | Sin errores JS bloqueantes; persiste issue a11y de labels |

## Evidencia de red observada (ronda adicional)

- `GET /backend/estancias/tabla?page=0&size=20&estados=FINALIZADA&tipoUnidad=APARTAESTUDIO&codigoUnidad=AE-301&numeroDocumentoCliente=123456789&tieneReservaAsociada=true&sort=entradaReal,desc` -> `200` (reqid `507` y `508`)
- `GET /backend/estancias/29` -> `200` (reqid `515`, `533`)
- `POST /backend/pagos/total` -> `200` (reqid `516`)
- `GET /backend/estancias/tabla?page=0&size=50&sort=entradaReal,desc` -> `200` (reqid `521`)
- `GET /backend/estancias/tabla?page=0&size=100&sort=entradaReal,desc` -> `200` (reqid `522`)
- `GET /backend/estancias/tabla?page=0&size=20&sort=entradaReal,desc` -> `200` (reqid `523`, `527`, `538`, `576`)
- `GET /backend/pagos?page=0&size=10&sort=fecha,desc&codigoEstancia=EST-49405C` -> `200` (reqid `526`)
- `GET /backend/ocupantes/tabla-clientes?page=0&size=20&sort=nombres,asc&numeroDocumento=123456789` -> `200` (reqid `537`)

## Hallazgos (ronda adicional)

- Falla UX: modal de `Notas` no cierra con tecla `Escape` (EST-49).
- Falla UX: modal de `Eliminar estancia` no cierra con tecla `Escape` (EST-54).
- Observacion UX/validacion: entrada de fecha invalida no muestra feedback claro y `Limpiar` tuvo comportamiento no inmediato en una iteracion (EST-38, EST-39).
- Observacion UX/contenido: modal de notas mantiene prefijo `null | ...` en el texto historico (EST-48).
- Observacion de navegacion/contexto: al volver desde edicion se pierde seleccion previa en tabla (EST-52).
- Observacion de accesibilidad (DevTools): persiste `No label associated with a form field` (6 ocurrencias).

## Resumen (ronda adicional)

- Casos ejecutados: **22**
- OK: **15**
- FAIL: **2**
- OK con observacion: **5**

## Resumen acumulado

- Casos ejecutados: **58**
- OK: **51**
- FAIL: **2**
- OK con observacion: **5**
- BLOQUEADOS: **0**
