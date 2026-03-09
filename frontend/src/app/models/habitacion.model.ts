import { EstadoOperativo, Piso } from './enums';

export interface HabitacionDTO {
  id: number;
  codigo: string;
  estado: EstadoOperativo;
  piso: Piso;
  codigoUnidad: string;
  informacionAdicional?: string | null;
}
