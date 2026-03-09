# Evidencia de pruebas: edicion de estancias

Fecha: 2026-03-04

## Enfoque aplicado

- UI real con Chrome MCP para validar flujo frontend (pantalla `estancias/nueva` en modo edicion).
- Preparacion de escenarios con backend en `http://localhost:8080` para acelerar casos complejos.
- Verificacion de respuestas HTTP, mensajes de error y estado final de unidades/estancias.

## Preparacion de datos

Se crearon ocupantes para pruebas de edicion:

- Cliente nuevo: `id=5`, documento `900100001` (`Carlos Editar`).
- Acompanantes: `id=6` (`Ana Acomp`) y `id=7` (`Luis Acomp`).

## Pruebas ejecutadas y resultado

### 1) Exito editando estancia apartamento

- Flujo UI: `APT-101` en modo edicion (`estanciaId=3`).
- Cambios aplicados: cliente nuevo + 2 acompanantes + nota + ajuste de entrada.
- Evidencia red: `PUT /backend/estancias/3` -> `204` (reqid `332`).
- Verificacion backend: estancia activa actualizada con `idCliente=5`, acompanantes `[6,7]`, `totalPersonas=3`.
- Resultado: **OK**

### 2) Fallo editando por reserva

- Flujo UI: editar estancia `id=4` (habitacion) intentando mover a `APT-301-H1`.
- Evidencia red: `PUT /backend/estancias/4` -> `409` (reqid `383`).
- Mensaje UI/backend:
  `No se puede editar la estancia: existe una reserva para las habitaciones con codigo: APT-301-H1, APT-301-H2, APT-301-H3`.
- Resultado: **OK**

### 3) Exito cambio apartamento -> apartaestudio

- API (escenario controlado): `PUT /estancias/3` con `tipoUnidad=APARTAESTUDIO`, `codigo=AE-302`.
- HTTP: `204`.
- Verificacion:
  - `APT-101` queda `DISPONIBLE`.
  - `AE-302` queda `OCUPADO`.
- Resultado: **OK**

### 4) Exito cambio apartaestudio -> habitacion

- API: `PUT /estancias/4` con `tipoUnidad=HABITACION`, `codigo=APT-201-H2`.
- HTTP: `204`.
- Verificacion: `APT-201` queda `PARCIALMENTE` con `H2` ocupado.
- Resultado: **OK**

### 5) Exito cambio habitacion -> apartamento

- API: `PUT /estancias/5` con `tipoUnidad=APARTAMENTO`, `codigo=APT-101`.
- HTTP: `204`.
- Verificacion: `APT-101` queda `OCUPADO` (ocupacion completa).
- Resultado: **OK**

### 6) Fallo editando por estancia existente

- API: intento de mover `id=4` a `APT-101` ya ocupado.
- HTTP: `409`.
- Mensaje:
  `No se puede editar la estancia: existe una estancia para las habitaciones con codigo: APT-101-H1, APT-101-H2, APT-101-H3`.
- Resultado: **OK**

### 7) Fallo fecha de salida anterior a entrada

- API: `entradaReal=2026-03-06T12:00`, `salidaEstimada=2026-03-02T12:00`.
- HTTP: `400`.
- Mensaje:
  `La fecha de salida debe ser posterior a la fecha de entrada`.
- Resultado: **OK**

### 8) Fallo fecha de entrada superior a ahora

- API: `entradaReal` futura (`2026-03-10T12:00`).
- HTTP: `400`.
- Mensaje:
  `La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura`.
- Resultado: **OK**

### 9) Exito editando a estado excedida

- API: edicion de `id=7` con entrada/salida historicas (`2026-03-01` a `2026-03-02`).
- HTTP: `204`.
- Verificacion en tabla: estado de `EST-42902E` en `EXCEDIDA`.
- Resultado: **OK**

### 10) Exito editando desde excedida a activa

- API: edicion previa de `id=7` a fechas recientes (`2026-03-03` a `2026-03-06`).
- HTTP: `204`.
- Resultado: **OK**

### 11) Exito editando sin acompanantes

- API: edicion de `id=7` sin `idAcompanantes`.
- HTTP: `204`.
- Verificacion en tabla: `totalPersonas=1` en la estancia editada.
- Resultado: **OK**

### 12) Exito editando sin cambios (sin nuevo evento)

- API: `PUT /estancias/4` reenviando exactamente los mismos datos actuales.
- HTTP: `204`.
- Verificacion de auditoria:
  - Antes: `GET /eventos?entidades=ESTANCIA&idEntidad=4` -> `totalElements=2`.
  - Despues: mismo query -> `totalElements=2`.
- Conclusión: no se crea evento adicional cuando no hay cambios efectivos.
- Resultado: **OK**

### 13) Auditoria y pagos por cambio de unidad

- Estancia `EST-92997F` (id 3):
  - `GET /detalle/estancia/3` muestra pago `tipoPago=CAMBIO_UNIDAD`, `estado=PENDIENTE`, `monto=670000.00`.
  - `GET /eventos?entidades=PAGO&codigoEstancia=EST-92997F` registra evento `CREACION_PAGO` asociado.
- Estancia `EST-56362K` (id 4):
  - `GET /detalle/estancia/4` muestra pago `tipoPago=CAMBIO_UNIDAD`, `estado=PENDIENTE`, `monto=230000.00`.
  - `GET /eventos?entidades=PAGO&codigoEstancia=EST-56362K` registra evento `CREACION_PAGO` asociado.
- Resultado: **OK**

## Estado final relevante (post pruebas)

- Unidades:
  - `APT-101`: `OCUPADO`
  - `APT-201`: `PARCIALMENTE`
  - `APT-301`: `DISPONIBLE`
  - `AE-201`: `OCUPADO`
  - `AE-202`: `DISPONIBLE`
  - `AE-301`: `OCUPADO`
  - `AE-302`: `OCUPADO`

- Estancias activas editadas visibles en tabla:
  - `EST-92997F` -> `AE-302` (cliente nuevo, 3 personas)
  - `EST-56362K` -> `APT-201-H2` (individual, 2 personas)
  - `EST-91651J` -> `APT-101` (completo, 3 personas)

## Cobertura frente a la bateria compartida

Se validaron de extremo a extremo los bloques mas criticos de la bateria:

- Edicion exitosa por tipo de unidad.
- Cambios de unidad (incluyendo cambios de tipo).
- Errores de negocio por conflicto con reserva y con estancia existente.
- Reglas de fechas (salida < entrada, entrada futura).
- Transicion de estado activa/excedida.
- Edicion sin acompanantes.
- Caso sin cambios sin generacion de nuevo evento.
- Auditoria de pago `CAMBIO_UNIDAD` creada y visible por estancia.

No quedan pendientes de la ronda acordada.
