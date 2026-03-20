# Informe de pruebas - Vista Reservas Nueva

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/reservas/nueva`
- Metodo: pruebas manuales E2E con Chrome MCP (solo desktop)
- Flujos cubiertos: carga inicial, reglas de formulario, cliente (buscar/agregar), fechas, pago y guardado
- Mobile: no aplicado por indicacion del usuario
- Base de datos: no modificada manualmente

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| RN-01 | Carga inicial de `reservas/nueva` | OK | Renderiza `Nueva reserva` con formulario y resumen |
| RN-02 | Estado inicial de `Codigo` sin tipo | OK | Campo `Codigo` inicia disabled |
| RN-03 | Estado inicial de cliente | OK | Muestra `Sin cliente seleccionado` |
| RN-04 | Estado inicial de pago | OK | Boton `Registrar pago` visible, resumen en `0` |
| RN-05 | Guardar sin completar obligatorios | OK | Mensaje `Completa los campos obligatorios.` |
| RN-06 | Seleccionar `Tipo de unidad = Apartaestudio` | OK | Habilita `Codigo` y carga opciones de AE |
| RN-07 | Opciones de `Codigo` para apartaestudio | OK | Se observan `AE-201/AE-202/AE-301/AE-302` |
| RN-08 | Seleccionar `Codigo = AE-201` | OK | Resumen actualiza `UNIDAD` |
| RN-09 | Seleccionar canal `Whatsapp` | OK | Resumen refleja `CANAL = WHATSAPP` |
| RN-10 | Editar `Numero de personas` | OK | Acepta `2` y resumen sincroniza `PERSONAS = 2` |
| RN-11 | Observacion de control en personas | OK | Input muestra `valuemax=0` aun con unidad seleccionada |
| RN-12 | Editar `Notas` | OK | Campo admite texto libre |
| RN-13 | Abrir modal `Buscar cliente` | OK | Modal `Buscar cliente` visible |
| RN-14 | Buscar documento inexistente | OK | Muestra `No se encontraron clientes.` |
| RN-15 | Buscar documento existente | OK | Lista coincidencia `Juan Rodriguez / CC 123456789` |
| RN-16 | Seleccionar cliente desde modal | OK | Cierra modal y fija cliente en formulario/resumen |
| RN-17 | Abrir modal `Agregar cliente` | OK | Modal `Nuevo cliente` visible |
| RN-18 | Cancelar modal `Agregar cliente` | OK | Cierra modal sin alterar cliente activo |
| RN-19 | Seleccionar `Entrada estimada` | OK | Datepicker operativo, valor aplicado en resumen |
| RN-20 | Seleccionar `Salida estimada` menor a entrada | OK | Permite guardar fecha invalida en UI (`2026-03-24`) |
| RN-21 | Abrir pago con fechas invalidas | OK | Modal muestra `La fecha de entrada debe ser anterior a la salida` |
| RN-22 | Calculo de pago con fechas invalidas | OK | Muestra `0 dias` y backend responde `400` |
| RN-23 | Cancelar modal de pago invalido | OK | Retorna al formulario conservando estado |
| RN-24 | Corregir `Salida estimada` mayor a entrada | OK | Fecha valida aplicada (`2026-03-27`) |
| RN-25 | Abrir pago con fechas validas | OK | Muestra `TOTAL CALCULADO = 600,000` |
| RN-26 | Editar monto de anticipo en pago | OK | Permite cambiar a `250000` |
| RN-27 | Cambiar medio de pago | OK | Selecciona `TRANSFERENCIA_BANCARIA` |
| RN-28 | Editar notas del pago | OK | Acepta texto libre |
| RN-29 | Guardar pago | OK | Boton cambia a `Editar pago`; resumen queda en `250,000` |
| RN-30 | Reabrir modal en `Editar pago` | FAIL | El monto vuelve a `600000` (pierde `250000`) |
| RN-31 | Resumen tras reabrir `Editar pago` | FAIL | Se sobrescribe a `600,000` antes de re-guardar |
| RN-32 | Quitar pago desde modal | OK | Elimina pago y resumen vuelve a `0` |
| RN-33 | Guardar reserva con conflicto de negocio | OK | Backend responde conflicto por estancia existente en `AE-201` |
| RN-34 | Mensaje de conflicto visible al usuario | OK | `No se puede crear la reserva: existe una estancia...` |
| RN-35 | Boton `Volver` desde vista nueva | OK | Navega segun contexto de historial (en sesion regreso a `calendario`) |

## Evidencia de red observada

- `GET /backend/recepcion/unidades?tipo=APARTAESTUDIO` -> `200` (reqid `321`)
- `GET /backend/ocupantes/buscar?documento=9999999999` -> `200` (reqid `322`)
- `GET /backend/ocupantes/buscar?documento=123456789` -> `200` (reqid `323`)
- `POST /backend/pagos/total` (fechas invalidas) -> `400` (reqid `324`)
- `POST /backend/pagos/total` (fechas validas) -> `200` (reqid `325`, `326`)
- `POST /backend/reservas` -> `409` (reqid `327`)

## Hallazgos

- Hallazgo funcional (prioridad media): al reabrir `Editar pago`, el monto manual guardado (`250000`) se restablece a `TOTAL CALCULADO` (`600000`) y el resumen se sobrescribe sin confirmacion.
- Hallazgo de UX/validacion (prioridad baja): el formulario permite dejar `Salida estimada < Entrada estimada`; la validacion aparece recien al abrir el modal de pago.
- Observacion de consistencia UI: `Numero de personas` expone `valuemax=0` en el control durante la sesion.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` (msgid `67`, count: 4).

## Resumen

- Casos ejecutados: **35**
- OK: **33**
- FAIL: **2**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| RN-36 | Modal `Buscar cliente` cierre con `Escape` | FAIL | El modal permanece abierto; no responde a cierre por teclado |
| RN-37 | Modal `Buscar cliente` cierre manual | OK | Boton `Cerrar` cierra correctamente |
| RN-38 | Modal `Agregar cliente` cierre con `Escape` | FAIL | El modal `Nuevo cliente` permanece abierto |
| RN-39 | Crear cliente desde `Agregar cliente` | OK | Alta exitosa de `Cliente Mcp` y auto-seleccion en formulario/resumen |
| RN-40 | Cambio `Tipo = Apartaestudio` recarga codigos | OK | Se habilitan `AE-201/AE-202/AE-301/AE-302` |
| RN-41 | `Numero de personas = 0` | OK | Input queda `invalid=true` (minimo 1) |
| RN-42 | `Numero de personas` fuera de capacidad (3 en apartaestudio) | FAIL | UI acepta `3` y lo refleja en resumen, pese a `valuemax=0` |
| RN-43 | Forzar `Salida < Entrada` (2026-04-08 < 2026-04-09) | OK | Formulario lo permite y mantiene valores en resumen |
| RN-44 | Abrir `Registrar pago` con fechas invalidas | OK | Modal muestra `La fecha de entrada debe ser anterior a la salida`, `0 dias` |
| RN-45 | Modal `Pago de reserva` cierre con `Escape` | FAIL | No cierra con teclado; permanece visible |
| RN-46 | Modal `Pago de reserva` cierre manual | OK | `Cancelar` cierra modal y conserva contexto |
| RN-47 | Recalculo de pago con fechas validas | OK | Para 3 dias muestra `TOTAL CALCULADO = 1,170,000` |
| RN-48 | Guardar pago editado (monto manual 500000) | OK | Tras `Guardar pago`, resumen queda en `500,000` |
| RN-49 | Reabrir `Editar pago` tras monto manual guardado | FAIL | Monto vuelve a `1,170,000` y el resumen se sobrescribe sin confirmar |
| RN-50 | `Quitar pago` desde modal de edicion | OK | Retorna a `Registrar pago` y resumen a `0` |
| RN-51 | Guardar reserva en escenarios con conflicto | OK | Se validan conflictos `409` para `AE-302`, `AE-201`, `APT-301`, `APT-101-H1` |
| RN-52 | Crear escenario y guardar reserva exitosa | OK | Ajustando a `Habitacion APT-101-H1` con rango `2027-01-10 -> 2027-01-12`, `POST /reservas` responde `201` |

## Evidencia de red - ronda adicional

- `POST /backend/ocupantes` -> `201` (reqid `1101`) al crear cliente de prueba (`Cliente Mcp`, documento `9900012345`).
- `GET /backend/recepcion/unidades?tipo=APARTAESTUDIO` -> `200` (reqid `1102`).
- `POST /backend/pagos/total` (fechas invalidas) -> `400` (reqid `1103`).
- `POST /backend/pagos/total` (fechas validas, apertura y reapertura de modal) -> `200` (reqid `1104`, `1105`).
- `POST /backend/reservas` (conflictos de negocio) -> `409` (reqid `1106`, `1107`, `1109`, `1111`).
- `GET /backend/recepcion/unidades?tipo=APARTAMENTO` -> `200` (reqid `1108`).
- `GET /backend/recepcion/habitaciones` -> `200` (reqid `1110`).
- `POST /backend/reservas` (escenario exitoso creado en la sesion) -> `201` (reqid `1112`).

## Hallazgos adicionales

- Hallazgo de accesibilidad/UX (prioridad media): los modales `Buscar cliente`, `Nuevo cliente` y `Pago de reserva` no cierran con `Escape`.
- Hallazgo funcional/validacion (prioridad media): el campo `Numero de personas` mantiene `valuemax=0` y permite valores no consistentes con capacidad (ej. `3` en apartaestudio).
- Hallazgo funcional (prioridad media): al reabrir `Editar pago`, el monto manual se restablece a `TOTAL CALCULADO` y sobrescribe el resumen sin confirmacion.
- Hallazgo de consola DevTools: `No label associated with a form field` (msgid `158`, count: 4).

## Resumen (ronda adicional)

- Casos ejecutados: **17**
- OK: **12**
- FAIL: **5**
- BLOQUEADOS: **0**

## Resumen acumulado

- Casos ejecutados: **52**
- OK: **45**
- FAIL: **7**
- BLOQUEADOS: **0**
