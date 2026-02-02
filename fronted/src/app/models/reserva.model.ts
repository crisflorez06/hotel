import { CanalReserva, TipoUnidad } from './enums';
import { PagoNuevoRequest } from './pago.model';

export interface ReservaNuevoRequest {
  tipoUnidad: TipoUnidad;
  codigo: string;
  idOcupante: number;
  numeroPersonas: number;
  entradaEstimada: string;
  salidaEstimada: string;
  canalReserva: CanalReserva;
  notas?: string;
  pago?: PagoNuevoRequest | null;
}
