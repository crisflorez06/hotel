import { CanalReserva, EstadoReserva, ModoOcupacion, TipoUnidad } from './enums';

export interface ReservaTablaItem {
  id: number;
  codigoReserva: string;
  codigoUnidad: string;
  tipoUnidad: TipoUnidad;
  nombreCliente: string;
  idCliente: number;
  numeroDocumentoCliente: string;
  numeroPersonas: number;
  canalReserva: CanalReserva;
  modoOcupacion: ModoOcupacion;
  estadoReserva: EstadoReserva;
  fechaCreacion: string;
  entradaEstimada: string;
  salidaEstimada: string;
  tieneEstanciaAsociada: boolean;
}

export interface ReservaTablaFiltros {
  estados: EstadoReserva[];
  canales: CanalReserva[];
  modoOcupacion: ModoOcupacion | '';
  tipoUnidad: TipoUnidad | '';
  codigoReserva: string;
  codigoUnidad: string;
  nombreCliente: string;
  numeroDocumentoCliente: string;
  fechaCreacionDesde: string;
  fechaCreacionHasta: string;
  entradaDesde: string;
  entradaHasta: string;
  salidaDesde: string;
  salidaHasta: string;
}
