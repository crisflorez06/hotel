import { EstadoPago, MedioPago, TipoPago } from './enums';

export interface PagoDTO {
  id: number;
  codigoReserva?: string | null;
  codigoEstancia?: string | null;
  monto: number;
  medioPago: MedioPago;
  fecha: string;
  estado: EstadoPago;
  tipoPago?: TipoPago;
}
