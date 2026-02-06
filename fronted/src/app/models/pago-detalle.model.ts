import { EstadoPago, MedioPago, TipoPago } from './enums';

export interface PagoDTO {
  id: number;
  monto: number;
  medioPago: MedioPago;
  fecha: string;
  estado: EstadoPago;
  tipoPago?: TipoPago;
}
