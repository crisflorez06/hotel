import { EstadoReserva, TipoUnidad } from './enums';

export interface ReservaCalendarioDTO {
  id: number;
  idCliente: number | null;
  idPagoReserva?: number | null;
  inicio: string;
  fin: string;
  estado: EstadoReserva;
  codigoUnidad: string | null;
  tipoUnidad: TipoUnidad | null;
  numeroPersonas: number | null;
  nombreCliente: string | null;
}
