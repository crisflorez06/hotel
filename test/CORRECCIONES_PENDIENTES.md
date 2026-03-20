# Correcciones pendientes segun documentacion de pruebas

Fecha de consolidacion: 2026-03-19

## Lista completa de correcciones

1. Corregir `GET /reservas/tabla` para que fechas invalidas en query (`LocalDateTime`) respondan `400` y no `500`.
2. Corregir `GET /reservas/buscar-por-documento` para que cuando falte `numero` responda `400` (no `500`).
3. Corregir `GET /estancias/tabla` para que fechas invalidas en query respondan `400`.
4. Corregir `GET /pagos` para que fechas invalidas en query respondan `400`.
5. Corregir `POST /pagos/total` para que faltantes/formatos invalidos (fecha, enum, campos obligatorios) respondan `400` de validacion.
6. Corregir `GET /estancias/activa` para que `tipoUnidad` faltante o invalido responda `400`.
7. Corregir `POST /pagos` para que `idEstancia` faltante en query param responda `400`.
8. Corregir `PUT /pagos/eliminar/{id}` para que path variable no numerica (`abc`) responda `400`.
9. Corregir `PUT /estancias/eliminar/{id}` para que path variable no numerica (`abc`) responda `400`.
10. Agregar `@Valid` (y validaciones de DTO necesarias) en `PUT /estancias/{id}` para evitar `500` con payload incompleto.
11. Agregar/asegurar `@Valid` en `POST /pagos/total` (controlador + DTO) para uniformar errores de entrada.
12. Mapear explicitamente en `ApiExceptionHandler` excepciones de entrada (`MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`, parseo de fecha, etc.) a `400`.
13. Unificar el formato de error para validacion (`VALIDATION_ERROR` + detalle por campo) en todos los endpoints.
14. Revisar y ajustar la regla de negocio de pagos donde `ANTICIPO_ESTANCIA` se persiste como `COMPLETADO` aunque llegue otro estado (definir si se conserva estado enviado o se documenta como regla estricta).
15. Estandarizar manejo de fechas frontend-backend (zona horaria/formato): evitar falsos errores por `toISOString` vs fecha local esperada.
16. Revisar la logica de estimacion por dias en pagos (mismo dia retorna `0`) y confirmar si ese comportamiento es el esperado de negocio.
17. Fortalecer pruebas automaticas de regresion para todos los casos anteriores (`400` vs `500`, validaciones y parseo).
18. Mejorar aislamiento de datos en suites (setup/cleanup consistente) para evitar fallos por ambiente contaminado.

## Nota

- Este archivo consolida errores y recomendaciones reportados en los documentos de `test/pago`, `test/estancia` y `test/reserva`.
