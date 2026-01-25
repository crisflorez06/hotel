import { EstadoPago, MedioPago } from './enums';

export interface PagoDTO {
  id: number;
  montoPagoReserva: number | null;
  montoPagoEstancia: number | null;
  medioPago: MedioPago;
  fecha: string;
  estado: EstadoPago;
}
