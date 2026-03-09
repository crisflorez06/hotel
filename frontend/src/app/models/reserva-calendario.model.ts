import { EstadoReserva, TipoUnidad } from './enums';

export interface ReservaCalendarioDTO {
  id: number;
  idCliente: number | null;
  idEstancia?: number | null;
  codigoReserva?: string | null;
  inicio: string;
  fin: string;
  estadoReserva: EstadoReserva;
  codigoUnidad: string | null;
  tipoUnidad: TipoUnidad | null;
  numeroPersonas: number | null;
  nombreCliente: string | null;
  totalAnticipo?: number | null;
}
