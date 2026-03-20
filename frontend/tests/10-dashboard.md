# Informe de pruebas - Vista Dashboard

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/dashboard`
- Metodo: pruebas manuales E2E con Chrome MCP (solo desktop)
- Flujos cubiertos: carga de bloques, rango financiero, graficas/indicadores, registro de gastos (filtros y modales CRUD)
- Mobile: no aplicado por indicacion del usuario
- Base de datos: no modificada manualmente (no se confirmaron altas/ediciones/eliminaciones)

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| DASH-01 | Carga inicial de `dashboard` | OK | Renderiza `Centro operativo del hotel` y todos los bloques |
| DASH-02 | Render de bloque `Rango financiero` | OK | Inputs `Desde/Hasta` y KPIs visibles |
| DASH-03 | Render de bloque `Ocupacion global` | OK | Tarjetas de Apartamentos, Apartaestudios y Habitaciones visibles |
| DASH-04 | Render de bloque `Canales de reserva` | OK | Grafico de canales visible |
| DASH-05 | Render de bloque `Indicadores operativos clave` | OK | Muestra estancias activas, reservas confirmadas y alertas |
| DASH-06 | Render de bloque `Evolucion de estancias` | OK | Grafico historico visible |
| DASH-07 | Render de bloque `Registro de gastos operativos` | OK | Tabla, filtros y acciones visibles |
| DASH-08 | Estado inicial de acciones de gastos | OK | `Editar` y `Eliminar` inician disabled |
| DASH-09 | Cambio de rango financiero `2026-03-12` a `2026-03-15` | OK | KPIs y etiqueta de periodo se actualizan |
| DASH-10 | Cambio de rango financiero `2026-03-01` a `2026-03-31` | OK | KPIs vuelven a valores de mes completo |
| DASH-11 | Recalculo automatico sin boton aplicar | OK | Cada cambio de fecha dispara refresco de resumen/distribucion |
| DASH-12 | Consistencia KPI financiero | OK | `Neto del periodo` cambia acorde a ingresos/gastos del rango |
| DASH-13 | KPI `Pendiente` y contador de pagos | OK | Se mantiene alineado (`$ 3.240.000`, `2 pagos`) en los rangos probados |
| DASH-14 | Indicadores operativos numericos visibles | OK | `Estancias activas 5`, `Reservas confirmadas 7`, `Alertas 4` |
| DASH-15 | Lectura de sub-alertas | OK | `Reservas expiradas 0`, `Estancias excedidas 4` |
| DASH-16 | Coherencia ocupacion Apartamentos | OK | Total 3 con distribucion 2/0/1 |
| DASH-17 | Coherencia ocupacion Apartaestudios | OK | Total 4 con distribucion 3/0/1 |
| DASH-18 | Coherencia ocupacion Habitaciones | OK | Total 9 con distribucion 6/0/3 |
| DASH-19 | Filtro gastos por texto con match (`fdg`) | OK | Conserva fila coincidente |
| DASH-20 | Filtro gastos por metodo (`Efectivo`) | OK | Conserva fila coincidente con medio esperado |
| DASH-21 | Filtro gastos por texto sin match | OK | Muestra `No hay gastos registrados.` |
| DASH-22 | Filtro gastos por fecha exacta sin match (`2026-03-15`) | OK | Muestra `No hay gastos registrados.` |
| DASH-23 | Boton `Limpiar` en gastos | OK | Limpia filtros de gastos y recupera dataset |
| DASH-24 | Alcance de `Limpiar` en gastos | OK | No afecta el rango financiero superior |
| DASH-25 | Seleccionar fila de gasto | OK | Habilita `Editar` y `Eliminar` |
| DASH-26 | Deseleccionar fila de gasto | OK | `Editar` y `Eliminar` vuelven a disabled |
| DASH-27 | Abrir modal `Agregar gasto` | OK | Modal `Formulario de gasto` visible |
| DASH-28 | Validacion requerido en `Agregar gasto` (guardar vacio) | OK | Mensaje: `El concepto del gasto es obligatorio.` |
| DASH-29 | Carga de campos en `Agregar gasto` (sin guardar) | OK | Concepto, monto, metodo, referencia editables |
| DASH-30 | Cierre de `Agregar gasto` con boton `Cerrar` | OK | Modal se cierra sin persistir |
| DASH-31 | Apertura de `Editar gasto` con fila seleccionada | OK | Modal precarga datos (`fdg`, `4433`, fecha/metodo) |
| DASH-32 | Cancelar `Editar gasto` | OK | Cierra modal sin cambios |
| DASH-33 | Apertura de `Eliminar gasto` con fila seleccionada | OK | Modal `Eliminar gasto` visible |
| DASH-34 | Cancelar `Eliminar gasto` | OK | Cierra modal sin borrar registro |
| DASH-35 | Integridad de tabla tras cancelar modales | OK | Fila original permanece en grilla |
| DASH-36 | Respuesta de UI tras filtros sucesivos | OK | No bloqueos visibles ni errores de render |
| DASH-37 | Consola DevTools durante ejecucion | OK | Sin errores ni warnings registrados |
| DASH-38 | Navegacion lateral disponible desde dashboard | OK | Menu principal operativo para rutas clave |

## Evidencia de red observada

- `GET /backend/detalle/dashboard/resumen` -> `200` (reqid `431`, `435`, `437`, `439`, `441`)
- `GET /backend/detalle/dashboard/financiero/distribucion` -> `200` (reqid `432`)
- `GET /backend/detalle/dashboard/financiero/distribucion?desde=2026-03-12T00:00:00` -> `200` (reqid `436`)
- `GET /backend/detalle/dashboard/financiero/distribucion?desde=2026-03-12T00:00:00&hasta=2026-03-15T23:59:59` -> `200` (reqid `438`)
- `GET /backend/detalle/dashboard/financiero/distribucion?desde=2026-03-01T00:00:00&hasta=2026-03-15T23:59:59` -> `200` (reqid `440`)
- `GET /backend/detalle/dashboard/financiero/distribucion?desde=2026-03-01T00:00:00&hasta=2026-03-31T23:59:59` -> `200` (reqid `442`)
- `GET /backend/gastos` -> `200` (reqid `433`)

## Hallazgos

- Observacion UX (prioridad baja): el mensaje `El concepto del gasto es obligatorio.` puede quedar visible en la pagina aun despues de cerrar el modal de agregar.
- Observacion de comportamiento (prioridad baja): `Limpiar` del bloque de gastos no resetea las fechas del bloque financiero superior (parece alcance por bloque; validar si es esperado).

## Resumen

- Casos ejecutados: **38**
- OK: **38**
- FAIL: **0**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas + calculos con data variada (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| DASH-39 | Carga base en nueva sesion | OK | Render completo de bloques y tabla de gastos visible |
| DASH-40 | Consistencia matematica base | OK | `5,954,007 - 4,433 = 5,949,574` (coincide con `Neto`) |
| DASH-41 | Validacion requerido en `Agregar gasto` (guardar vacio) | OK | Mensaje `El concepto del gasto es obligatorio.` sin envio de alta |
| DASH-42 | Alta gasto A (`DASH-E2E-A`, `1111`, `Efectivo`) | OK | KPI `Gastos` sube `4,433 -> 5,544`; `Neto` baja `5,949,574 -> 5,948,463` |
| DASH-43 | Alta gasto B (`DASH-E2E-B`, `22222`, `Transferencia`) | OK | KPI `Gastos` sube `5,544 -> 27,766`; `Neto` baja a `5,926,241` |
| DASH-44 | Alta gasto C futuro (`DASH-E2E-C`, `333333`, `Otro`, `2026-04-05`) | FAIL | En estado `Inicio/Hoy` el KPI incluye el gasto futuro (`Gastos` pasa a `361,099`) |
| DASH-45 | Rango financiero manual `2026-03-01 -> 2026-03-31` | OK | Excluye gasto futuro; `Gastos` vuelve a `27,766` |
| DASH-46 | Consistencia matematica con rango marzo | OK | `5,954,007 - 27,766 = 5,926,241` |
| DASH-47 | Rango financiero invertido (`desde > hasta`) | OK | UI muestra `desde no puede ser mayor que hasta` y backend responde `400` controlado |
| DASH-48 | Recuperacion tras rango invertido | OK | Reestableciendo rango valido, KPIs vuelven a valores esperados |
| DASH-49 | Filtro de gastos por texto (`DASH-E2E-B`) | OK | Tabla queda con 1 fila (`DASH-E2E-B`) |
| DASH-50 | Filtro de gastos por metodo (`Transferencia`) | OK | Mantiene solo gasto B coherente con criterio |
| DASH-51 | Filtro de gastos por fecha exacta (`2026-03-19`) | OK | Mantiene registro de prueba en la fecha esperada |
| DASH-52 | `Limpiar` en bloque de gastos | OK | Limpia texto/metodo/fechas del bloque y recupera lista |
| DASH-53 | Alcance de `Limpiar` en gastos vs rango financiero | OK | No altera `Desde/Hasta` del bloque financiero (`2026-03-01 -> 2026-03-31`) |
| DASH-54 | Apertura de `Editar gasto` con fila seleccionada | OK | Modal precargado con datos de `DASH-E2E-A` |
| DASH-55 | Cierre de modal `Editar gasto` con `Escape` | FAIL | Modal permanece abierto; no responde a cierre por teclado |
| DASH-56 | Edicion real de gasto A (`1111 -> 5555`, metodo `Tarjeta Debito`) | OK | Toast `Gasto actualizado correctamente.` y tabla refleja cambio |
| DASH-57 | Recalculo tras editar gasto A | OK | `Gastos` cambia `27,766 -> 32,210`; `Neto` baja a `5,921,797` |
| DASH-58 | Apertura de modal `Eliminar gasto` | OK | Modal de confirmacion visible sobre gasto seleccionado |
| DASH-59 | Cierre de modal `Eliminar gasto` con `Escape` | FAIL | Modal permanece abierto; no cierra con teclado |
| DASH-60 | Eliminacion de gasto A | OK | `DELETE` exitoso y KPI vuelve a `Gastos: 26,655` |
| DASH-61 | Eliminacion de gasto B | OK | `DELETE` exitoso y KPI vuelve a `Gastos: 4,433` |
| DASH-62 | Eliminacion de gasto C | OK | `DELETE` exitoso y limpieza completa de datos de prueba |
| DASH-63 | Verificacion de limpieza final | OK | Con filtro `DASH-E2E` aparece `No hay gastos registrados.` |
| DASH-64 | Restauracion final del estado operativo | OK | `Limpiar` devuelve tabla base (`fdg`) y KPIs quedan en baseline |

## Evidencia de red - ronda adicional

- Carga inicial de dashboard:
  - `GET /backend/detalle/dashboard/resumen` -> `200` (reqid `1408`)
  - `GET /backend/detalle/dashboard/financiero/distribucion` -> `200` (reqid `1409`)
  - `GET /backend/gastos` -> `200` (reqid `1410`)
- Altas de gastos de prueba:
  - `POST /backend/gastos` -> `200` (reqid `1412`, `1416`, `1420`)
- Actualizacion/edicion de gasto:
  - `PUT /backend/gastos/2` -> `200` (reqid `1436`)
- Eliminaciones de limpieza:
  - `DELETE /backend/gastos/2` -> `204` (reqid `1440`)
  - `DELETE /backend/gastos/3` -> `204` (reqid `1444`)
  - `DELETE /backend/gastos/4` -> `204` (reqid `1448`)
- Rango financiero:
  - `GET /backend/detalle/dashboard/financiero/distribucion?desde=2026-03-01T00:00:00&hasta=2026-03-31T23:59:59` -> `200` (reqid `1427`, `1435`, `1439`, `1443`, `1447`, `1451`)
  - `GET /backend/detalle/dashboard/financiero/distribucion?desde=2026-03-31T00:00:00&hasta=2026-03-01T23:59:59` -> `400` (reqid `1431`)

## Hallazgos adicionales

- Hallazgo funcional (prioridad media): en estado `Inicio/Hoy` el KPI financiero incluyo un gasto futuro (`2026-04-05`), lo que sugiere inconsistencia entre etiqueta visible y filtro efectivo.
- Hallazgo de accesibilidad/UX (prioridad media): los modales `Editar gasto` y `Eliminar gasto` no cierran con `Escape`.
- Hallazgo operativo: el manejo de rango invertido responde con mensaje de UI y `400` controlado, sin romper la vista.
- Verificacion de integridad de datos: se crearon, editaron y eliminaron gastos de prueba (`DASH-E2E-A/B/C`) y la vista quedo restaurada al estado base.
- Hallazgo de consola DevTools: `Failed to load resource: 400 (Bad Request)` asociado al caso de rango invertido (msgid `193`).

## Resumen (ronda adicional)

- Casos ejecutados: **26**
- OK: **23**
- FAIL: **3**
- BLOQUEADOS: **0**

## Resumen acumulado

- Casos ejecutados: **64**
- OK: **61**
- FAIL: **3**
- BLOQUEADOS: **0**
