import { EstadoEstancia, ModoOcupacion, TipoUnidad } from './enums';

export interface EstanciaTablaItem {
  id: number;
  codigoEstancia: string;
  codigoReservaAsociada?: string | null;
  totalPagoEstancia?: number | null;
  cantidadPagosModificadosOEliminados?: number | null;
  codigoUnidad: string;
  tipoUnidad: TipoUnidad;
  nombreCliente: string;
  idCliente: number;
  tipoDocumentoCliente?: string | null;
  numeroDocumentoCliente: string;
  totalPersonas?: number | null;
  estadoEstancia: EstadoEstancia;
  modoOcupacion: ModoOcupacion;
  entradaReal: string;
  salidaEstimada: string;
  salidaReal: string | null;
  tieneReservaAsociada: boolean;
  idReservaAsociada: number | null;
}

export interface EstanciaTablaFiltros {
  estados: EstadoEstancia[];
  modoOcupacion: ModoOcupacion | '';
  tipoUnidad: TipoUnidad | '';
  codigoEstancia: string;
  codigoUnidad: string;
  nombreCliente: string;
  numeroDocumentoCliente: string;
  rangoGeneralDesde: string;
  rangoGeneralHasta: string;
  entradaDesde: string;
  entradaHasta: string;
  salidaEstimadaDesde: string;
  salidaEstimadaHasta: string;
  salidaRealDesde: string;
  salidaRealHasta: string;
  tieneReservaAsociada: '' | 'SI' | 'NO';
}
