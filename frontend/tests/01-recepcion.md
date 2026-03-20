# Informe de pruebas - Vista Recepcion

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/recepcion`
- Metodo: pruebas manuales con Chrome MCP (UI-first)
- Preparacion backend: no requerida para esta ronda
- Base de datos: no utilizada

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Navegador: Chrome DevTools MCP
- Viewports ejecutados: desktop `1366x768` y mobile `390x844`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| REC-01 | Carga inicial de la vista | OK | URL `/recepcion`, total en vista `7` |
| REC-02 | Conteo y render de tarjetas base | OK | Se muestran apartamentos, habitaciones y apartaestudios |
| REC-03 | Filtro por tipo `APARTAMENTO` | OK | Total en vista pasa a `3` |
| REC-04 | Toggle de tipo (desactivar `APARTAMENTO`) | OK | Total vuelve a `7` |
| REC-05 | Filtro por tipo `HABITACION` | OK | Total en vista `9` |
| REC-06 | Filtro combinado `HABITACION + OCUPADO + PISO 2` | OK | Total en vista `3` (APT-201-H1/H2/H3) |
| REC-07 | Filtro por codigo `APT-201` sobre combinacion anterior | OK | Mantiene total `3` |
| REC-08 | Filtro por codigo sin resultados (`ZZZ`) | OK | Muestra estado vacio con mensaje de no resultados |
| REC-09 | Boton `Limpiar` tras filtros activos | OK | Restablece filtros y total vuelve a `7` |
| REC-10 | Modal de alerta (unidad) apertura | OK | Se abre modal "Alerta en unidad APT-201" |
| REC-11 | Modal de alerta cierre por backdrop | OK | Cierra correctamente |
| REC-12 | Modal de proxima reserva apertura | OK | Se abre "Proxima reserva AE-201" con entrada/salida/personas |
| REC-13 | Modal de proxima reserva cierre por boton | OK | Cierra correctamente |
| REC-14 | Navegacion a detalle unidad desde tarjeta | OK | URL `/recepcion/panel?codigo=APT-101&tipo=APARTAMENTO` |
| REC-15 | Navegacion a detalle habitacion desde subtarjeta | OK | URL `/recepcion/panel?codigo=APT-101-H1&tipo=HABITACION` |
| REC-16 | Acciones visibles en unidad `DISPONIBLE` | OK | Aparecen `Crear estancia` y `Crear reserva` |
| REC-17 | Acciones visibles en unidad `OCUPADO` | OK | Aparecen `Crear reserva` y `Dar salida` |
| REC-18 | Flujo boton `Crear estancia` desde recepcion | OK | Navega a `/estancias/nueva?codigo=APT-101&tipo=APARTAMENTO` |
| REC-19 | Flujo boton `Crear reserva` desde recepcion | OK | Navega a `/reservas/nueva?codigo=APT-101&tipo=APARTAMENTO` |
| REC-20 | Flujo boton `Dar salida` desde recepcion | OK | Navega a `/estancias/salida` con datos precargados |
| REC-21 | Validacion responsive mobile | OK | Vista carga en `390x844` con filtros y tarjetas operativas |
| REC-22 | Revision de issues en consola | OK con observacion | Issue a11y reportado: "No label associated with a form field" (3) |

## Evidencia de red

- `GET /backend/recepcion/unidades` -> `200` (reqid `313`)
- `GET /backend/recepcion/habitaciones` -> `200` (reqid `316`)
- `GET /backend/recepcion/habitaciones?codigo=ZZZ` -> `200` (reqid `317`)

## Hallazgos

- No se detectaron fallos funcionales bloqueantes en la vista `recepcion` durante esta ejecucion.
- Se mantiene observacion de accesibilidad detectada por DevTools:
  - `No label associated with a form field` (3 ocurrencias).

## Resumen

- Casos ejecutados: **22**
- OK: **22**
- FAIL: **0**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

- Cobertura: solo desktop (`1366x768`)
- Objetivo: estresar filtros, modales, navegacion, CRUD y rehidratacion de estado

### Casos adicionales ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| REC-23 | Filtros cruzados de alta combinacion (`HABITACION + OCUPADO + PISO 2 + codigo`) | OK | `Total en vista: 3`, lista `APT-201-H1/H2/H3`; request `GET /backend/recepcion/habitaciones?estado=OCUPADO&piso=PISO_2&codigo=APT-201` (reqid `146`) |
| REC-24 | Combinacion excluyente con codigo inexistente sobre filtros activos | FAIL | Con `tipo=APARTAMENTO + estado=OCUPADO/RESERVADO + piso=2 + codigo=NO-EXISTE-XYZ` mantiene `Total en vista: 1`; request `GET /backend/recepcion/unidades?...&codigo=NO-EXISTE-XYZ` (reqid `149`) |
| REC-25 | Idempotencia de chips y recuperacion con `Limpiar` | OK | Activacion/desactivacion repetida sin bloqueo; `Limpiar` restablece `Total en vista: 7` |
| REC-26 | Persistencia tras navegacion cruzada (Recepcion -> Panel -> Recepcion) | OK | Navegacion a `/recepcion/panel?codigo=AE-201&tipo=APARTAESTUDIO` y retorno estable a `/recepcion` |
| REC-27 | Variantes de busqueda por codigo (minusculas/exacto/espacios) | FAIL | `codigo=apt-101` no acota resultados (`Total 7`); `codigo=APT-101-H2` acota a `Total 1`; `codigo=" APT-101 "` retorna vacio (`Total 0`) |
| REC-28 | Apertura/cierre repetido de modal `Ver alerta` | OK | Modal `Alerta en unidad APT-201` abre y cierra por boton de cierre y CTA `Cerrar` |
| REC-29 | Cierre por tecla `Escape` en modal `Proxima reserva` | FAIL | Modal `Proxima reserva AE-201` no cierra con `Escape`; requiere boton `Cerrar` |
| REC-30 | Verificacion de contenido y navegacion desde tarjetas | OK | Modal de proxima reserva muestra `entrada/salida/personas`; navegacion de tarjeta a panel correcta |
| REC-31 | Flujo de `Crear estancia` desde recepcion con validaciones de negocio | OK | Bloqueos controlados: `POST /backend/estancias` -> `400` (entrada futura, reqid `172`) y `409` (conflicto por reserva, reqid `173`) |
| REC-32 | Flujo CRUD de `Crear reserva` desde recepcion | OK | Reserva creada con exito en `APT-101-H1`; `POST /backend/reservas` -> `201` (reqid `179`), toast `Operacion realizada con exito.` |
| REC-33 | Flujo CRUD de `Dar salida` desde recepcion | OK | `PUT /backend/estancias/finalizar` -> `204` (reqid `186`), toast `Estancia finalizada con exito.` |
| REC-34 | Secuencia encadenada de acciones sobre mismas unidades | OK | Tras crear reserva en `APT-101-H1` aparece `Ver proxima reserva`; tras salida en `AE-301` pasa a `DISPONIBLE` con `Crear estancia/Crear reserva` |
| REC-35 | Trazabilidad de red por accion/filtro/CRUD | OK | Evidencia de `GET/POST/PUT` esperados en reqids `146, 149, 172, 173, 179, 186` |
| REC-36 | Verificacion de consola post-flujos agresivos | OK con observacion | Sin excepciones JS bloqueantes; persiste issue a11y `No label associated with a form field` (count: 3, msgid `31`/`35`) |
| REC-37 | Rehidratacion tras recarga dura (`reload` sin cache) | OK | `reload` exitoso y vista estable con `Total en vista: 7`; recarga `GET /backend/recepcion/unidades` (reqid `231`) |

### Evidencia de red adicional

- Filtros y busqueda:
  - `GET /backend/recepcion/habitaciones?estado=OCUPADO&piso=PISO_2&codigo=APT-201` -> `200` (reqid `146`)
  - `GET /backend/recepcion/unidades?tipo=APARTAMENTO&estado=OCUPADO&estado=RESERVADO&piso=PISO_2&codigo=NO-EXISTE-XYZ` -> `200` (reqid `149`)
  - `GET /backend/recepcion/unidades?codigo=apt-101` -> `200` (reqid `151`)
  - `GET /backend/recepcion/unidades?codigo=APT-101-H2` -> `200` (reqid `152`)
- Estancias/reservas/salida:
  - `POST /backend/estancias` -> `400` (reqid `172`)
  - `POST /backend/estancias` -> `409` (reqid `173`)
  - `POST /backend/reservas` -> `201` (reqid `179`)
  - `PUT /backend/estancias/finalizar` -> `204` (reqid `186`)
- Recarga final:
  - `GET /backend/recepcion/unidades` -> `200` (reqid `231`)

### Hallazgos adicionales

- Hallazgo funcional medio: combinacion con codigo inexistente en ciertos filtros de unidades no retorna vacio (REC-24).
- Hallazgo funcional medio: comportamiento inconsistente del filtro por codigo segun formato (`apt-101`, espacios, codigo exacto) (REC-27).
- Hallazgo UX bajo: modal de proxima reserva no responde a cierre por `Escape` (REC-29).

### Resumen de ronda adicional

- Casos adicionales ejecutados: **15**
- OK: **12**
- FAIL: **3**
- BLOQUEADOS: **0**
