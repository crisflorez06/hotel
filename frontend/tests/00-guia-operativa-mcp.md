# Guia operativa de pruebas con Chrome MCP

Fecha de creacion: 2026-03-19

## Objetivo

Mantener un recordatorio estable del metodo de trabajo acordado para evitar perdida de contexto durante la ejecucion de pruebas E2E en las vistas del sistema.

## Acuerdos de trabajo

- El usuario indicara en cada momento que vista se debe trabajar.
- No se debe avanzar a una nueva vista sin una indicacion explicita del usuario, aunque la vista anterior ya haya terminado.
- Antes de definir nuevas pruebas, se debe leer el archivo de pruebas existente de la vista correspondiente en `frontend/tests/`.
- Se debe analizar la vista con Chrome MCP antes de establecer la bateria de pruebas a ejecutar.
- Se deben ejecutar pruebas agresivas que cubran todas las acciones disponibles y todas las formas/estados en que puede mostrarse la vista.
- Se puede ejecutar cualquier accion CRUD necesaria para cubrir completamente la vista y sus flujos.
- No se deben ejecutar pruebas en mobile por ahora; la cobertura es solo desktop.
- Para preparar escenarios de prueba, primero se intentara por backend (`http://localhost:8080`).
- Si el backend no permite preparar el escenario requerido, se usara base de datos como ultimo recurso.
- Al finalizar una vista, se debe editar unicamente el documento de pruebas relacionado para agregar los nuevos casos y evidencias.
- No se deben modificar ni borrar los casos que ya estaban documentados previamente.

## Formato operativo por vista

1. Recibir vista objetivo indicada por el usuario.
2. Leer el `.md` de pruebas de esa vista y detectar cobertura existente y faltantes.
3. Explorar la vista con Chrome MCP para mapear acciones, estados, validaciones y navegacion.
4. Definir y ejecutar pruebas agresivas (incluyendo CRUD cuando aplique).
5. Registrar evidencias tecnicas (requests, responses, consola, comportamiento UI).
6. Actualizar el `.md` de la vista agregando nuevos casos, resultados y hallazgos.
7. Detenerse y esperar la siguiente vista indicada por el usuario antes de continuar.

## Regla de comunicacion

- Todas las respuestas al usuario deben ser en espanol.
