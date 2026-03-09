import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import 'chart.js/auto';
import { BaseChartDirective } from 'ng2-charts';
import { forkJoin } from 'rxjs';

import {
  CategoriaMontoDTO,
  DashboardAlertasResponseDTO,
  DashboardConteoUnidadDTO,
  DashboardDistribucionFinancieraDTO,
  DashboardGranularidad,
  DashboardResumenDTO,
  DashboardSerieFinancieraDTO,
} from '../../models/dashboard.model';
import {
  EntidadMonitor,
  EventoMonitorFiltros,
  EventoMonitorItem,
  TipoEventoMonitor,
} from '../../models/evento-monitor.model';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import { DetalleService } from '../../services/detalle.service';
import { EventoService } from '../../services/evento.service';

interface DetalleCampoVisual {
  etiqueta: string;
  valor?: string;
  anterior?: string;
  nuevo?: string;
}

interface DashboardFiltrosVisual {
  desde: string;
  hasta: string;
  granularidad: DashboardGranularidad;
  diasAlertas: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, BaseChartDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  readonly tiposEventoDisponibles: TipoEventoMonitor[] = [
    'CREACION_RESERVA',
    'MODIFICACION_RESERVA',
    'ELIMINACION_RESERVA',
    'CREACION_ESTANCIA',
    'ACTIVACION_ESTANCIA',
    'MODIFICACION_ESTANCIA',
    'ELIMINACION_ESTANCIA',
    'FINALIZACION_ESTANCIA',
    'CREACION_PAGO',
    'MODIFICACION_PAGO',
    'ELIMINACION_PAGO',
  ];
  readonly tiposEventoEstancia: TipoEventoMonitor[] = [
    'CREACION_ESTANCIA',
    'ACTIVACION_ESTANCIA',
    'MODIFICACION_ESTANCIA',
    'ELIMINACION_ESTANCIA',
    'FINALIZACION_ESTANCIA',
  ];
  readonly tiposEventoReserva: TipoEventoMonitor[] = [
    'CREACION_RESERVA',
    'MODIFICACION_RESERVA',
    'ELIMINACION_RESERVA',
  ];
  readonly tiposEventoPago: TipoEventoMonitor[] = [
    'CREACION_PAGO',
    'MODIFICACION_PAGO',
    'ELIMINACION_PAGO',
  ];

  readonly entidadesDisponibles: EntidadMonitor[] = ['ESTANCIA', 'RESERVA', 'PAGO'];
  readonly tamanoPagina = 20;
  readonly granularidadesDisponibles: DashboardGranularidad[] = ['DAY', 'WEEK', 'MONTH'];
  readonly tamanoAlertas = 6;

  dashboardFiltros: DashboardFiltrosVisual = this.crearFiltrosDashboardIniciales();
  dashboardCargando = false;
  dashboardError = '';
  dashboardResumen: DashboardResumenDTO | null = null;
  dashboardConteosUnidad: DashboardConteoUnidadDTO[] = [];
  dashboardSerieFinanciera: DashboardSerieFinancieraDTO[] = [];
  dashboardDistribucion: DashboardDistribucionFinancieraDTO | null = null;
  dashboardAlertas: DashboardAlertasResponseDTO | null = null;

  readonly unidadChartType: 'bar' = 'bar';
  readonly serieChartType: 'line' = 'line';
  readonly distribucionChartType: 'doughnut' = 'doughnut';

  unidadChartData: ChartConfiguration<'bar'>['data'] = { labels: [], datasets: [] };
  serieChartData: ChartConfiguration<'line'>['data'] = { labels: [], datasets: [] };
  distribucionTipoChartData: ChartConfiguration<'doughnut'>['data'] = { labels: [], datasets: [] };
  distribucionMedioChartData: ChartConfiguration<'doughnut'>['data'] = { labels: [], datasets: [] };
  distribucionGastoChartData: ChartConfiguration<'doughnut'>['data'] = { labels: [], datasets: [] };

  unidadChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: { stacked: true, grid: { display: false } },
      y: { stacked: true, beginAtZero: true, ticks: { precision: 0 } },
    },
    plugins: {
      legend: { position: 'bottom' },
    },
  };

  serieChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    scales: {
      y: { beginAtZero: true, ticks: { precision: 0 } },
    },
    plugins: {
      legend: { position: 'bottom' },
    },
  };

  distribucionChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '58%',
    plugins: {
      legend: { position: 'bottom' },
    },
  };

  filtros: EventoMonitorFiltros = this.crearFiltrosVacios();

  eventos: EventoMonitorItem[] = [];
  cargando = false;
  error = '';

  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  modalAbierto = false;
  eventoDetalleSeleccionado: EventoMonitorItem | null = null;
  detalleSeleccionadoCampos: DetalleCampoVisual[] = [];

  constructor(
    private readonly detalleService: DetalleService,
    private readonly eventoService: EventoService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.cargarPanelDashboard();
    this.cargarEventos(0);
  }

  aplicarFiltrosPanel(): void {
    this.cargarPanelDashboard();
  }

  limpiarFiltrosPanel(): void {
    this.dashboardFiltros = this.crearFiltrosDashboardIniciales();
    this.cargarPanelDashboard();
  }

  aplicarFiltros(): void {
    this.cargarEventos(0);
  }

  limpiarFiltros(): void {
    this.filtros = this.crearFiltrosVacios();
    this.cargarEventos(0);
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0 || this.cargando) {
      return;
    }
    this.cargarEventos(this.paginaActual - 1);
  }

  irPaginaSiguiente(): void {
    if (this.paginaActual >= this.totalPaginas - 1 || this.cargando) {
      return;
    }
    this.cargarEventos(this.paginaActual + 1);
  }

  toggleTipoEvento(tipo: TipoEventoMonitor): void {
    if (this.filtros.tiposEvento.includes(tipo)) {
      this.filtros.tiposEvento = this.filtros.tiposEvento.filter((item) => item !== tipo);
      return;
    }

    this.filtros.tiposEvento = [...this.filtros.tiposEvento, tipo];
  }

  toggleEntidad(entidad: EntidadMonitor): void {
    if (this.filtros.entidades.includes(entidad)) {
      this.filtros.entidades = this.filtros.entidades.filter((item) => item !== entidad);
      return;
    }

    this.filtros.entidades = [...this.filtros.entidades, entidad];
  }

  estaTipoEventoSeleccionado(tipo: TipoEventoMonitor): boolean {
    return this.filtros.tiposEvento.includes(tipo);
  }

  estaEntidadSeleccionada(entidad: EntidadMonitor): boolean {
    return this.filtros.entidades.includes(entidad);
  }

  formatearEtiqueta(valor: string | null | undefined): string {
    if (!valor) {
      return '-';
    }

    return valor
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
  }

  formatearFecha(fecha: string): string {
    const fechaObj = new Date(fecha);
    return Number.isNaN(fechaObj.getTime()) ? fecha : fechaObj.toLocaleString('es-CO');
  }

  formatearMoneda(valor: number | null | undefined): string {
    const monto = Number(valor ?? 0);
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0,
    }).format(monto);
  }

  formatearNumero(valor: number | null | undefined): string {
    return new Intl.NumberFormat('es-CO').format(Number(valor ?? 0));
  }

  formatearGranularidad(granularidad: DashboardGranularidad): string {
    if (granularidad === 'DAY') {
      return 'Diario';
    }
    if (granularidad === 'WEEK') {
      return 'Semanal';
    }
    return 'Mensual';
  }

  porcentajeOcupacion(): number {
    const porcentaje = this.dashboardResumen?.operativo.ocupacionPorcentaje ?? 0;
    return this.limitarPorcentaje(porcentaje);
  }

  netoFinanciero(): number {
    return this.dashboardResumen?.financiero.neto ?? 0;
  }

  get resumenAlertasTotal(): number {
    return this.dashboardAlertas?.resumen.totalAlertas ?? this.dashboardResumen?.alertas.totalAlertas ?? 0;
  }

  trackByTipoUnidad(_: number, item: DashboardConteoUnidadDTO): string {
    return item.tipoUnidad;
  }

  trackByPeriodo(_: number, item: DashboardSerieFinancieraDTO): string {
    return item.periodo;
  }

  trackByCategoria(_: number, item: CategoriaMontoDTO): string {
    return item.categoria;
  }

  trackByAlerta(_: number, item: { tipo: string; fechaReferencia: string; mensaje: string }): string {
    return `${item.tipo}-${item.fechaReferencia}-${item.mensaje}`;
  }

  irATablaReservas(evento: EventoMonitorItem): void {
    const codigoReserva = evento.codigoReserva?.trim() ?? '';
    if (!codigoReserva) {
      return;
    }

    this.router.navigate(['/reservas'], {
      queryParams: { codigoReserva },
    });
  }

  irATablaEstancias(evento: EventoMonitorItem): void {
    const codigoEstancia = evento.codigoEstancia?.trim() ?? '';
    if (!codigoEstancia) {
      return;
    }

    this.router.navigate(['/estancias'], {
      queryParams: { codigoEstancia },
    });
  }

  abrirModalDetalle(evento: EventoMonitorItem): void {
    this.eventoDetalleSeleccionado = evento;
    this.detalleSeleccionadoCampos = this.construirCamposDetalle(evento.detalle);
    this.modalAbierto = true;
  }

  cerrarModalDetalle(): void {
    this.modalAbierto = false;
    this.eventoDetalleSeleccionado = null;
    this.detalleSeleccionadoCampos = [];
  }

  trackByEventoId(_: number, evento: EventoMonitorItem): number {
    return evento.id;
  }

  get indiceInicio(): number {
    return this.totalElementos === 0 ? 0 : this.paginaActual * this.tamanoPagina + 1;
  }

  get indiceFin(): number {
    return Math.min((this.paginaActual + 1) * this.tamanoPagina, this.totalElementos);
  }

  private cargarPanelDashboard(): void {
    this.dashboardCargando = true;
    this.dashboardError = '';

    const desde = this.formatearFechaInicioDia(this.dashboardFiltros.desde);
    const hasta = this.formatearFechaFinDia(this.dashboardFiltros.hasta);
    const diasAlertas = this.normalizarDiasAlertas(this.dashboardFiltros.diasAlertas);

    forkJoin({
      resumen: this.detalleService.obtenerDashboardResumen(desde, hasta),
      conteosUnidad: this.detalleService.obtenerDashboardConteosUnidad(),
      serie: this.detalleService.obtenerDashboardSerieFinanciera(desde, hasta, this.dashboardFiltros.granularidad),
      distribucion: this.detalleService.obtenerDashboardDistribucionFinanciera(desde, hasta),
      alertas: this.detalleService.obtenerDashboardAlertas(diasAlertas, 0, this.tamanoAlertas),
    }).subscribe({
      next: (response) => {
        this.dashboardResumen = response.resumen;
        this.dashboardConteosUnidad = response.conteosUnidad ?? [];
        this.dashboardSerieFinanciera = response.serie ?? [];
        this.dashboardDistribucion = response.distribucion;
        this.dashboardAlertas = response.alertas;
        this.actualizarGraficosDashboard();
        this.dashboardCargando = false;
      },
      error: (errorResponse: unknown) => {
        this.dashboardResumen = null;
        this.dashboardConteosUnidad = [];
        this.dashboardSerieFinanciera = [];
        this.dashboardDistribucion = null;
        this.dashboardAlertas = null;
        this.actualizarGraficosDashboard();
        this.dashboardError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar los indicadores del dashboard.'
        );
        this.dashboardCargando = false;
      },
    });
  }

  private actualizarGraficosDashboard(): void {
    this.unidadChartData = {
      labels: this.dashboardConteosUnidad.map((item) => this.formatearEtiqueta(item.tipoUnidad)),
      datasets: [
        {
          label: 'Disponible',
          data: this.dashboardConteosUnidad.map((item) => item.disponible),
          backgroundColor: '#15803d',
          borderRadius: 6,
          stack: 'estado',
        },
        {
          label: 'Parcial',
          data: this.dashboardConteosUnidad.map((item) => item.parcialmente),
          backgroundColor: '#d97706',
          borderRadius: 6,
          stack: 'estado',
        },
        {
          label: 'Ocupado',
          data: this.dashboardConteosUnidad.map((item) => item.ocupado),
          backgroundColor: '#b91c1c',
          borderRadius: 6,
          stack: 'estado',
        },
      ],
    };

    this.serieChartData = {
      labels: this.dashboardSerieFinanciera.map((item) => item.periodo),
      datasets: [
        {
          type: 'line',
          label: 'Ingresos',
          data: this.dashboardSerieFinanciera.map((item) => item.ingresos),
          borderColor: '#0f766e',
          backgroundColor: 'rgba(15, 118, 110, 0.2)',
          fill: true,
          tension: 0.3,
          pointRadius: 2,
        },
        {
          type: 'line',
          label: 'Gastos',
          data: this.dashboardSerieFinanciera.map((item) => item.gastos),
          borderColor: '#d97706',
          backgroundColor: 'rgba(217, 119, 6, 0.14)',
          fill: true,
          tension: 0.3,
          pointRadius: 2,
        },
        {
          type: 'line',
          label: 'Neto',
          data: this.dashboardSerieFinanciera.map((item) => item.neto),
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37, 99, 235, 0.12)',
          fill: false,
          tension: 0.3,
          pointRadius: 2,
        },
      ],
    };

    this.distribucionTipoChartData = this.crearDataDoughnut(
      this.dashboardDistribucion?.ingresosPorTipoPago ?? [],
      ['#0f766e', '#0891b2', '#2563eb', '#14b8a6', '#06b6d4']
    );

    this.distribucionMedioChartData = this.crearDataDoughnut(
      this.dashboardDistribucion?.ingresosPorMedioPago ?? [],
      ['#1d4ed8', '#2563eb', '#0ea5e9', '#0284c7', '#38bdf8']
    );

    this.distribucionGastoChartData = this.crearDataDoughnut(
      this.dashboardDistribucion?.gastosPorConcepto ?? [],
      ['#d97706', '#f59e0b', '#ea580c', '#b45309', '#fb923c']
    );
  }

  private crearDataDoughnut(categorias: CategoriaMontoDTO[], colores: string[]): ChartConfiguration<'doughnut'>['data'] {
    return {
      labels: categorias.map((item) => this.formatearEtiqueta(item.categoria)),
      datasets: [
        {
          data: categorias.map((item) => Number(item.monto ?? 0)),
          backgroundColor: categorias.map((_, index) => colores[index % colores.length]),
          borderWidth: 1,
          borderColor: '#f8fafc',
        },
      ],
    };
  }

  private cargarEventos(pagina: number): void {
    this.cargando = true;
    this.error = '';
    const filtrosConsulta = this.construirFiltrosConsulta();

    this.eventoService.obtenerEventos(filtrosConsulta, pagina, this.tamanoPagina).subscribe({
      next: (response) => {
        this.eventos = response.content;
        this.paginaActual = response.number;
        this.totalPaginas = response.totalPages;
        this.totalElementos = response.totalElements;
        this.cargando = false;
      },
      error: (errorResponse: unknown) => {
        this.eventos = [];
        this.error = extractBackendErrorMessage(errorResponse, 'No fue posible cargar los eventos.');
        this.cargando = false;
      },
    });
  }

  private crearFiltrosVacios(): EventoMonitorFiltros {
    return {
      tiposEvento: [],
      entidades: [],
      codigoReserva: '',
      codigoEstancia: '',
      fechaDesde: '',
      fechaHasta: '',
    };
  }

  private crearFiltrosDashboardIniciales(): DashboardFiltrosVisual {
    const hoy = new Date();
    const desde = new Date(hoy);
    desde.setDate(hoy.getDate() - 29);

    return {
      desde: this.formatearFechaSoloDia(desde),
      hasta: this.formatearFechaSoloDia(hoy),
      granularidad: 'DAY',
      diasAlertas: 7,
    };
  }

  private formatearFechaSoloDia(fecha: Date): string {
    const year = fecha.getFullYear();
    const month = String(fecha.getMonth() + 1).padStart(2, '0');
    const day = String(fecha.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private normalizarDiasAlertas(valor: number): number {
    const numero = Number(valor);
    if (!Number.isFinite(numero) || numero < 1) {
      this.dashboardFiltros.diasAlertas = 7;
      return 7;
    }

    const redondeado = Math.floor(numero);
    this.dashboardFiltros.diasAlertas = redondeado;
    return redondeado;
  }

  private limitarPorcentaje(valor: number): number {
    return Math.min(100, Math.max(0, Number.isFinite(valor) ? valor : 0));
  }

  private construirFiltrosConsulta(): EventoMonitorFiltros {
    return {
      ...this.filtros,
      codigoReserva: this.filtros.codigoReserva?.trim() ?? '',
      codigoEstancia: this.filtros.codigoEstancia?.trim() ?? '',
      fechaDesde: this.formatearFechaInicioDia(this.filtros.fechaDesde),
      fechaHasta: this.formatearFechaFinDia(this.filtros.fechaHasta),
    };
  }

  private formatearFechaInicioDia(fecha?: string): string {
    const fechaTrimmed = fecha?.trim() ?? '';
    if (!fechaTrimmed) {
      return '';
    }

    return `${fechaTrimmed}T00:00:00`;
  }

  private formatearFechaFinDia(fecha?: string): string {
    const fechaTrimmed = fecha?.trim() ?? '';
    if (!fechaTrimmed) {
      return '';
    }

    return `${fechaTrimmed}T23:59:59`;
  }

  private parsearDetalle(detalle: unknown): Record<string, unknown> | null {
    if (!detalle) {
      return null;
    }

    if (typeof detalle === 'object' && !Array.isArray(detalle)) {
      return detalle as Record<string, unknown>;
    }

    if (typeof detalle !== 'string') {
      return null;
    }

    const detalleTrimmed = detalle.trim();
    if (!detalleTrimmed) {
      return null;
    }

    try {
      const parsed = JSON.parse(detalleTrimmed) as unknown;
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return null;
      }

      return parsed as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private formatearValorDetalle(valor: unknown): string {
    if (valor === null || valor === undefined) {
      return '-';
    }

    if (typeof valor === 'string') {
      return this.formatearFechaDetalleSiAplica(valor);
    }

    if (typeof valor === 'number' || typeof valor === 'boolean') {
      return String(valor);
    }

    try {
      return JSON.stringify(valor);
    } catch {
      return String(valor);
    }
  }

  private formatearFechaDetalleSiAplica(valor: string): string {
    const valorTrimmed = valor.trim();
    if (!valorTrimmed) {
      return '-';
    }

    const isoDateTimePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,6})?)?(Z|[+\-]\d{2}:\d{2})?$/;
    if (!isoDateTimePattern.test(valorTrimmed)) {
      return valorTrimmed;
    }

    const parsed = new Date(valorTrimmed);
    if (Number.isNaN(parsed.getTime())) {
      return valorTrimmed;
    }

    return parsed.toLocaleString('es-CO');
  }

  private construirCamposDetalle(detalle: unknown): DetalleCampoVisual[] {
    const detalleJson = this.parsearDetalle(detalle);
    if (detalleJson) {
      return this.mapearObjetoACampos(detalleJson);
    }

    if (typeof detalle !== 'string') {
      return [{ etiqueta: 'Detalle', valor: '-' }];
    }

    const detalleTrimmed = detalle.trim();
    if (!detalleTrimmed) {
      return [{ etiqueta: 'Detalle', valor: '-' }];
    }

    const segmentos = detalleTrimmed
      .split('|')
      .map((segmento) => segmento.trim())
      .filter((segmento) => !!segmento);

    if (!segmentos.length) {
      return [{ etiqueta: 'Detalle', valor: detalleTrimmed }];
    }

    const campos = segmentos.map((segmento) => {
      const indexSeparador = segmento.indexOf(':');
      if (indexSeparador < 0) {
        return {
          etiqueta: 'Detalle',
          valor: segmento,
        };
      }

      const etiqueta = segmento.slice(0, indexSeparador).trim();
      const valorRaw = segmento.slice(indexSeparador + 1).trim();
      const cambio = this.parsearCambioDesdeValorRaw(valorRaw);

      if (cambio) {
        return {
          etiqueta: this.formatearEtiqueta(etiqueta),
          anterior: cambio.anterior,
          nuevo: cambio.nuevo,
        };
      }

      return {
        etiqueta: this.formatearEtiqueta(etiqueta),
        valor: valorRaw || '-',
      };
    });

    return campos.length ? campos : [{ etiqueta: 'Detalle', valor: detalleTrimmed }];
  }

  private mapearObjetoACampos(objeto: Record<string, unknown>): DetalleCampoVisual[] {
    return Object.entries(objeto).map(([clave, valor]) => {
      const cambio = this.parsearCambioDesdeValorRaw(valor);

      if (cambio) {
        return {
          etiqueta: this.formatearEtiqueta(clave),
          anterior: cambio.anterior,
          nuevo: cambio.nuevo,
        };
      }

      return {
        etiqueta: this.formatearEtiqueta(clave),
        valor: this.formatearValorDetalle(valor),
      };
    });
  }

  private parsearCambioDesdeValorRaw(valor: unknown): { anterior: string; nuevo: string } | null {
    let valorObj: unknown = valor;

    if (typeof valor === 'string') {
      const valorTrimmed = valor.trim();
      if (!valorTrimmed) {
        return null;
      }

      if (valorTrimmed.startsWith('{') && valorTrimmed.endsWith('}')) {
        try {
          valorObj = JSON.parse(valorTrimmed) as unknown;
        } catch {
          valorObj = valor;
        }
      }
    }

    if (!valorObj || typeof valorObj !== 'object' || Array.isArray(valorObj)) {
      return null;
    }

    const obj = valorObj as Record<string, unknown>;
    if (!('nuevo' in obj) && !('anterior' in obj)) {
      return null;
    }

    return {
      anterior: this.formatearValorDetalle(obj['anterior']),
      nuevo: this.formatearValorDetalle(obj['nuevo']),
    };
  }
}
