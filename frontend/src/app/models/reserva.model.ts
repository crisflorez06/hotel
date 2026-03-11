import { CanalReserva, TipoUnidad } from './enums';
import { OcupanteDTO } from './ocupante.model';

export interface ReservaNuevoRequest {
  tipoUnidad: TipoUnidad;
  codigo: string;
  idOcupante: number;
  numeroPersonas: number;
  entradaEstimada: string;
  salidaEstimada: string;
  canalReserva: CanalReserva;
  notas?: string;
}

export interface ReservaDTO {
  id: number;
  codigoReserva: string;
  idEstancia?: number | null;
  codigoEstancia?: string | null;
  cliente?: OcupanteDTO | null;
  fechaCreacion: string;
  entradaEstimada: string;
  salidaEstimada: string;
  numeroPersonas: number;
  canalReserva: CanalReserva;
  notas?: string | null;
  modoOcupacion?: string;
  estadoReserva?: string;
  estadoEstancia?: string | null;
}
