import { HttpErrorResponse } from '@angular/common/http';

type ErrorFields = Record<string, unknown>;

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null;
}

function extraerTexto(valor: unknown): string {
  return typeof valor === 'string' ? valor.trim() : '';
}

function parsearJson(valor: string): unknown {
  try {
    return JSON.parse(valor);
  } catch {
    return valor;
  }
}

function extraerPrimerErrorCampos(fields: unknown): string {
  if (!esObjeto(fields)) {
    return '';
  }

  const primerMensaje = Object.values(fields)
    .map((valor) => extraerTexto(valor))
    .find((mensaje) => Boolean(mensaje));

  return primerMensaje ?? '';
}

export function extractBackendErrorMessage(errorResponse: unknown, fallback: string): string {
  if (!(errorResponse instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (errorResponse.status >= 500) {
    return fallback;
  }

  const rawPayload = errorResponse.error;
  const payload =
    typeof rawPayload === 'string' && rawPayload.trim() ? parsearJson(rawPayload) : rawPayload;

  if (typeof payload === 'string') {
    const texto = payload.trim();
    if (texto) {
      return texto;
    }
  }

  if (esObjeto(payload)) {
    const fieldMessage = extraerPrimerErrorCampos(payload['fields'] as ErrorFields);
    if (fieldMessage) {
      return fieldMessage;
    }

    const message = extraerTexto(payload['message']);
    if (message) {
      return message;
    }

    const error = extraerTexto(payload['error']);
    if (error) {
      return error;
    }
  }

  const message = errorResponse.message?.trim();
  return message || fallback;
}
