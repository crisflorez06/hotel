# Informe de pruebas - Vista Recepcion Panel

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/recepcion/panel`
- Metodo: pruebas manuales con Chrome MCP (solo desktop)
- Mobile: no aplicado por indicacion del usuario
- Preparacion backend: no requerida
- Base de datos: no utilizada

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| RCP-01 | Apertura sin `codigo` | OK | Mensaje: "No se recibió el código de la unidad." |
| RCP-02 | Apertura con `codigo=APT-201&tipo=APARTAMENTO` | OK | Carga unidad `APT-201` en estado `OCUPADO` |
| RCP-03 | Render de bloque Unidad (tipo/piso/habitaciones) | OK | Muestra `APARTAMENTO`, piso `2`, habitaciones `3` |
| RCP-04 | Render de ocupacion de habitaciones | OK | Muestra `3 de 3 habitaciones` |
| RCP-05 | Apertura con codigo inexistente | OK | Mensaje: "No se encontró la unidad solicitada." |
| RCP-06 | Apertura con `codigo=APT-101` (disponible) | OK | Muestra estado `DISPONIBLE` |
| RCP-07 | Estado vacio de estancia en unidad disponible | OK | "No existe estancia activa..." |
| RCP-08 | Estado vacio de reserva en unidad disponible | OK | "No existe una reserva activa asociada." |
| RCP-09 | Boton `Consultar estancias` | OK | Navega a `/estancias?codigoUnidad=APT-201` |
| RCP-10 | Boton `Consultar reservas` | OK | Navega a `/reservas?codigoUnidad=APT-201` |
| RCP-11 | Link ocupante a clientes (desde estancia) | OK | Navega a `/ocupantes/tabla-clientes?numeroDocumento=23232&tipoDocumento=TI` |
| RCP-12 | Link cliente a clientes (desde reserva) | OK | Navega a `/ocupantes/tabla-clientes?numeroDocumento=23232&tipoDocumento=TI` |
| RCP-13 | Navegacion a subpanel de habitacion desde lista interna | OK | Navega a `/recepcion/panel?codigo=APT-201-H1&tipo=HABITACION` |
| RCP-14 | Boton `Volver` con `returnTo` (subpanel abierto desde panel) | OK | Regresa a `/recepcion/panel?codigo=APT-201&tipo=APARTAMENTO` |
| RCP-15 | Apertura directa de panel de habitacion (`APT-201-H1`) | OK | Carga `OCUPADO` con tipo `HABITACION` |
| RCP-16 | Coherencia estado habitacion ocupada vs detalle | FAIL | Aparece `OCUPADO` pero sin estancia/reserva asociada |
| RCP-17 | Boton `Volver` en panel habitacion abierto desde recepcion | FAIL | Permanece en la misma URL, no retorna |
| RCP-18 | Boton `Finalizar` en estancia activa | OK | Navega a `/estancias/salida` con datos precargados |
| RCP-19 | Link `Editar` estancia | OK | Navega a `/estancias/nueva?codigo=APT-201&tipo=APARTAMENTO&editar=true&estanciaId=37` |
| RCP-20 | Apertura modal `Eliminar estancia` | OK | Modal visible con accion irreversible |
| RCP-21 | Cierre modal `Eliminar estancia` por `Cancelar` | OK | Modal cierra sin ejecutar eliminacion |
| RCP-22 | Disponibilidad de acciones de reserva para estado `COMPLETADA` | OK | No aparecen botones `Entrada`, `Editar`, `Eliminar` de reserva |
| RCP-23 | Apertura modal `Agregar pago` | OK | Muestra calculo (`Tipo`, `Dias`, `Personas`, `Total`) |
| RCP-24 | Carga de total calculado en modal pago | OK | Total calculado visible: `300,000` |
| RCP-25 | Validacion modal pago con monto `0` y guardar | OK | Error: "Completa los campos de pago obligatorios." |
| RCP-26 | Cambio de medio de pago en modal | OK | Cambio a `TRANSFERENCIA_BANCARIA` aplicado |
| RCP-27 | Edicion de notas en modal pago | OK | Campo notas editable |
| RCP-28 | Cierre modal pago por boton `Cerrar` | OK | Modal cierra correctamente |
| RCP-29 | Cierre modal pago por boton `Cancelar` | OK | Modal cierra correctamente |
| RCP-30 | Boton `Volver` desde panel de apartamento (acceso directo) | OK | Regresa a `/recepcion` |

## Evidencia de red observada

- `GET /backend/recepcion/unidades?tipo=APARTAMENTO&codigo=APT-201` -> `200`
- `GET /backend/estancias/37` -> `200`
- `GET /backend/reservas/27` -> `200`
- `GET /backend/recepcion/habitaciones?codigo=APT-201-H1` -> `200`
- `GET /backend/recepcion/unidades?tipo=APARTAMENTO&codigo=NOEXISTE` -> `200`

## Hallazgos

- Hallazgo funcional 1 (critico de consistencia): en `HABITACION` se puede ver estado `OCUPADO` sin datos de estancia/reserva en el panel.
- Hallazgo funcional 2 (navegacion): `Volver` en panel de habitacion abierto directamente desde recepcion no retorna y deja al usuario en la misma URL.
- En el escenario probado (`RES-80320Y` completada), el sistema oculta correctamente acciones de gestion de reserva.

## Resumen

- Casos ejecutados: **30**
- OK: **28**
- FAIL: **2**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

- Cobertura: solo desktop (`1366x768`)
- Objetivo: estresar navegacion contextual, modales, acciones CRUD y consistencia post-accion

### Casos adicionales ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| RCP-31 | Revalidacion de inconsistencia en habitacion ocupada sin detalle | FAIL | `APT-201-H1/H2` muestran estado `OCUPADO` pero en panel aparece `No existe estancia activa` y `No existe una reserva activa asociada` |
| RCP-32 | Revalidacion de `Volver` en panel de habitacion (acceso directo) | OK | En `codigo=APT-201-H2&tipo=HABITACION`, `Volver` retorna a `/recepcion` |
| RCP-33 | `Volver` en subpanel abierto desde panel de apartamento | OK | `APT-201-H1` (abierto desde APT-201) retorna a `/recepcion/panel?codigo=APT-201&tipo=APARTAMENTO` |
| RCP-34 | Navegacion `Consultar estancias` desde panel | OK | Redireccion a `/estancias?codigoUnidad=APT-201` con filtro `Codigo unidad` precargado |
| RCP-35 | Navegacion `Consultar reservas` desde panel | OK | Redireccion a `/reservas?codigoUnidad=APT-201` con filtro `Codigo unidad` precargado |
| RCP-36 | Link de cliente desde bloque estancia a tabla clientes | OK | Navega a `/ocupantes/tabla-clientes?numeroDocumento=23232&tipoDocumento=TI` |
| RCP-37 | Modal `Agregar pago`: validacion de monto `0` | OK | Error inline: `Completa los campos de pago obligatorios.` |
| RCP-38 | Modal `Agregar pago`: cierre por tecla `Escape` | FAIL | `Escape` no cierra el modal de pago; cierra correctamente con boton `Cerrar` |
| RCP-39 | Navegacion a `Editar estancia` desde panel y retorno | FAIL | Abre `/estancias/nueva?...editar=true&estanciaId=37`, pero `Volver` retorna a `/recepcion` (pierde contexto de panel) |
| RCP-40 | Eliminacion de estancia desde panel (flujo completo) | FAIL | `PUT /backend/estancias/eliminar/37` -> `204`, pero tras confirmar redirige a `/monitor-eventos` (no retorna a panel/recepcion esperado) |
| RCP-41 | Coherencia post-eliminacion en recepcion para `APT-201` | OK | Unidad pasa a `DISPONIBLE` con `Ocupacion 0/3` y habitaciones con acciones `Crear estancia/Crear reserva` |

### Evidencia de red adicional

- Carga panel y detalle asociado:
  - `GET /backend/recepcion/unidades?tipo=APARTAMENTO&codigo=APT-201` -> `200` (reqid `445`, `459`)
  - `GET /backend/estancias/37` -> `200` (reqid `448`, `460`)
  - `GET /backend/reservas/27` -> `200` (reqid `449`, `461`)
- Modal de pago:
  - `POST /backend/pagos/total` -> `200` (reqid `450`)
- Eliminacion de estancia:
  - `PUT /backend/estancias/eliminar/37` -> `204` (reqid `462`)
- Navegacion posterior inesperada:
  - `GET /backend/eventos?page=0&size=20&sort=fecha,desc` -> `200` (reqid `466`)

### Hallazgos adicionales

- Se mantiene hallazgo de consistencia funcional: habitaciones en `OCUPADO` sin detalle de estancia/reserva en panel (`RCP-31`).
- Hallazgo UX: modal `Agregar pago` no responde a cierre por `Escape` (`RCP-38`).
- Hallazgo de navegacion: `Volver` desde `Editar estancia` no conserva contexto de panel (`RCP-39`).
- Hallazgo de flujo critico: al confirmar eliminacion de estancia en panel, la app redirige a `monitor-eventos` en lugar de permanecer en flujo de recepcion (`RCP-40`).

### Resumen de ronda adicional

- Casos adicionales ejecutados: **11**
- OK: **7**
- FAIL: **4**
- BLOQUEADOS: **0**
