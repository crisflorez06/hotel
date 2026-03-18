import { EstadoPago, MedioPago, TipoPago } from './enums';

export interface PagoNuevoRequest {
  tipoPago: TipoPago;
  monto: number;
  montoEstimado?: number;
  medioPago: MedioPago;
  fecha: string;
  notas?: string;
  estado: EstadoPago;
}
