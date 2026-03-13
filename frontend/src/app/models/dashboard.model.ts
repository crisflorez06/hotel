export interface DashboardResumenOperativoDTO {
  estanciasActivas: number;
  reservasConfirmadas: number;
  reservasPorCanal: Record<string, number>;
  apartamentosOcupados: number;
  apartamentosReservados: number;
  apartaestudioOcupados: number;
  apartaestudioReservados: number;
  apartamentosTotales: number;
  apartaestudioTotales: number;
  habitacionesOcupadas: number;
  habitacionesReservadas: number;
  habitacionesTotales: number;
  ocupacionPorcentaje: number;
  reservadasPorcentaje: number;
  estanciasUltimos12Meses: EstanciaMensualDTO[];
}

export interface EstanciaMensualDTO {
  periodo: string;
  cantidad: number;
}

export interface DashboardResumenAlertasDTO {
  reservasExpiradas: number;
  estanciasExcedidas: number;
  totalAlertas: number;
}

export interface DashboardResumenDTO {
  operativo: DashboardResumenOperativoDTO;
  alertas: DashboardResumenAlertasDTO;
}

export interface CategoriaMontoDTO {
  categoria: string;
  monto: number;
}

export interface CategoriaMontoConteoDTO {
  categoria: string;
  monto: number;
  conteoPagos: number;
}

export interface DashboardDistribucionFinancieraDTO {
  ingresosTotales: number;
  ingresosPagos: number;
  gastos: number;
  neto: number;
  ingresosPendientesCantidad: number;
  ingresosPendientesMonto: number;
  ingresosPorTipoPago: CategoriaMontoDTO[];
  ingresosPorMedioPago: CategoriaMontoConteoDTO[];
}
