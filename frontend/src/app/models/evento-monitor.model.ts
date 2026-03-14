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
  | 'ELIMINACION_PAGO'
  | 'MODIFICACION_TARIFA_BASE'
  | 'MODIFICACION_TEMPORADA_ACTIVA';

export type EntidadMonitor = 'ESTANCIA' | 'RESERVA' | 'PAGO' | 'TARIFA_BASE' | 'AJUSTE_TEMPORADA';

export interface EventoMonitorItem {
  id: number;
  entidad: EntidadMonitor;
  idEntidad: number;
  codigoReserva?: string | null;
  codigoEstancia?: string | null;
  tipoEvento: TipoEventoMonitor;
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
