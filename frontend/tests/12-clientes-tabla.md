# Informe de pruebas - Vista Clientes (tabla)

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/ocupantes/tabla-clientes`
- Metodo: pruebas manuales E2E con Chrome MCP (solo desktop)
- Flujos cubiertos: filtros, seleccion/edicion, creacion, historicos de reservas/estancias, navegacion cruzada y paginacion
- Mobile: no aplicado por indicacion del usuario
- Base de datos: sin cambios persistentes intencionales durante la corrida

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| CT-01 | Carga inicial de `tabla-clientes` | OK | Renderiza filtros, tabla y acciones principales |
| CT-02 | Estructura de columnas en tabla | OK | Se muestran nombres, documento, contacto, tipo, reservas y estancias |
| CT-03 | Estado inicial de `Editar` sin seleccion | OK | Boton aparece disabled con mensaje de ayuda |
| CT-04 | Botones por fila sin historial | OK | `Ver reservas/Ver estancias` disabled cuando conteo es `0` |
| CT-05 | Seleccion de cliente habilita `Editar` | OK | Al marcar un cliente, `Editar` queda enabled |
| CT-06 | Cambio de seleccion entre filas | OK | La seleccion activa cambia de fila y no quedan varias seleccionadas |
| CT-07 | Deseleccionar fila activa | OK | `Editar` vuelve a disabled y reaparece mensaje de ayuda |
| CT-08 | Apertura de modal `Editar cliente` | OK | Modal abre con datos precargados del cliente seleccionado |
| CT-09 | Cierre de modal `Editar` con `Cerrar` (X) | OK | Modal se cierra sin errores |
| CT-10 | Cierre de modal `Editar` con `Cancelar` | OK | Modal se cierra manteniendo contexto de la tabla |
| CT-11 | Filtro por `Nombre=Juan` | OK | Resultado acotado a `Total: 1` (Juan Rodriguez) |
| CT-12 | `Limpiar` tras filtro por nombre | OK | Campos vacios y retorno a listado general (`Total: 5`) |
| CT-13 | Filtro por `Tipo documento=TI` | OK | Resultado acotado a cliente TI (`Total: 1`) |
| CT-14 | Filtro combinado sin coincidencia (`Apellido=zzz` + TI) | OK | Estado vacio con mensaje `No hay clientes con los filtros actuales.` |
| CT-15 | Paginacion en estado vacio | OK | `Pagina 1 de 1`, botones `Anterior/Siguiente` disabled |
| CT-16 | `Limpiar` desde estado vacio | OK | Se recupera dataset general |
| CT-17 | Filtro por `Telefono=3007654321` | OK | Resultado acotado a cliente `Mcp Prueba` |
| CT-18 | Filtro combinado telefono + email | OK | Query combinada aplicada y resultado consistente (`Total: 1`) |
| CT-19 | Cambio de tamano de pagina a `10` | OK | Control refleja `10` y refresca consulta |
| CT-20 | Retorno de tamano de pagina a `20` | OK | Control vuelve a `20` y se refresca consulta |
| CT-21 | Apertura de modal `Crear cliente` | OK | Modal visible con campos requeridos |
| CT-22 | Cierre de `Crear cliente` con `Cancelar` | OK | Modal se cierra sin efectos colaterales |
| CT-23 | Cierre de `Crear cliente` con `Cerrar` (X) | OK | Modal se cierra correctamente |
| CT-24 | Guardar cliente vacio en `Crear cliente` | OK | Mensaje: `Nombres, apellidos, tipo y numero de documento son obligatorios.` |
| CT-25 | Apertura de historico `Ver reservas` con cliente con reservas | OK | Modal `Reservas de ...` con filas y botones `Ver mas` |
| CT-26 | Cierre de historico de reservas | OK | Cierre por `Cerrar` sin romper estado de vista |
| CT-27 | Navegacion `Ver mas` desde reservas | OK | Redireccion a `/reservas?codigoReserva=...` |
| CT-28 | Carga de modulo `Reservas` tras redireccion | OK | Filtro por codigo aplicado en tabla destino (`Total: 1`) |
| CT-29 | Retorno a `Clientes` desde menu | OK | Vista vuelve operativa con listado cargado |
| CT-30 | Apertura de historico `Ver estancias` con cliente con estancias | OK | Modal `Estancias de ...` con filas y `Ver mas` |
| CT-31 | Cierre de historico de estancias | OK | Cierre correcto del modal |
| CT-32 | Navegacion `Ver mas` desde estancias | OK | Redireccion a `/estancias?codigoEstancia=...` |
| CT-33 | Carga de modulo `Estancias` tras redireccion | OK | Filtro por codigo aplicado en tabla destino (`Total: 1`) |
| CT-34 | Retorno a `Clientes` desde menu (segunda navegacion cruzada) | OK | Vista de clientes disponible y estable |
| CT-35 | Reapertura de `Editar` tras varias interacciones | OK | Modal sigue abriendo con datos del cliente seleccionado |
| CT-36 | Guardar edicion con `Nombres/Apellidos` vacios | FAIL | Se envia `PUT`, modal cierra y no hay feedback de validacion en UI |
| CT-37 | Estabilidad general de la vista tras flujos encadenados | OK | Sin bloqueos de UI ni errores visibles |

## Evidencia de red observada

- `GET /backend/ocupantes/tabla-clientes?page=0&size=20&sort=nombres,asc` -> `200` (reqid `651`, `656`, `659`, `663`, `666`, `669`, `671`, `677`, `682`, `685`)
- `GET /backend/ocupantes/tabla-clientes?...&nombre=Juan` -> `200` (reqid `652`, `662`)
- `GET /backend/ocupantes/tabla-clientes?...&tipoDocumento=TI` -> `200` (reqid `664`)
- `GET /backend/ocupantes/tabla-clientes?...&apellido=zzz&tipoDocumento=TI` -> `200` (reqid `665`)
- `GET /backend/ocupantes/tabla-clientes?...&telefono=3007654321` -> `200` (reqid `667`)
- `GET /backend/ocupantes/tabla-clientes?...&telefono=3007654321&email=example.com` -> `200` (reqid `668`)
- `GET /backend/ocupantes/tabla-clientes?page=0&size=10&sort=nombres,asc` -> `200` (reqid `670`)
- Apertura de edicion:
  - `GET /backend/ocupantes/buscar?documento=123456789` -> `200` (reqid `660`, `661`, `683`)
- Guardado de edicion (caso fallido de validacion UI):
  - `PUT /backend/ocupantes/1` -> `200` (reqid `684`)
- Navegacion cruzada desde historicos:
  - `GET /backend/reservas/tabla?page=0&size=20&codigoReserva=RES-84569L&sort=fechaCreacion,desc` -> `200` (reqid `675`)
  - `GET /backend/estancias/tabla?page=0&size=20&codigoEstancia=EST-49405C&sort=entradaReal,desc` -> `200` (reqid `681`)

## Hallazgos

- Hallazgo funcional (prioridad media): en `Editar cliente`, al vaciar `Nombres` y `Apellidos` y guardar, la UI no muestra error de validacion y cierra el modal (caso `CT-36`).
- Hallazgo de UX (prioridad baja): `Limpiar` restablece filtros, pero no siempre limpia la seleccion activa de cliente, lo que puede mantener `Editar` habilitado.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` (msgid `105`, count: 6).
- Hallazgo de accesibilidad DevTools: `An element doesn't have an autocomplete attribute` (msgid `104` y `106`, count total: 2).

## Resumen

- Casos ejecutados: **37**
- OK: **36**
- FAIL: **1**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| CT-38 | Refresco de dataset actual en carga base | OK | Tabla actualiza a `Total: 6` al inicio de la ronda |
| CT-39 | `Limpiar` con cliente seleccionado | FAIL | Mantiene seleccion activa (`Juan`) y `Editar` habilitado tras limpiar filtros |
| CT-40 | Modal `Editar cliente` cierre con `Escape` | FAIL | Modal permanece abierto; no responde a cierre por teclado |
| CT-41 | Regresion `CT-36`: guardar edicion con nombres/apellidos vacios | OK | Muestra mensaje `Nombres, apellidos, tipo y numero de documento son obligatorios.` sin cerrar modal |
| CT-42 | Cerrar `Editar cliente` con `Cancelar` tras error de validacion | OK | Cierra modal y conserva estado de tabla |
| CT-43 | Modal `Crear cliente` cierre con `Escape` | FAIL | Modal permanece abierto; no cierra con tecla |
| CT-44 | Crear cliente nuevo valido en UI | OK | Alta exitosa de `Prueba E2E327` (`CC 9900099009`), tabla pasa a `Total: 7` |
| CT-45 | Filtro por documento del cliente creado | OK | `numeroDocumento=9900099009` retorna `Total: 1` con fila creada |
| CT-46 | `Limpiar` tras filtro de cliente creado | OK | Restituye listado completo con `Total: 7` |
| CT-47 | Modal `Reservas de Juan Rodriguez` apertura | OK | Renderiza historico con multiples reservas y botones `Ver mas` |
| CT-48 | Modal `Reservas de Juan Rodriguez` cierre con `Escape` | FAIL | Modal permanece abierto; no responde a teclado |
| CT-49 | Navegacion `Ver mas` desde historico de reservas | OK | Redirige a `/reservas?codigoReserva=RES-42045R` con `Total: 1` |
| CT-50 | Retorno a `clientes-tabla` desde menu | OK | Vista vuelve operativa y conserva dataset actualizado |
| CT-51 | Cambio de `Tamano pagina` `20 -> 10 -> 20` | OK | Reconsulta backend y mantiene consistencia (`Mostrando 1-7`, `Pagina 1 de 1`) |

## Evidencia de red - ronda adicional

- `GET /backend/ocupantes/tabla-clientes?page=0&size=20&sort=nombres,asc` -> `200` (reqid `1306`, `1307`, `1310`, `1312`, `1318`, `1320`).
- Apertura/edicion de cliente seleccionado:
  - `GET /backend/ocupantes/buscar?documento=123456789` -> `200` (reqid `1308`).
- Creacion de cliente en la ronda:
  - `POST /backend/ocupantes` -> `201` (reqid `1309`) para `Prueba E2E327`, doc `9900099009`.
- Filtro por cliente creado:
  - `GET /backend/ocupantes/tabla-clientes?page=0&size=20&sort=nombres,asc&numeroDocumento=9900099009` -> `200` (reqid `1311`).
- Navegacion cruzada desde historico de reservas:
  - `GET /backend/reservas/tabla?page=0&size=20&codigoReserva=RES-42045R&sort=fechaCreacion,desc` -> `200` (reqid `1316`).
- Cambio de tamano de pagina:
  - `GET /backend/ocupantes/tabla-clientes?page=0&size=10&sort=nombres,asc` -> `200` (reqid `1319`).

## Hallazgos adicionales

- Hallazgo funcional (prioridad media): `Limpiar` continua sin limpiar la seleccion activa; puede dejar `Editar` habilitado sin intencion del usuario.
- Hallazgo de accesibilidad/UX (prioridad media): los modales `Editar cliente`, `Crear cliente` y `Reservas de ...` no cierran con `Escape`.
- Hallazgo de regresion resuelta: el caso historico `CT-36` ahora muestra validacion en UI y evita cerrar el modal al guardar con nombres/apellidos vacios.
- Hallazgo de accesibilidad DevTools: `An element doesn't have an autocomplete attribute` (msgid `183`, count: 1).

## Resumen (ronda adicional)

- Casos ejecutados: **14**
- OK: **10**
- FAIL: **4**
- BLOQUEADOS: **0**

## Resumen acumulado

- Casos ejecutados: **51**
- OK: **46**
- FAIL: **5**
- BLOQUEADOS: **0**
