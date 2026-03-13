import { Params } from '@angular/router';

import { EstanciaDTO } from '../../models/estancia-detalle.model';
import { OcupanteDTO } from '../../models/ocupante.model';

function normalizarTexto(valor: string | null | undefined): string {
  return (valor ?? '').trim();
}

export function construirQueryReservasPorCodigo(
  codigoReserva: string | null | undefined,
): Params | null {
  const codigo = normalizarTexto(codigoReserva);
  if (!codigo) {
    return null;
  }

  return { codigoReserva: codigo };
}

export function construirQueryEstanciasPorCodigo(
  codigoEstancia: string | null | undefined,
): Params | null {
  const codigo = normalizarTexto(codigoEstancia);
  if (!codigo) {
    return null;
  }

  return { codigoEstancia: codigo };
}

export function construirQueryClientesPorOcupante(
  cliente: OcupanteDTO | null | undefined,
): Params | null {
  if (!cliente) {
    return null;
  }

  const numeroDocumento = normalizarTexto(cliente.numeroDocumento);
  const tipoDocumento = normalizarTexto(cliente.tipoDocumento);
  if (numeroDocumento) {
    return tipoDocumento
      ? { numeroDocumento, tipoDocumento }
      : { numeroDocumento };
  }

  const nombre = normalizarTexto(cliente.nombres);
  const apellido = normalizarTexto(cliente.apellidos);
  const query: Params = {};
  if (nombre) {
    query['nombre'] = nombre;
  }
  if (apellido) {
    query['apellido'] = apellido;
  }

  return Object.keys(query).length ? query : null;
}

export function construirQueryClientesPorEstancia(
  estancia: EstanciaDTO | null | undefined,
): Params | null {
  if (!estancia) {
    return null;
  }

  const personas: OcupanteDTO[] = [
    ...(estancia.cliente ? [estancia.cliente] : []),
    ...(estancia.acompanantes ?? []),
  ];

  const documentos = Array.from(
    new Set(
      personas
        .map((persona) => normalizarTexto(persona.numeroDocumento))
        .filter((documento) => Boolean(documento)),
    ),
  );

  if (!documentos.length) {
    return null;
  }

  return {
    documentos: documentos.join(','),
  };
}
