# Informe de pruebas - Vista Estancias Nueva

Fecha: 2026-03-18

## Alcance

- Vista principal validada: `http://localhost:4200/estancias/nueva`
- Modos revisados: nueva estancia, edicion (`?editar=true`), ingreso desde reserva (`?flujo=INGRESO`)
- Metodo: pruebas manuales con Chrome MCP (solo desktop)
- Mobile: no aplicado por indicacion del usuario
- Base de datos: no tocada manualmente

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| EN-01 | Carga inicial de la vista nueva | OK | Header `Nueva estancia` y bloques principales visibles |
| EN-02 | Campo `Codigo` deshabilitado sin `Tipo de unidad` | OK | Combo de codigo inicia deshabilitado |
| EN-03 | Seleccionar `Apartamento` habilita codigos | OK | Carga `APT-101`, `APT-201`, `APT-301` |
| EN-04 | Seleccionar `Habitacion` carga codigos por habitacion | OK | Carga `APT-xxx-Hx` |
| EN-05 | Seleccionar `Apartaestudio` carga codigos correctos | OK | Carga `AE-201`, `AE-202`, `AE-301`, `AE-302` |
| EN-06 | Cambiar tipo actualiza resumen de `TIPO` | OK | Resumen refleja `APARTAMENTO/HABITACION/APARTAESTUDIO` |
| EN-07 | Seleccionar `APT-101` actualiza resumen de unidad | OK | Resumen `UNIDAD = APT-101` |
| EN-08 | Abrir modal `Buscar cliente` | OK | Modal abre con campo `Cedula *` |
| EN-09 | Buscar cliente sin cedula | OK | Mensaje `Ingresa la cedula para buscar.` |
| EN-10 | Buscar cliente por documento parcial (`000`) | OK | Lista de resultados con boton `Seleccionar` |
| EN-11 | Seleccionar cliente desde busqueda | OK | Cliente asignado `Laura Perez` en formulario y resumen |
| EN-12 | Abrir modal `Agregar cliente` | OK | Modal `Nuevo cliente` visible |
| EN-13 | Guardar cliente sin nombres/apellidos | OK | Mensaje `Completa los campos obligatorios.` |
| EN-14 | Cancelar modal `Nuevo cliente` | OK | Cierra modal sin crear cliente |
| EN-15 | Abrir modal `Buscar acompanante` | OK | Modal abre con campo `Documento *` |
| EN-16 | Buscar acompanante sin documento | OK | Mensaje `Ingresa el documento para buscar.` |
| EN-17 | Buscar acompanante existente (`123456789`) | OK | Lista con `Juan Rodriguez` |
| EN-18 | Seleccionar acompanante desde busqueda | OK | Se agrega etiqueta de acompanante |
| EN-19 | Prevencion de duplicado de acompanante | OK | Boton cambia a `Agregado` deshabilitado |
| EN-20 | Impacto de acompanante en personas del resumen | OK | `PERSONAS` pasa de `1` a `2` |
| EN-21 | Abrir modal `Nuevo acompanante` | OK | Modal visible |
| EN-22 | Guardar acompanante sin nombres/apellidos | OK | Mensaje `Completa los campos obligatorios.` |
| EN-23 | Cancelar modal `Nuevo acompanante` | OK | Cierra modal sin crear acompanante |
| EN-24 | Quitar acompanante desde etiqueta | OK | Se elimina etiqueta y `PERSONAS` vuelve a `1` |
| EN-25 | Seleccionar `Entrada real` desde datepicker | OK | Valor `2026-03-18 12:00` |
| EN-26 | Seleccionar `Salida estimada` desde datepicker | OK | Valor `2026-03-20 12:00` |
| EN-27 | Cerrar datepicker por teclado (`Esc`) | OK | Datepicker cierra sin bloquear formulario |
| EN-28 | Campo `Meses` habilitado al tener entrada | OK | Input meses deja de estar deshabilitado |
| EN-29 | Meses = 1 autocompleta salida (+30 dias) | OK | `SALIDA = 2026-04-17 12:00` |
| EN-30 | Resumen muestra bloque de `MESES` cuando aplica | OK | Se renderiza `MESES = 1` |
| EN-31 | Abrir modal `Pago inicial` con datos completos | OK | Modal abre con meta `DIAS` y `PERSONAS` |
| EN-32 | Calculo automatico de pago en modal (nueva) | OK | `TOTAL CALCULADO = 3,000,000` |
| EN-33 | Guardar pago y reflejar en resumen | OK | Boton cambia a `Editar pago`, total `3,000,000` |
| EN-34 | Reabrir pago y ejecutar `Quitar pago` | OK | Resumen vuelve a `ANTICIPO EN ESTANCIA = 0` |
| EN-35 | Guardar estancia sin campos obligatorios completos | OK | Mensaje `Completa los campos obligatorios.` |
| EN-36 | Boton `Volver` desde nueva estancia | OK | Navega a `/recepcion` |
| EN-37 | Modo ingreso (`flujo=INGRESO`) muestra bloque reserva | OK | Header `Registrar ingreso` y card `Informacion de la reserva` |
| EN-38 | Modo edicion (`editar=true&estanciaId=31`) precarga completa de ficha | FAIL | Header `Editar estancia`, pero `tipo/codigo` quedan en `Selecciona...` y resumen `UNIDAD/TIPO` en `-` |

## Evidencia de red observada

- `GET /backend/recepcion/unidades?tipo=APARTAMENTO` -> `200` (reqid `1229`, `1376`)
- `POST /backend/pagos/total` -> `200` (reqid `1230`, `1379`)
- `GET /backend/estancias/31` -> `200` (reqid `1331`)
- Busqueda de ocupantes por documento (cliente/acompanante) operativa durante modales (200)

## Hallazgos

- Hallazgo funcional: en modo edicion con `?editar=true&estanciaId=31` no se precargan correctamente `tipoUnidad` y `codigo` en el formulario/resumen, aunque si llega informacion de cliente y fechas.
- Hallazgo a11y DevTools: `No label associated with a form field` y `A form field element should have an id or name attribute`.
- Observacion UX: en nueva estancia el boton `Volver` redirigio a `recepcion` en el flujo probado.

## Resumen

- Casos ejecutados: **38**
- OK: **37**
- FAIL: **1**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| EN-39 | Revalidacion de edicion con `?editar=true&estanciaId=31` | FAIL | `Tipo/Codigo` en `Selecciona...`, resumen `UNIDAD/TIPO = -` |
| EN-40 | Intento de `Actualizar estancia` en `estanciaId=31` con nota | FAIL | Mensaje `Completa los campos obligatorios.` pese a datos precargados de cliente/fechas |
| EN-41 | Flujo `?flujo=INGRESO&reservaId=31` | OK con observacion | Se muestra bloque `Informacion de la reserva`, pero datos de reserva quedan en `-` |
| EN-42 | Nueva estancia: validacion por fechas invertidas (`salida < entrada`) | OK | Mensaje `La fecha de salida debe ser posterior a la fecha de entrada` |
| EN-43 | Nueva estancia: validacion por entrada futura | OK | Mensaje `La fecha de entrada no puede ser superior a la fecha actual...` |
| EN-44 | Nueva estancia en `APT-201` con conflicto por reservas | OK | Bloquea creacion con detalle de habitaciones `APT-201-H1/H2/H3` |
| EN-45 | Nueva estancia en `APT-101` con conflicto por reservas | OK con observacion | Bloquea creacion; mensaje repite codigos de habitaciones (`H1/H2/H3`) |
| EN-46 | Creacion exitosa de estancia en `AE-301` | OK | `POST /backend/estancias -> 201`, redirige a `/recepcion` con toast de exito |
| EN-47 | Verificacion post-creacion en tabla de estancias | OK | Nueva fila `EST-77171D`, `AE-301`, estado `Activa`, total cambia a `24` |
| EN-48 | Edicion de la nueva estancia (`estanciaId=40`) | OK | Navega a editar con `tipo/codigo` precargados correctamente |
| EN-49 | Regreso con `Volver` desde editar (`estanciaId=40`) | OK | Retorna a `/estancias` |

## Evidencia de red observada (ronda adicional)

- `GET /backend/recepcion/unidades?tipo=APARTAMENTO` -> `200` (reqid `756`)
- `GET /backend/recepcion/unidades?tipo=APARTAESTUDIO` -> `200` (reqid `762`, `773`)
- `GET /backend/ocupantes/buscar?documento=123456789` -> `200` (reqid `757`)
- `POST /backend/estancias` -> `400` por validacion de fechas (reqid `758`, `759`)
- `POST /backend/estancias` -> `409` por conflicto de reservas/unidad (reqid `760`, `761`)
- `POST /backend/estancias` -> `201` creacion exitosa (reqid `763`)
- `GET /backend/estancias/tabla?page=0&size=20&sort=entradaReal,desc` -> `200` (reqid `771`, `775`)
- `GET /backend/estancias/40` -> `200` (reqid `774`)

## Hallazgos (ronda adicional)

- Se confirma el hallazgo previo en edicion de `estanciaId=31`: no precarga `tipo/codigo` y bloquea actualizacion por validacion obligatoria.
- Inconsistencia de edicion: `estanciaId=40` si precarga correctamente `tipo/codigo`; el fallo no afecta a todos los registros.
- Observacion UX: tras corregir fechas, mensajes de validacion previos pueden permanecer visibles hasta nuevo intento de guardado.
- Observacion de mensaje backend: en conflicto de `APT-101` se repiten codigos de habitaciones en el detalle del error (`H1/H2/H3` duplicados).

## Resumen (ronda adicional)

- Casos ejecutados: **11**
- OK: **7**
- FAIL: **2**
- OK con observacion: **2**
- BLOQUEADOS: **0**

## Resumen acumulado

- Casos ejecutados: **49**
- OK: **44**
- FAIL: **3**
- OK con observacion: **2**
- BLOQUEADOS: **0**
