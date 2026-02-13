import { EstadoEstancia, ModoOcupacion, TipoUnidad } from './enums';

export interface EstanciaTablaItem {
  id: number;
  codigoEstancia: string;
  codigoReservaAsociada?: string | null;
  totalPagoEstancia?: number | null;
  codigoUnidad: string;
  tipoUnidad: TipoUnidad;
  nombreCliente: string;
  idCliente: number;
  numeroDocumentoCliente: string;
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
  entradaDesde: string;
  entradaHasta: string;
  salidaEstimadaDesde: string;
  salidaEstimadaHasta: string;
  salidaRealDesde: string;
  salidaRealHasta: string;
  tieneReservaAsociada: '' | 'SI' | 'NO';
}
