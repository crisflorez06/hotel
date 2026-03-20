# Informe de pruebas - Vista Ajustes

Fecha: 2026-03-19

## Alcance

- Vista validada: `http://localhost:4200/ajustes`
- Metodo: pruebas manuales E2E con Chrome MCP (solo desktop)
- Flujos cubiertos: temporada activa (seleccion/activacion), carga de tarifas base, modal de edicion de tarifas y navegacion cruzada
- Mobile: no aplicado por indicacion del usuario
- Base de datos: se realizo cambio de temporada `ALTA -> BAJA -> ALTA` para validar activacion y se restauro estado inicial

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| AJ-01 | Carga inicial de `ajustes` | OK | Renderiza secciones `Temporada operativa` y `Tarifas base por unidad` |
| AJ-02 | Visualizacion de temporada activa | OK | Muestra `Temporada activa: ALTA` |
| AJ-03 | Opciones de selector de temporada | OK | Combo incluye `Selecciona una opcion`, `Temporada alta`, `Temporada baja` |
| AJ-04 | Estado inicial de `Activar temporada` con temporada ya activa | OK | Con `Temporada alta` seleccionada, boton disabled |
| AJ-05 | Seleccionar `Temporada baja` | OK | Boton `Activar temporada` se habilita |
| AJ-06 | Activar `Temporada baja` | OK | Toast `Temporada activa actualizada correctamente.` y etiqueta cambia a `BAJA` |
| AJ-07 | Estado post-activacion de temporada | OK | Boton vuelve a disabled al coincidir seleccion y estado activo |
| AJ-08 | Reversion a `Temporada alta` | OK | Se habilita boton al cambiar seleccion |
| AJ-09 | Activar `Temporada alta` (restauracion) | OK | Toast de exito y etiqueta vuelve a `ALTA` |
| AJ-10 | Seleccionar placeholder `Selecciona una opcion` | OK | Boton `Activar temporada` permanece disabled |
| AJ-11 | Estado de bloque de tarifas base | OK | Muestra estado `Configuradas` |
| AJ-12 | Render de tarjetas de tarifas por unidad | OK | Aparecen `Habitacion`, `Apartaestudio`, `Apartamento` con valores visibles |
| AJ-13 | Apertura de modal editar tarifa (Habitacion) | OK | Modal `Editar tarifa · Habitacion` con 6 campos numericos precargados |
| AJ-14 | Cierre de modal editar con `Cancelar` | OK | Modal cierra sin cambios visibles |
| AJ-15 | Apertura de modal editar tarifa (Apartaestudio) | OK | Modal abre con campos precargados y acciones `Cancelar`/`Confirmar cambios` |
| AJ-16 | Intento de confirmar con campo requerido vacio | OK | Modal se cierra; no se observa request de guardado |
| AJ-17 | Apertura de modal editar tarifa (Apartamento) | OK | Modal abre con valores actuales |
| AJ-18 | Confirmar cambios sin modificar valores | OK | Modal cierra; no se observa request de guardado |
| AJ-19 | Navegacion a `Dashboard` desde menu superior | OK | Carga `dashboard` con metricas y tablas |
| AJ-20 | Retorno a `Ajustes` desde menu superior | OK | Vista recarga y mantiene `Temporada activa: ALTA` |
| AJ-21 | Recarga de datos de ajustes tras navegacion | OK | `temporada activa` y `tarifas base` responden `200` al regresar |
| AJ-22 | Verificacion de consola en vista ajustes | OK | Solo issues de accesibilidad, sin excepciones JS bloqueantes |
| AJ-23 | Persistencia E2E de guardado de tarifas con cambios reales | BLOQUEADO | No ejecutado para evitar modificar politicas tarifarias de negocio en esta corrida |
| AJ-24 | Validaciones de negocio de tarifas (negativos/cero/formato) con guardado | BLOQUEADO | Requiere guardados deliberados y posterior limpieza de datos |

## Evidencia de red observada

- Carga de ajustes:
  - `GET /backend/ajustes/temporada/activa` -> `200` (reqid `85`, `95`)
  - `GET /backend/ajustes/tarifas-base` -> `200` (reqid `86`, `96`)
- Activacion de temporada:
  - `PUT /backend/ajustes/temporada?temporada=BAJA` -> `204` (reqid `87`)
  - `PUT /backend/ajustes/temporada?temporada=ALTA` -> `204` (reqid `88`)
- Navegacion cruzada (control):
  - `GET /backend/detalle/dashboard/resumen` -> `200` (reqid `91`)
  - `GET /backend/detalle/dashboard/financiero/distribucion` -> `200` (reqid `92`)
  - `GET /backend/gastos` -> `200` (reqid `93`)
- Intentos de confirmacion en modal de tarifas (AJ-16, AJ-18):
  - No se detectaron requests `PUT/PATCH` de tarifas durante esos intentos.

## Hallazgos

- Hallazgo de UX (prioridad baja): al confirmar en modal de tarifas con campo requerido vacio (AJ-16), el modal cierra sin mensaje explicito de validacion.
- Hallazgo de trazabilidad funcional (prioridad baja): al confirmar modal de tarifas sin cambios (AJ-18), no hay feedback explicito de `sin cambios`.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` (msgid `11`, `12`, count total: 2).

## Resumen

- Casos ejecutados: **24**
- OK: **22**
- FAIL: **0**
- BLOQUEADOS: **2**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| AJ-25 | Carga base de ajustes en nueva sesion | OK | Renderiza `Temporada activa: ALTA` y tarjetas de tarifas |
| AJ-26 | Seleccionar `Temporada baja` habilita accion | OK | `Activar temporada` pasa de disabled a enabled |
| AJ-27 | Activar `Temporada baja` | OK | Toast `Temporada activa actualizada correctamente.` y estado `BAJA` |
| AJ-28 | Reversion `BAJA -> ALTA` con doble click en accion | OK | Se aplica una sola actualizacion efectiva y termina en `ALTA` |
| AJ-29 | Seleccionar placeholder `Selecciona una opcion` | OK | `Activar temporada` permanece disabled |
| AJ-30 | Apertura modal `Editar tarifa · Habitacion` | OK | Carga 6 campos numericos precargados |
| AJ-31 | Cierre de modal de tarifa con `Escape` | FAIL | El modal permanece abierto; no responde a teclado |
| AJ-32 | Confirmar modal de tarifa con requerido vacio | FAIL | Modal cierra sin feedback explicito de validacion |
| AJ-33 | Captura de valor negativo en tarifa (`-1`) | OK | Input queda `invalid=true` en UI |
| AJ-34 | Confirmar tarifa con valor negativo | FAIL | UI envia request y backend responde `400`; validacion depende del backend |
| AJ-35 | Guardado real controlado de tarifa (`Adic. estadia corta` 100 -> 101) | OK | Toast de exito y tarjeta muestra `COP101` |
| AJ-36 | Restauracion de tarifa (`101 -> 100`) | OK | Guardado exitoso y tarjeta vuelve a `COP100` |
| AJ-37 | Navegacion `Dashboard -> Ajustes` tras cambios | OK | Vista recarga estable con `Temporada activa: ALTA` y tarifas restauradas |

## Evidencia de red - ronda adicional

- Carga de vista:
  - `GET /backend/ajustes/temporada/activa` -> `200` (reqid `1357`, `1370`)
  - `GET /backend/ajustes/tarifas-base` -> `200` (reqid `1358`, `1371`)
- Activacion de temporada:
  - `PUT /backend/ajustes/temporada?temporada=BAJA` -> `204` (reqid `1359`)
  - `PUT /backend/ajustes/temporada?temporada=ALTA` -> `204` (reqid `1360`)
- Edicion de tarifas:
  - `PUT /backend/ajustes/tarifas-base` -> `400` (reqid `1361`) al confirmar valor negativo
  - `PUT /backend/ajustes/tarifas-base` -> `200` (reqid `1362`) guardado controlado `100 -> 101`
  - `PUT /backend/ajustes/tarifas-base` -> `200` (reqid `1363`) restauracion `101 -> 100`
- Navegacion cruzada de control:
  - `GET /backend/detalle/dashboard/resumen` -> `200` (reqid `1366`)
  - `GET /backend/detalle/dashboard/financiero/distribucion` -> `200` (reqid `1367`)
  - `GET /backend/gastos` -> `200` (reqid `1368`)

## Hallazgos adicionales

- Hallazgo de accesibilidad/UX (prioridad media): modal de edicion de tarifas no cierra con `Escape`.
- Hallazgo de UX/validacion (prioridad media): al confirmar con campo requerido vacio, el modal se cierra sin mensaje explicito de validacion.
- Hallazgo funcional (prioridad media): la UI permite confirmar valores negativos y delega rechazo al backend (`400`).
- Verificacion de integridad: se realizo guardado real y se restauro el valor original para evitar deriva de configuracion (`Adic. estadia corta` de apartaestudio queda en `COP100`).
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` (msgid `189`, count: 1).

## Resumen (ronda adicional)

- Casos ejecutados: **13**
- OK: **10**
- FAIL: **3**
- BLOQUEADOS: **0**

## Resumen acumulado

- Casos ejecutados: **37**
- OK: **32**
- FAIL: **3**
- BLOQUEADOS: **2**
