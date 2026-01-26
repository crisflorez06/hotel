import { EstadoReserva, TipoUnidad } from './enums';

export interface ReservaCalendarioDTO {
  id: number;
  inicio: string;
  fin: string;
  estado: EstadoReserva;
  codigoUnidad: string | null;
  tipoUnidad: TipoUnidad | null;
  numeroPersonas: number | null;
}
