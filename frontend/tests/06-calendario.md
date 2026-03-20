# Informe de pruebas - Vista Calendario

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/calendario`
- Metodo: pruebas manuales E2E con Chrome MCP (solo desktop)
- Flujos cubiertos: filtros, navegacion de rango, seleccion de celdas, modales de reserva/estancia, acciones rapidas y navegacion cruzada
- Mobile: no aplicado por indicacion del usuario
- Base de datos: no modificada manualmente

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| CAL-01 | Carga inicial de `calendario` | OK | Renderiza header `Calendario operativo` y grilla diaria |
| CAL-02 | Rango inicial visible | OK | Muestra `18/03/2026 - 01/04/2026` |
| CAL-03 | Columna `UNIDAD / HABITACION` visible | OK | Lista `APT-xxx`, `APT-xxx-Hx` y `AE-xxx` |
| CAL-04 | Estados iniciales activos en filtros | OK | Se ven `Reserva: 2`, `Estancia: 2`, `2 filtro(s) activo(s)` |
| CAL-05 | Filtro `Tipo de unidad = Apartaestudio` | OK | Refresca calendario con parametro `tipoUnidad=APARTAESTUDIO` |
| CAL-06 | Filtro `Tipo de unidad = Habitacion` | OK | Refresca calendario con parametro `tipoUnidad=HABITACION` |
| CAL-07 | Volver a `Tipo de unidad = Todas` | OK | Recupera vista general sin `tipoUnidad` |
| CAL-08 | Filtro `Codigo = AE-301` | OK | Solicitud con `codigoUnidad=AE-301` |
| CAL-09 | Filtro `Codigo` inexistente (`ZZZ`) | OK | Respuesta `200` sin eventos visibles para ese codigo |
| CAL-10 | Debounce en filtro `Codigo` | OK | La consulta se dispara tras pausa de tipeo (no por cada tecla) |
| CAL-11 | Activar estado reserva `Cancelada` | OK | Agrega `estadosReserva=CANCELADA` en query |
| CAL-12 | Activar estado estancia `Finalizada` | OK | Agrega `estadosEstancia=FINALIZADA` en query |
| CAL-13 | `Limpiar filtros` restaura defaults | OK | Vuelve a filtros base `CONFIRMADA/EXPIRADA` y `ACTIVA/EXCEDIDA` |
| CAL-14 | Navegacion rango con boton `<` | OK | Cambia a rango anterior |
| CAL-15 | Navegacion rango con boton `>` | OK | Regresa al rango posterior esperado |
| CAL-16 | Boton `Hoy` recentra rango actual | OK | Vuelve a mostrar la ventana con fecha actual |
| CAL-17 | Seleccion de celda futura libre | OK | Habilita `Crear reserva` y mantiene `Agregar estancia` disabled |
| CAL-18 | Seleccion de celda en hoy/pasado libre | OK | Habilita `Agregar estancia` y mantiene `Crear reserva` disabled |
| CAL-19 | Seleccion de celda bloqueada por evento | OK | Ambas acciones rapidas quedan disabled |
| CAL-20 | Click en evento de reserva abre modal | OK | Dialog `Detalle de reserva` visible |
| CAL-21 | Modal reserva muestra datos clave | OK | Codigo/estado/unidad/cliente visibles |
| CAL-22 | Modal reserva `Editar` | OK | Navega a `/reservas/nueva` en modo edicion |
| CAL-23 | `Volver` desde reserva editada | OK | Regresa a `calendario` |
| CAL-24 | Modal reserva `Dar entrada` | OK | Navega a `/estancias/nueva` en modo `Registrar ingreso` |
| CAL-25 | `Volver` desde ingreso | OK | Regresa a `calendario` |
| CAL-26 | Cierre manual de modal reserva | OK | `Cerrar` cierra dialog y conserva contexto de calendario |
| CAL-27 | Accion rapida `Crear reserva` desde barra | OK | Abre `/reservas/nueva` con fecha de entrada precargada (`2026-03-23`) |
| CAL-28 | `Volver` desde flujo crear reserva | OK | Retorna a `calendario` |
| CAL-29 | Accion rapida `Agregar estancia` desde barra | OK | Abre `/estancias/nueva` con fecha de entrada precargada (`2026-03-17`) |
| CAL-30 | `Volver` desde flujo agregar estancia | OK | Retorna a `calendario` |
| CAL-31 | Click en evento de estancia abre modal | OK | Dialog `Detalle de estancia` visible |
| CAL-32 | Modal estancia muestra datos clave | OK | `EST-49405C`, `AE-301`, cliente, fechas y estado `Activa` |
| CAL-33 | Modal estancia `Editar` | OK | Navega a `/estancias/nueva?codigo=AE-301&tipo=APARTAESTUDIO&editar=true&estanciaId=31` |
| CAL-34 | Precarga en edicion desde calendario | OK | `Tipo=Apartaestudio` y `Codigo=AE-301` cargados |
| CAL-35 | `Volver` desde edicion de estancia | OK | Regresa a `calendario` |
| CAL-36 | Modal estancia `Dar salida` | OK | Navega a `/estancias/salida` |
| CAL-37 | Precarga de salida desde calendario | OK | Carga `AE-301`, `APARTAESTUDIO`, fechas de estancia y cliente |
| CAL-38 | `Volver` desde salida | OK | Retorna a `calendario` |
| CAL-39 | Modal estancia `Eliminar` abre confirmacion | OK | Dialog `Eliminar estancia` visible |
| CAL-40 | `Cancelar` en confirmacion de eliminar | OK | Cierra confirmacion y mantiene detalle de estancia |
| CAL-41 | Cierre de modal detalle estancia | OK | `Cerrar` cierra dialog principal |
| CAL-42 | Texto de notas en detalle de estancia | FAIL | Muestra placeholders tecnicos: `NULL | NOTAS DE ACTIVACION: NULL` |
| CAL-43 | Comportamiento de celdas ocupadas | OK | Celdas con `reserva confirmada/expirada` o `estancia activa/excedida` no seleccionables |
| CAL-44 | Validacion de accesibilidad en sesion | OK | DevTools reporta issue `No label associated with a form field` (msgid `32`) |

## Evidencia de red observada

- `GET /backend/detalle/calendario?...` -> `200` en refrescos de filtros/rango (reqid `106`, `109`-`115`, `123`, `127`, `128`, `130`, `133`, `139`)
- `GET /backend/recepcion/unidades?tipo=APARTAMENTO` -> `200` (reqid `122`, `126`, `129`)
- `GET /backend/recepcion/unidades?tipo=APARTAESTUDIO` -> `200` (reqid `131`)
- `GET /backend/estancias/31` -> `200` (reqid `132`, `134`, `137`)
- `POST /backend/pagos/total` -> `200` (reqid `138`)

Detalle de calculo en salida (reqid `138`):

- Request body: `{"tipoUnidad":"APARTAESTUDIO","numeroPersonas":1,"fechaEntrada":"2026-03-14T00:00","fechaSalida":"2026-03-18T20:59","idEstancia":31,"tipoCalculo":"ESTANDAR"}`
- Response body: `-16000.00`

## Hallazgos

- Hallazgo funcional/UX (prioridad media): en detalle de estancia se muestran literales tecnicos en notas (`NULL | NOTAS DE ACTIVACION: NULL`), en lugar de texto limpio para usuario.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` (count: 4) durante la sesion.
- Observacion de negocio: en salida anticipada del caso probado, el calculo devolvio total negativo (`-16000.00`); conviene confirmar si el comportamiento esperado es saldo a favor.

## Resumen

- Casos ejecutados: **44**
- OK: **43**
- FAIL: **1**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| CAL-45 | Activar estado de estancia `Finalizada` desde filtros | OK | Contador cambia a `Estancia: 3` y aparece `Estados personalizados activos.` |
| CAL-46 | Filtro `Tipo de unidad = Apartaestudio` con estado finalizada activo | OK | Vista reducida a filas `AE-*` y chip `Tipo: Apartaestudio` |
| CAL-47 | Filtro `Codigo = AE-301` en combinacion con `Tipo = Apartaestudio` | OK | Se muestra chip `Codigo: AE-301` y eventos de `AE-301` |
| CAL-48 | Filtro `Codigo` inexistente (`ZZZ`) | FAIL | Aun con `Codigo: ZZZ` la grilla mantiene `AE-301` y eventos asociados |
| CAL-49 | `Limpiar filtros` restaura defaults de calendario | OK | Regresa a `Estancia: 2`, rango base y sin chips de tipo/codigo |
| CAL-50 | Navegacion temporal con `<` | OK | Rango cambia a `18/03/2026 - 01/04/2026` |
| CAL-51 | Navegacion temporal con `>` y retorno a ventana actual | OK | Rango vuelve a `19/03/2026 - 02/04/2026` |
| CAL-52 | Boton `Hoy` con rango ya centrado | OK | Mantiene rango actual sin perder contexto |
| CAL-53 | Seleccion de celda operativa libre (`APT-101 · 19/03/2026`) | OK | Celda marcada como `Seleccionado` y acciones rapidas habilitadas |
| CAL-54 | Accion rapida `Crear reserva` desde celda seleccionada | OK | Navega a `/reservas/nueva` con `Tipo=Apartamento`, `Codigo=APT-101`, `Entrada estimada=2026-03-19` |
| CAL-55 | Regreso desde `reservas/nueva` a `calendario` por historial | OK | Se recupera `calendario` y estado base de filtros |
| CAL-56 | Apertura de modal `Detalle de reserva` (`RES-57692W`) | OK | Muestra codigo, estado, unidad, cliente, fechas y acciones (`Eliminar/Editar/Dar entrada`) |
| CAL-57 | Cierre de modal de reserva con tecla `Escape` | FAIL | Modal permanece abierto; no responde a cierre por teclado |
| CAL-58 | Apertura de `Detalle de estancia` (`EST-77171D`) y cierre con `Escape` | FAIL | Modal de estancia permanece abierto con `Escape`; cierre manual `Cerrar` si funciona |

## Evidencia de red - ronda adicional

- `GET /backend/detalle/calendario?...&estadosEstancia=ACTIVA&estadosEstancia=EXCEDIDA&estadosEstancia=FINALIZADA` -> `200` (reqid `1026`, `1038`)
- `GET /backend/detalle/calendario?...&tipoUnidad=APARTAESTUDIO...` -> `200` (reqid `1027`)
- `GET /backend/detalle/calendario?...&tipoUnidad=APARTAESTUDIO&codigoUnidad=AE-301...` -> `200` (reqid `1028`)
- `GET /backend/detalle/calendario?...&tipoUnidad=APARTAESTUDIO&codigoUnidad=ZZZ...` -> `200` (reqid `1029`)
- `GET /backend/detalle/calendario?...&estadosEstancia=ACTIVA&estadosEstancia=EXCEDIDA` -> `200` (reqid `1030`, `1037`)
- `GET /backend/recepcion/unidades?tipo=APARTAMENTO` -> `200` (reqid `1036`)

## Hallazgos adicionales

- Hallazgo funcional (prioridad media): el filtro `Codigo` no respeta valor inexistente (`ZZZ`) y mantiene resultados visibles de `AE-301`.
- Hallazgo de accesibilidad/UX (prioridad media): el modal de `Detalle de reserva` no cierra con `Escape`.
- Hallazgo de accesibilidad/UX (prioridad media): el modal de `Detalle de estancia` tampoco cierra con `Escape` (patron consistente en modales operativos).
- Observacion de presentacion (prioridad baja): en detalle de `EST-77171D` las notas se siguen renderizando como `-SALIDA FINAL VALIDADA EN RONDA AGRESIVA` (prefijo con guion y mayusculas forzadas).
- Hallazgo de consola DevTools: `No label associated with a form field` (count: 4).

## Resumen (ronda adicional)

- Casos ejecutados: **14**
- OK: **11**
- FAIL: **3**
- BLOQUEADOS: **0**

## Resumen acumulado

- Casos ejecutados: **58**
- OK: **54**
- FAIL: **4**
- BLOQUEADOS: **0**
