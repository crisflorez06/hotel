import { TipoUnidad } from './enums';
import { PagoNuevoRequest } from './pago.model';

export interface EstanciaNuevoRequest {
  tipoUnidad: TipoUnidad;
  codigo: string;
  idCliente: number;
  entradaReal: string;
  salidaEstimada: string;
  idAcompanantes?: number[];
  notas?: string;
  pago?: PagoNuevoRequest | null;
}

export interface EstanciaEditarRequest {
  idCliente?: number;
  entradaReal?: string;
  salidaEstimada?: string;
  idAcompanantes?: number[];
  notas?: string;
}
