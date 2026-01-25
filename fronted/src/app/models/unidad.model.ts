import { EstadoOperativo, Piso, TipoUnidad } from './enums';
import { HabitacionDTO } from './habitacion.model';

export interface UnidadDTO {
  id: number;
  codigo: string;
  tipo: TipoUnidad;
  piso: Piso;
  estado: EstadoOperativo;
  habitaciones: HabitacionDTO[];
}
