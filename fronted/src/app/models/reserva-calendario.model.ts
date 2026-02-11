import { EstadoReserva, TipoUnidad } from './enums';
import { PagoDTO } from './pago-detalle.model';

export interface ReservaCalendarioDTO {
  id: number;
  idCliente: number | null;
  idPagoReserva?: number | null;
  codigoReserva?: string | null;
  inicio: string;
  fin: string;
  estado: EstadoReserva;
  codigoUnidad: string | null;
  tipoUnidad: TipoUnidad | null;
  numeroPersonas: number | null;
  nombreCliente: string | null;
  totalAnticipo?: number | null;
  pagosReserva?: PagoDTO[];
}
