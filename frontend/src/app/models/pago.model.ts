import { EstadoPago, MedioPago, TipoPago } from './enums';

export interface PagoNuevoRequest {
  tipoPago: TipoPago;
  monto: number;
  medioPago: MedioPago;
  fecha: string;
  estado: EstadoPago;
}
