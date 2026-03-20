# Informe de pruebas - Vista Pagos

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/pagos`
- Metodo: pruebas manuales E2E con Chrome MCP (solo desktop)
- Flujos cubiertos: filtros base/avanzados, modo pagar pendientes, eliminacion, paginacion y navegacion cruzada
- Mobile: no aplicado por indicacion del usuario
- Base de datos: no modificada manualmente

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| PAG-01 | Carga inicial de `pagos` | OK | Renderiza header, filtros y tabla |
| PAG-02 | Estado inicial `Eliminar pago` | OK | Inicia disabled sin seleccion |
| PAG-03 | Columnas principales visibles | OK | Reserva, estancia, tipo, monto, medio, estado, fecha, notas |
| PAG-04 | Paginacion inicial | OK | `Total: 25`, `Mostrando 1-10`, `Pagina 1 de 3` |
| PAG-05 | Expandir `Mas filtros` | OK | Muestra bloques `Estados` y `Medios de pago` |
| PAG-06 | Filtro rapido `Filtrar para pagar pendientes` | OK | Activa modo pendiente y cambia dataset |
| PAG-07 | Dataset en modo pendiente | OK | Lista solo `ESTANCIA_COMPLETADA` en `PENDIENTE` (2 filas) |
| PAG-08 | Mensajeria en modo pendiente | OK | Muestra `Salir modo pago pendiente` + instruccion operativa |
| PAG-09 | Intento de marcar pendiente como completado | OK | Checkbox abre modal `Confirmar pago pendiente` |
| PAG-10 | Cancelar confirmacion de pago pendiente | OK | Cierra modal sin ejecutar cambio |
| PAG-11 | Salir de modo pendiente | OK | Restaura boton `Filtrar para pagar pendientes` y dataset general |
| PAG-12 | Seleccionar fila `ELIMINADO` | OK | `Eliminar pago` permanece disabled por regla de negocio |
| PAG-13 | Mensaje de restriccion para estado eliminado | OK | Texto: `Solo se pueden eliminar pagos COMPLETADO o PENDIENTE` |
| PAG-14 | Seleccionar fila `COMPLETADO` | OK | `Eliminar pago` se habilita |
| PAG-15 | Abrir modal `Eliminar pago` | OK | Modal de confirmacion visible |
| PAG-16 | Cancelar eliminacion | OK | Cierra modal sin ejecutar accion |
| PAG-17 | Boton `Ver notas del pago` en fila con notas | OK | Abre modal de notas |
| PAG-18 | Cerrar modal de notas | OK | Retorna a tabla sin perder contexto |
| PAG-19 | `Ver estancia asociada` desde fila | OK | Navega a `/estancias?codigoEstancia=EST-51728P` |
| PAG-20 | Retorno a `pagos` desde menu | OK | Vista vuelve a cargar operativa |
| PAG-21 | Filtro por `Codigo reserva` + `Codigo estancia` incompatibles | OK | Resultado `No hay pagos con los filtros actuales` |
| PAG-22 | `Limpiar` tras combinacion sin resultados | OK | Restablece listado general |
| PAG-23 | Filtro por `Tipo de pago = Anticipo Reserva` | OK | Resultado acotado a pagos tipo reserva |
| PAG-24 | Navegacion `Ver reserva asociada` | OK | Redirige a `/reservas?codigoReserva=RES-16096V` |
| PAG-25 | Retorno a `pagos` desde menu | OK | Vista vuelve a cargar operativa |
| PAG-26 | Filtro avanzado `Estado=Completado` + `Medio=Transferencia Bancaria` | OK | Resultado acotado a 1 pago (`EST-28024M`) |
| PAG-27 | `Limpiar` tras filtros avanzados | OK | Dataset completo restaurado |
| PAG-28 | Filtro por rango de fechas (`2026-03-12` a `2026-03-15`) | OK | Query con `fechaDesde/fechaHasta` y resultado acotado |
| PAG-29 | `Limpiar` tras filtro por fechas | OK | Vacia fechas y restaura datos |
| PAG-30 | Paginacion `Siguiente` | OK | Cambia a pagina 2 (`Mostrando 11-20`) |
| PAG-31 | Paginacion `Anterior` | OK | Retorna a pagina 1 (`Mostrando 1-10`) |
| PAG-32 | Boton `Ver reserva asociada` deshabilitado cuando no aplica | OK | Filas sin reserva muestran boton disabled |
| PAG-33 | Boton `Ver estancia asociada` deshabilitado cuando no aplica | OK | Filas sin estancia muestran boton disabled |
| PAG-34 | Boton `Ver notas` deshabilitado cuando no hay notas | OK | Fila muestra `Sin notas` en estado disabled |
| PAG-35 | Etiquetado de tipo/estado en grilla | OK | Se observan `Anticipo Reserva`, `Anticipo Estancia`, `Estancia Completada`, `Pendiente`, `Completado`, `Eliminado` |
| PAG-36 | Resiliencia de tabla tras cambios de filtros/paginacion | OK | No se observaron bloqueos ni errores visibles de UI |

## Evidencia de red observada

- `GET /backend/pagos?page=0&size=10&sort=fecha,desc` -> `200` (reqid `370`, `373`, `378`, `380`, `382`, `388`, `390`, `392`, `394`)
- `GET /backend/pagos?...&estados=PENDIENTE&tipoPago=ESTANCIA_COMPLETADA` -> `200` (reqid `372`, `391`)
- `GET /backend/pagos?...&codigoReserva=RES-16096V&codigoEstancia=EST-51728P` -> `200` (reqid `379`)
- `GET /backend/pagos?...&tipoPago=ANTICIPO_RESERVA` -> `200` (reqid `383`)
- `GET /backend/pagos?...&estados=COMPLETADO&mediosPago=TRANSFERENCIA_BANCARIA` -> `200` (reqid `389`)
- `GET /backend/pagos?...&fechaDesde=2026-03-12T00:00:00&fechaHasta=2026-03-15T23:59:59` -> `200` (reqid `393`)
- `GET /backend/pagos?page=1&size=10&sort=fecha,desc` -> `200` (reqid `381`)
- Navegacion cruzada:
  - `GET /backend/estancias/tabla?...&codigoEstancia=EST-51728P` -> `200` (reqid `377`)
  - `GET /backend/reservas/tabla?...&codigoReserva=RES-16096V` -> `200` (reqid `387`)

## Hallazgos

- Observacion de UX/copy (prioridad baja): modal de notas muestra concatenacion compacta (`Pago #26 -probando`) que puede afectar legibilidad.
- Observacion funcional menor: al cancelar modal `Confirmar pago pendiente`, el checkbox de fila puede quedar marcado visualmente en la tabla hasta cambiar de estado/filtro.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` (msgid `75`, count: 2).

## Resumen

- Casos ejecutados: **36**
- OK: **36**
- FAIL: **0**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| PAG-37 | Activar modo `Filtrar para pagar pendientes` con dataset actual | OK | Modo activo muestra `Total: 3` pendientes (`EST-49405C`, `EST-35109K`, `EST-40373V`) |
| PAG-38 | Modal `Confirmar pago pendiente` cierre con `Escape` | FAIL | Modal permanece abierto; no responde a cierre por teclado |
| PAG-39 | Cancelar `Confirmar pago pendiente` | OK | Cierra modal; checkbox puede quedar marcado visualmente en fila cancelada |
| PAG-40 | Confirmar pago pendiente (`EST-35109K`) | OK | `PUT /backend/pagos/pagar-pendiente/24 -> 204`, toast `Pago pendiente marcado como completado.` y retorno a vista general |
| PAG-41 | Abrir modal `Notas del pago` (fila `EST-77171D`) | OK | Muestra `Pago #29 -Pago final transferencia completado` |
| PAG-42 | Modal `Notas del pago` cierre con `Escape` | FAIL | Modal permanece abierto; no cierra por teclado |
| PAG-43 | Cierre manual de modal `Notas del pago` | OK | Boton `Cerrar` funciona y retorna a tabla |
| PAG-44 | Abrir modal `Reemplazar pago de estancia finalizada` desde `Eliminar pago` | OK | Modal muestra formulario (`Monto`, `Medio`, `Fecha`, `Estado`, `Notas`) |
| PAG-45 | Modal `Reemplazar pago...` cierre con `Escape` | FAIL | Modal permanece abierto; no responde a `Escape` |
| PAG-46 | Cancelar modal `Reemplazar pago...` | OK | Cierra modal y conserva seleccion en la grilla |
| PAG-47 | Seleccionar fila en estado `Eliminado` | OK | `Eliminar pago` se deshabilita y muestra `Solo se pueden eliminar pagos COMPLETADO o PENDIENTE.` |
| PAG-48 | Navegacion cruzada `Ver estancia asociada` (`EST-77171D`) | OK | Navega a `/estancias?codigoEstancia=EST-77171D`; retorno a `pagos` exitoso |
| PAG-49 | Paginacion `Siguiente` en dataset general | OK | Cambia a `Mostrando 11-20`, `Pagina 2 de 3` |
| PAG-50 | Navegacion cruzada `Ver reserva asociada` desde pagina 2 | OK | Navega a `/reservas?codigoReserva=RES-16096V`; retorno a `pagos` exitoso |
| PAG-51 | Filtro avanzado `Estado=Completado` + `Medio=Transferencia Bancaria` | OK | Resultado acotado a `Total: 2` (`EST-77171D`, `EST-28024M`) |
| PAG-52 | `Limpiar` tras filtros avanzados | OK | Restaura dataset completo con `Total: 27` y paginacion `1 de 3` |

## Evidencia de red - ronda adicional

- `GET /backend/pagos?page=0&size=10&sort=fecha,desc` -> `200` (reqid `1200`, `1204`, `1209`, `1215`, `1217`).
- `GET /backend/pagos?page=0&size=10&sort=fecha,desc&estados=PENDIENTE&tipoPago=ESTANCIA_COMPLETADA` -> `200` (reqid `1202`).
- `PUT /backend/pagos/pagar-pendiente/24` -> `204` (reqid `1203`).
- `GET /backend/estancias/tabla?page=0&size=20&codigoEstancia=EST-77171D&sort=entradaReal,desc` -> `200` (reqid `1208`).
- `GET /backend/pagos?page=1&size=10&sort=fecha,desc` -> `200` (reqid `1210`).
- `GET /backend/reservas/tabla?page=0&size=20&codigoReserva=RES-16096V&sort=fechaCreacion,desc` -> `200` (reqid `1214`).
- `GET /backend/pagos?page=0&size=10&sort=fecha,desc&estados=COMPLETADO&mediosPago=TRANSFERENCIA_BANCARIA` -> `200` (reqid `1216`).

## Hallazgos adicionales

- Hallazgo de accesibilidad/UX (prioridad media): los modales `Confirmar pago pendiente`, `Notas del pago` y `Reemplazar pago de estancia finalizada` no cierran con `Escape`.
- Observacion funcional menor: al cancelar `Confirmar pago pendiente`, el checkbox de la fila puede quedar marcado visualmente.
- Observacion operativa: el dataset actualiza a `Total: 27` y el modo pendientes muestra `Total: 3` en esta ronda.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` (msgid `171`, count: 2).

## Resumen (ronda adicional)

- Casos ejecutados: **16**
- OK: **13**
- FAIL: **3**
- BLOQUEADOS: **0**

## Resumen acumulado

- Casos ejecutados: **52**
- OK: **49**
- FAIL: **3**
- BLOQUEADOS: **0**
