# Informe de pruebas - Vista Reservas

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/reservas`
- Metodo: pruebas manuales E2E con Chrome MCP (solo desktop)
- Flujos cubiertos: filtros basicos y avanzados, acciones de fila/toolbar, paginacion y navegacion cruzada
- Mobile: no aplicado por indicacion del usuario
- Base de datos: no modificada manualmente

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| RES-01 | Carga inicial de `reservas` | OK | Renderiza header `Reservas` y tabla operativa |
| RES-02 | Estado inicial de acciones masivas | OK | `Dar entrada`, `Editar`, `Eliminar` inician disabled sin seleccion |
| RES-03 | Columnas principales visibles | OK | Codigo, tipo, unidad, cliente, canal, fechas, estado, pagos y notas |
| RES-04 | Paginacion inicial | OK | Se muestra `Total: 28`, `Mostrando 1-20`, `Pagina 1 de 2` |
| RES-05 | Filtro rapido `Hoy` | OK | Completa `rangoGeneralDesde/Hasta` del dia actual |
| RES-06 | Aplicar filtros con `Hoy` | OK | Consulta con rango del dia y lista acotada |
| RES-07 | Limpiar filtros tras `Hoy` | OK | Restaura consulta general sin rango |
| RES-08 | Filtro por `Codigo reserva` | OK | Consulta con `codigoReserva` (ej. `RES-30114F`) |
| RES-09 | Filtro por `Codigo unidad` | OK | Consulta con `codigoUnidad` (ej. `APT-101-H1`) |
| RES-10 | Filtro por `Nombre cliente` | OK | Consulta con `nombreCliente` (ej. `ejemplo`) |
| RES-11 | Filtro por `Documento cliente` | OK | Consulta con `numeroDocumentoCliente` (ej. `23232`) |
| RES-12 | Limpiar despues de filtros por texto | OK | Regresa a dataset general |
| RES-13 | Paginacion boton `Siguiente` | OK | Cambia a pagina 2 (`Mostrando 21-28`) |
| RES-14 | Paginacion boton `Anterior` | OK | Retorna a pagina 1 (`Mostrando 1-20`) |
| RES-15 | Abrir panel `Mas filtros` | OK | Despliega bloques de tipo, estado, canal y rangos avanzados |
| RES-16 | Chip `Tipo de unidad = Apartaestudio` | OK | Permite combinar filtro avanzado por tipo |
| RES-17 | Chip `Estado = Confirmada` | OK | Permite combinar filtro avanzado por estado |
| RES-18 | Chip `Canal = Whatsapp` | OK | Permite combinar filtro avanzado por canal |
| RES-19 | Aplicar filtros avanzados combinados | OK | Resultado acotado a 1 registro (`RES-56506N`) |
| RES-20 | Limpiar tras filtros avanzados | OK | Recupera listado completo de reservas |
| RES-21 | Seleccionar fila `Cancelada` | OK | Acciones masivas permanecen disabled por regla de negocio |
| RES-22 | Mensajes de restriccion en estado cancelado | OK | Tooltip/texto indica solo `confirmadas/expiradas` para operar |
| RES-23 | Seleccionar fila `Confirmada` | OK | `Dar entrada`, `Editar`, `Eliminar` se habilitan |
| RES-24 | Deseleccionar fila activa | OK | Acciones vuelven a estado disabled |
| RES-25 | Accion toolbar `Editar` con fila valida | OK | Navega a `/reservas/nueva` en modo edicion |
| RES-26 | Boton `Volver` desde edicion | OK | Regresa a `reservas` |
| RES-27 | Accion toolbar `Dar entrada` con fila valida | OK | Navega a `/estancias/nueva` en modo ingreso |
| RES-28 | Boton `Volver` desde ingreso | OK | Regresa a `reservas` |
| RES-29 | Boton `Nueva reserva` | OK | Navega a `/reservas/nueva` en modo creacion |
| RES-30 | Boton `Volver` desde nueva reserva | OK | Regresa a `reservas` |
| RES-31 | Boton por fila `Ver notas de la reserva` | OK | Abre modal de notas |
| RES-32 | Cierre de modal de notas | OK | Modal cierra y conserva contexto de tabla |
| RES-33 | Boton por fila `Ver pagos de la reserva` habilitado | OK | Navega a `/pagos` con query params |
| RES-34 | Query params en `Ver pagos` | OK | URL incluye `codigoReserva` y `tipoPago=ANTICIPO_RESERVA` |
| RES-35 | Retorno desde `pagos` a `reservas` | OK | Navegacion por menu funciona sin errores |
| RES-36 | Boton por fila `Ver estancia asociada` | OK | Navega a `/estancias?codigoEstancia=...` |
| RES-37 | Retorno desde `estancias` a `reservas` | OK | Navegacion por menu funciona sin errores |
| RES-38 | Boton por fila `Ver cliente en tabla de clientes` | OK | Navega a `/ocupantes/tabla-clientes` con documento en query |
| RES-39 | Retorno desde `clientes` a `reservas` | OK | Navegacion por menu funciona sin errores |
| RES-40 | Estado disabled de `Ver pagos` cuando no aplica | OK | Filas sin pago asociado mantienen boton disabled |
| RES-41 | Estado disabled de `Ver estancia` cuando no aplica | OK | Filas sin estancia asociada mantienen boton disabled |
| RES-42 | Visualizacion de canales en grilla | OK | Se observan valores `Mostrador`, `Whatsapp`, `Email`, `Telefono` |

## Evidencia de red observada

- `GET /backend/reservas/tabla?page=0&size=20&sort=fechaCreacion,desc` -> `200` (reqid `177`, `188`, `190`, `200`, `202`, `204`, `206`, `208`, `210`, `214`, `216`, `220`, `225`, `227`)
- `GET /backend/reservas/tabla?page=1&size=20&sort=fechaCreacion,desc` -> `200` (reqid `191`)
- `GET /backend/reservas/tabla?...&codigoReserva=RES-30114F` -> `200` (reqid `201`)
- `GET /backend/reservas/tabla?...&codigoUnidad=APT-101-H1` -> `200` (reqid `203`)
- `GET /backend/reservas/tabla?...&nombreCliente=ejemplo` -> `200` (reqid `205`)
- `GET /backend/reservas/tabla?...&numeroDocumentoCliente=23232` -> `200` (reqid `207`)
- `GET /backend/reservas/tabla?...&rangoGeneralDesde=2026-03-18T00:00:00&rangoGeneralHasta=2026-03-18T23:59:59` -> `200` (reqid `209`)
- `GET /backend/reservas/tabla?...&estados=CONFIRMADA&canales=WHATSAPP&tipoUnidad=APARTAESTUDIO` -> `200` (reqid `226`)
- `GET /backend/pagos?...&tipoPago=ANTICIPO_RESERVA&codigoReserva=RES-80320Y` -> `200` (reqid `215`)
- `GET /backend/estancias/tabla?...&codigoEstancia=EST-51728P` -> `200` (reqid `213`)
- `GET /backend/ocupantes/tabla-clientes?...&numeroDocumento=123456789` -> `200` (reqid `219`)

## Hallazgos

- Hallazgo de UX/copy (prioridad baja): en filtros avanzados, el canal se muestra como `Airbinb` (typo), deberia ser `Airbnb`.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` durante la sesion (msgid `53`, count: 6).

## Resumen

- Casos ejecutados: **42**
- OK: **42**
- FAIL: **0**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| RES-43 | Refresco de dataset en carga actual | OK | Contador actualizado a `Total: 29` y pagina `2 de 2` |
| RES-44 | Filtro rapido `Manana` + aplicar | OK | Resultado acotado a `Total: 1` (reserva `RES-20255E`) |
| RES-45 | Filtro por codigo inexistente `RES-NO-EXISTE-999` | OK | Estado vacio visible (`No hay reservas...`) y `Total: 0` |
| RES-46 | Limpiar despues de filtro sin resultados | OK | Recupera listado completo con `Total: 29` |
| RES-47 | Accion por fila `Ver pagos de la reserva` habilitada | OK | Navega a `/pagos` con reserva valida |
| RES-48 | Query params en navegacion a `pagos` | OK | URL incluye `codigoReserva=RES-80320Y` y `tipoPago=ANTICIPO_RESERVA` |
| RES-49 | Paginacion `Siguiente` con tamano 20 | OK | Cambia a `Mostrando 21-29`, `Pagina 2 de 2` |
| RES-50 | Seleccion de reserva `Confirmada` (RES-20255E) | OK | Habilita `Dar entrada`, `Editar` y `Eliminar` |
| RES-51 | Seleccion posterior de reserva `Cancelada` (RES-86098X) | OK | Reemplaza seleccion y deshabilita acciones con mensajes de restriccion |
| RES-52 | Accion masiva `Eliminar` con reserva confirmada | OK | Abre modal `Cancelar reserva` con codigo `RES-20255E` |
| RES-53 | Cierre con tecla `Escape` en modal `Cancelar reserva` | FAIL | Modal permanece abierto; no responde a cierre por teclado |
| RES-54 | Boton `Cancelar` en modal de eliminacion | OK | Cierra modal y conserva la reserva sin cambios visibles |
| RES-55 | Cambio de `Tamano pagina` de 20 a 10 | OK | Recalcula a `Mostrando 1-10`, `Pagina 1 de 3` |
| RES-56 | Paginacion con `Tamano pagina = 10` | OK | Permite avanzar a `Pagina 3 de 3` con `Mostrando 21-29` y `Siguiente` disabled |
| RES-57 | Cierre con tecla `Escape` en modal `Notas de reserva` | FAIL | Modal (`RES-30114F`) permanece abierto; no cierra con teclado |
| RES-58 | Cierre manual del modal `Notas de reserva` | OK | Boton `Cerrar` funciona y retorna a la tabla |
| RES-59 | Copy en canal de filtros avanzados (`Airbinb`) | FAIL | Se mantiene typo visible en chip/boton de canal; esperado `Airbnb` |

## Evidencia de red - ronda adicional

- `GET /backend/reservas/tabla?page=0&size=20&sort=fechaCreacion,desc` -> `200` (reqid `1041`, `1044`, `1046`, `1050`)
- `GET /backend/reservas/tabla?page=0&size=20&rangoGeneralDesde=2026-03-20T00:00:00&rangoGeneralHasta=2026-03-20T23:59:59&sort=fechaCreacion,desc` -> `200` (reqid `1043`)
- `GET /backend/reservas/tabla?page=0&size=20&codigoReserva=RES-NO-EXISTE-999&sort=fechaCreacion,desc` -> `200` (reqid `1045`)
- `GET /backend/pagos?page=0&size=10&sort=fecha,desc&tipoPago=ANTICIPO_RESERVA&codigoReserva=RES-80320Y` -> `200` (reqid `1049`)
- `GET /backend/reservas/tabla?page=1&size=20&sort=fechaCreacion,desc` -> `200` (reqid `1051`)
- `GET /backend/reservas/tabla?page=0&size=10&sort=fechaCreacion,desc` -> `200` (reqid `1052`)
- `GET /backend/reservas/tabla?page=1&size=10&sort=fechaCreacion,desc` -> `200` (reqid `1053`)
- `GET /backend/reservas/tabla?page=2&size=10&sort=fechaCreacion,desc` -> `200` (reqid `1054`)

## Hallazgos adicionales

- Hallazgo de accesibilidad/UX (prioridad media): el modal `Cancelar reserva` no cierra con `Escape`.
- Hallazgo de accesibilidad/UX (prioridad media): el modal `Notas de reserva` tampoco cierra con `Escape`.
- Hallazgo de UX/copy (prioridad baja): en filtros avanzados se mantiene el typo `Airbinb` en lugar de `Airbnb`.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` durante la sesion (msgid `147`, count: 6).

## Resumen (ronda adicional)

- Casos ejecutados: **17**
- OK: **14**
- FAIL: **3**
- BLOQUEADOS: **0**

## Resumen acumulado

- Casos ejecutados: **59**
- OK: **56**
- FAIL: **3**
- BLOQUEADOS: **0**
