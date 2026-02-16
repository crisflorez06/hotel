export type TipoEventoMonitor =
  | 'CREACION_RESERVA'
  | 'MODIFICACION_RESERVA'
  | 'ELIMINACION_RESERVA'
  | 'CREACION_ESTANCIA'
  | 'ACTIVACION_ESTANCIA'
  | 'MODIFICACION_ESTANCIA'
  | 'ELIMINACION_ESTANCIA'
  | 'FINALIZACION_ESTANCIA'
  | 'CREACION_PAGO'
  | 'MODIFICACION_PAGO'
  | 'ELIMINACION_PAGO';

export type EntidadMonitor = 'ESTANCIA' | 'RESERVA' | 'PAGO';

export interface EventoMonitorItem {
  id: number;
  entidad: string;
  idEntidad: number;
  codigoReserva?: string | null;
  codigoEstancia?: string | null;
  tipoEvento: string;
  detalle: string;
  fecha: string;
}

export interface EventoMonitorFiltros {
  tiposEvento: TipoEventoMonitor[];
  entidades: EntidadMonitor[];
  codigoReserva?: string;
  codigoEstancia?: string;
  fechaDesde?: string;
  fechaHasta?: string;
}
