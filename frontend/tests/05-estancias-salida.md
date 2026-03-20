# Informe de pruebas - Vista Estancias Salida

Fecha: 2026-03-18

## Alcance

- Vista validada: `http://localhost:4200/estancias/salida`
- Metodo: pruebas manuales E2E con Chrome MCP (solo desktop)
- Entradas probadas: acceso directo y acceso desde `recepcion` con boton `Dar salida`
- Mobile: no aplicado por indicacion del usuario
- Base de datos: no modificada manualmente

## Entorno

- Frontend: `http://localhost:4200`
- API via proxy: `/backend`
- Viewport: `1366x768`

## Casos ejecutados

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| SAL-01 | Carga de vista `estancias/salida` | OK | Encabezado `Registrar salida` renderiza correctamente |
| SAL-02 | Acceso directo sin contexto (pestana aislada) | OK | Mensaje `No se encontro la estancia para finalizar.` |
| SAL-03 | Boton `Confirmar salida` deshabilitado sin `estanciaId` | OK | Estado disabled en modo sin contexto |
| SAL-04 | Campos de datos base deshabilitados en modo sin contexto | OK | `Codigo/Tipo/Entrada/Salida estimada` disabled |
| SAL-05 | `Salida real` autocompletada al abrir vista sin contexto | OK | Valor datetime local precargado |
| SAL-06 | Acceso desde `recepcion` usando `Dar salida` | OK | Vista hidrata `AE-201` con cliente y fechas |
| SAL-07 | Precarga de `Codigo` | OK | `AE-201` |
| SAL-08 | Precarga de `Tipo de unidad` | OK | `APARTAESTUDIO` |
| SAL-09 | Precarga de `Entrada real` | OK | `2026-03-09 09:26` |
| SAL-10 | Precarga de `Salida estimada` | OK | `2026-03-16 16:26` |
| SAL-11 | Resumen de salida muestra cliente | OK | `Juan Rodriguez` |
| SAL-12 | Resumen de pago muestra anticipos acumulados | OK | `TOTAL ANTICIPOS = 0` |
| SAL-13 | Calculo inicial de pago al cargar con contexto | OK | `TOTAL CALCULADO = 3,240,000` |
| SAL-14 | Metricas de calculo visibles | OK | `DIAS = 9`, `PERSONAS = 1` |
| SAL-15 | Bloque `PAGO PENDIENTE` visible en salida | OK | `+ $ 540.000` |
| SAL-16 | Edicion manual de `Monto` | OK | Cambio a `3,000,000` aplicado |
| SAL-17 | Resumen refleja monto manual editado | OK | `TOTAL PAGO SALIDA = 3,000,000` |
| SAL-18 | Resumen recalcula gran total con monto manual | OK | `GRAN TOTAL = 3,000,000` |
| SAL-19 | Cambio de `Medio de pago` | OK | `TRANSFERENCIA_BANCARIA` aplicado |
| SAL-20 | Resumen refleja `Medio de pago` actualizado | OK | Campo `MEDIO` sincronizado |
| SAL-21 | Cambio de `Estado` de pago | OK | `COMPLETADO` aplicado |
| SAL-22 | Resumen refleja `Estado` actualizado | OK | Campo `ESTADO` sincronizado |
| SAL-23 | Edicion de `Notas de salida` | OK | Texto libre aplicado en formulario |
| SAL-24 | Edicion de `Notas del pago` | OK | Texto libre aplicado en formulario |
| SAL-25 | Apertura de modal `Confirmar salida` | OK | Modal operativo visible |
| SAL-26 | Cierre de modal con `Cancelar` | OK | Modal cierra sin ejecutar finalizacion |
| SAL-27 | Cambio de `Salida real` a fecha anterior a entrada | OK | Dispara recalculo con datos invalidos |
| SAL-28 | Manejo de error de calculo por fecha invalida | OK | Mensaje `La fecha de entrada debe ser anterior a la salida` |
| SAL-29 | Respuesta backend ante fecha invalida | OK | `POST /backend/pagos/total` devuelve `400` |
| SAL-30 | Estado de boton durante recalculo | OK | Boton muestra `Calculando pago...` y disabled temporal |
| SAL-31 | Regla de bloqueo de confirmacion con calculo invalido | FAIL | Aun con error de calculo, `Confirmar salida` vuelve a habilitarse |
| SAL-32 | Navegacion con boton `Volver` desde salida | OK | Retorna a `recepcion` |

## Evidencia de red observada

- `GET /backend/estancias/29` -> `200` (reqid `1515`, `1525`, `1528`)
- `POST /backend/pagos/total` (valido) -> `200` (reqid `1516`, `1526`, `1529`)
- `POST /backend/pagos/total` (invalido: salida < entrada) -> `400` (reqid `1519`)
- `GET /backend/recepcion/unidades` -> `200` (reqid `1534`, retorno por boton `Volver`)

Detalle request invalido (reqid `1519`):

- Request body: `{"tipoUnidad":"APARTAESTUDIO","numeroPersonas":1,"fechaEntrada":"2026-03-09T09:26","fechaSalida":"2026-03-08T17:53","idEstancia":29,"tipoCalculo":"ESTANDAR"}`
- Response body: `{"status":400,"error":"BAD_REQUEST","message":"La fecha de entrada debe ser anterior a la salida"}`

## Hallazgos

- Hallazgo funcional (prioridad media): con error de calculo activo por fecha invalida, el flujo vuelve a habilitar `Confirmar salida` y permite abrir modal de finalizacion (deberia bloquearse hasta corregir la fecha).
- Hallazgo de UX/consistencia: tras error de calculo, se mantiene el ultimo monto manual en resumen, lo que puede inducir a confirmar con datos inconsistentes.
- Hallazgo de accesibilidad DevTools: `No label associated with a form field` y `A form field element should have an id or name attribute` en la sesion.

## Resumen

- Casos ejecutados: **32**
- OK: **31**
- FAIL: **1**
- BLOQUEADOS: **0**

## Ronda adicional - pruebas agresivas (desktop)

Fecha: 2026-03-19

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| SAL-33 | Acceso directo con query `?estanciaId=40` sin contexto de navegacion | OK | Mensaje `No se encontro la estancia para finalizar.` |
| SAL-34 | Ingreso con contexto real desde `estancias` (`EST-77171D` + `Dar salida`) | OK | Precarga de `AE-301`, `APARTAESTUDIO`, entrada/salida y cliente |
| SAL-35 | Fecha de `Salida real` invalida inyectada por script (`2026-03-18 23:00`) | OK | Error visible: `La fecha de entrada debe ser anterior a la salida` |
| SAL-36 | Regla de bloqueo con error de calculo (revalidacion) | FAIL | `Confirmar salida` permanece habilitado y abre modal |
| SAL-37 | Cierre de modal de confirmacion con tecla `Escape` | FAIL | Modal permanece abierto; no cierra por teclado |
| SAL-38 | Cierre de modal con `Cancelar` en ronda adicional | OK | Modal se cierra correctamente |
| SAL-39 | Finalizacion real de estancia con datos validos y confirmacion positiva | OK | Boton `Si, finalizar estancia` ejecuta flujo y redirige a `estancias` |
| SAL-40 | Verificacion post-accion en tabla de `estancias` | OK | `EST-77171D` queda en estado `Finalizada`, salida real `19/03/2026 11:10 a. m.` y pago `$ 50.000` |
| SAL-41 | Contrato backend de finalizacion | OK | `PUT /backend/estancias/finalizar` -> `204` con payload de cierre y pago |

## Evidencia de red - ronda adicional

- `GET /backend/estancias/40` -> `200` (reqid `968`)
- `POST /backend/pagos/total` -> `200` (reqid `969`)
- `POST /backend/pagos/total` (fecha invalida) -> `400` (reqid `972`)
- `POST /backend/pagos/total` (fecha corregida) -> `200` (reqid `973`)
- `PUT /backend/estancias/finalizar` -> `204` (reqid `974`)
- `GET /backend/estancias/tabla?page=0&size=20&sort=entradaReal,desc` -> `200` (reqid `975`)

Detalle request finalizacion (reqid `974`):

- Request body: `{"idEstancia":40,"fechaSalidaReal":"2026-03-19T11:10","notasSalida":"Salida final validada en ronda agresiva","pagoEstancia":{"tipoPago":"ESTANCIA_COMPLETADA","monto":50000,"montoEstimado":0,"medioPago":"TRANSFERENCIA_BANCARIA","fecha":"2026-03-19T11:05","notas":"Pago final transferencia completado","estado":"COMPLETADO"}}`
- Response body: vacio (204)

## Hallazgos adicionales

- Hallazgo funcional (prioridad media): se confirma nuevamente que `Confirmar salida` queda habilitado aun cuando el calculo retorna error 400 por fechas invalidas.
- Hallazgo de accesibilidad/UX (prioridad media): el modal de confirmacion no cierra con `Escape`; solo responde al boton `Cancelar`.
- Hallazgo de estabilidad UI (prioridad baja): en intentos previos de interaccion con `Salida real`, el foco/click se desalineo y disparo navegacion no esperada a `calendario`.

## Resumen (ronda adicional)

- Casos ejecutados: **9**
- OK: **7**
- FAIL: **2**
- BLOQUEADOS: **0**

## Resumen acumulado

- Casos ejecutados: **41**
- OK: **38**
- FAIL: **3**
- BLOQUEADOS: **0**
