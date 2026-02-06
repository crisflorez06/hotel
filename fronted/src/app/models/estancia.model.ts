import { TipoUnidad } from './enums';
import { PagoNuevoRequest } from './pago.model';

export interface EstanciaNuevoRequest {
  tipoUnidad: TipoUnidad;
  codigo: string;
  idReserva?: number | null;
  idCliente: number;
  entradaReal: string;
  salidaEstimada: string;
  idAcompanantes?: number[];
  notas?: string;
  pago?: PagoNuevoRequest | null;
}

export interface EstanciaActivarRequest {
  idReserva: number;
  idCliente: number;
  entradaReal: string;
  salidaEstimada: string;
  idAcompanantes?: number[];
  pago?: PagoNuevoRequest | null;
}

export interface EstanciaEditarRequest {
  tipoUnidad: TipoUnidad;
  codigo: string;
  idCliente?: number;
  entradaReal?: string;
  salidaEstimada?: string;
  idAcompanantes?: number[];
  notas?: string;
}

export interface SalidaEstanciaRequest {
  idEstancia: number;
  fechaSalidaReal: string;
  notasSalida?: string;
  pagoEstancia?: PagoNuevoRequest | null;
}
