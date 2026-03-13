import { CanalReserva, EstadoReserva, ModoOcupacion, TipoUnidad } from './enums';

export interface ReservaTablaItem {
  id: number;
  codigoReserva: string;
  codigoEstancia?: string | null;
  codigoUnidad: string;
  tipoUnidad: TipoUnidad;
  nombreCliente?: string | null;
  idCliente?: number | null;
  tipoDocumentoCliente?: string | null;
  numeroDocumentoCliente?: string | null;
  numeroPersonas: number;
  canalReserva: CanalReserva;
  modoOcupacion: ModoOcupacion;
  estadoReserva: EstadoReserva;
  totalPagoReserva: number | null;
  cantidadPagosModificadosOEliminados: number;
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
  rangoGeneralDesde: string;
  rangoGeneralHasta: string;
  fechaCreacionDesde: string;
  fechaCreacionHasta: string;
  entradaDesde: string;
  entradaHasta: string;
  salidaDesde: string;
  salidaHasta: string;
}
