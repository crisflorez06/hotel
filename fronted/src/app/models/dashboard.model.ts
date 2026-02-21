export type DashboardGranularidad = 'DAY' | 'WEEK' | 'MONTH';

export interface DashboardResumenOperativoDTO {
  estanciasActivas: number;
  reservasConfirmadas: number;
  habitacionesOcupadas: number;
  habitacionesTotales: number;
  ocupacionPorcentaje: number;
}

export interface DashboardResumenFinancieroDTO {
  ingresos: number;
  gastos: number;
  neto: number;
  pagosPendientesCantidad: number;
  pagosPendientesMonto: number;
}

export interface DashboardResumenAlertasDTO {
  reservasExpiradas: number;
  estanciasExcedidas: number;
  pagosPendientesAntiguos: number;
  totalAlertas: number;
}

export interface DashboardResumenDTO {
  operativo: DashboardResumenOperativoDTO;
  financiero: DashboardResumenFinancieroDTO;
  alertas: DashboardResumenAlertasDTO;
}

export interface DashboardConteoUnidadDTO {
  tipoUnidad: string;
  disponible: number;
  ocupado: number;
  parcialmente: number;
  total: number;
}

export interface DashboardSerieFinancieraDTO {
  periodo: string;
  ingresos: number;
  gastos: number;
  neto: number;
}

export interface CategoriaMontoDTO {
  categoria: string;
  monto: number;
}

export interface DashboardDistribucionFinancieraDTO {
  ingresosPorTipoPago: CategoriaMontoDTO[];
  ingresosPorMedioPago: CategoriaMontoDTO[];
  gastosPorConcepto: CategoriaMontoDTO[];
}

export interface DashboardAlertaItemDTO {
  tipo: string;
  severidad: string;
  codigoReserva?: string | null;
  codigoEstancia?: string | null;
  fechaReferencia: string;
  mensaje: string;
}

export interface DashboardAlertasResponseDTO {
  resumen: DashboardResumenAlertasDTO;
  items: DashboardAlertaItemDTO[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}
