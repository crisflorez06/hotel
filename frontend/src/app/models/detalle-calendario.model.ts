import { EstadoEstancia, EstadoReserva, TipoUnidad } from './enums';
import { HabitacionDTO } from './habitacion.model';
import { ReservaDTO } from './reserva.model';
import { EstanciaDTO } from './estancia-detalle.model';
import { UnidadDTO } from './unidad.model';

export interface DetalleCalendarioHabitacionDTO {
  habitacion: HabitacionDTO;
  estancias: EstanciaDTO[];
  reservas: ReservaDTO[];
}

export interface DetalleCalendarioUnidadDTO {
  unidad: UnidadDTO;
  estancias: EstanciaDTO[];
  reservas: ReservaDTO[];
  habitaciones: DetalleCalendarioHabitacionDTO[];
}

export interface EstanciaCalendarioDTO {
  id: number;
  inicio: string;
  fin: string;
  codigoEstancia: string | null;
  codigoUnidad: string | null;
  tipoUnidad: TipoUnidad | null;
  numeroPersonas: number | null;
  nombreCliente: string | null;
  idCliente: number | null;
  totalPagado: number | null;
  estadoEstancia: EstadoEstancia;
}

export interface ReservaCalendarioResumenDTO {
  id: number;
  idEstancia?: number | null;
  inicio: string;
  fin: string;
  codigoReserva: string | null;
  codigoUnidad: string | null;
  tipoUnidad: TipoUnidad | null;
  numeroPersonas: number | null;
  nombreCliente: string | null;
  idCliente: number | null;
  totalAnticipo: number | null;
  estadoReserva: EstadoReserva;
}

export interface DetalleCalendarioDTO {
  estancias: EstanciaCalendarioDTO[];
  reservas: ReservaCalendarioResumenDTO[];
}
