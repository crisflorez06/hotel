import { EstadoEstancia, EstadoReserva, TipoUnidad } from './enums';

export interface EstanciaCalendarioDTO {
  id: number;
  inicio: string;
  fin: string;
  codigoEstancia: string | null;
  codigoUnidad: string | null;
  tipoUnidad: TipoUnidad | null;
  numeroPersonas: number | null;
  nombreCliente: string | null;
  idCliente: number | null;
  totalPagado: number | null;
  estadoEstancia: EstadoEstancia;
}

export interface ReservaCalendarioResumenDTO {
  id: number;
  inicio: string;
  fin: string;
  codigoReserva: string | null;
  codigoUnidad: string | null;
  tipoUnidad: TipoUnidad | null;
  numeroPersonas: number | null;
  nombreCliente: string | null;
  idCliente: number | null;
  totalAnticipo: number | null;
  estadoReserva: EstadoReserva;
}

export interface DetalleCalendarioDTO {
  estancias: EstanciaCalendarioDTO[];
  reservas: ReservaCalendarioResumenDTO[];
}
