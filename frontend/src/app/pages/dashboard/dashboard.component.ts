import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ChartConfiguration,
  ChartData,
  ChartOptions,
  TooltipItem,
} from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { forkJoin } from 'rxjs';

import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import {
  DashboardDistribucionFinancieraDTO,
  DashboardResumenDTO,
  EstanciaMensualDTO,
} from '../../models/dashboard.model';
import { DetalleService } from '../../services/detalle.service';

interface DashboardFiltrosVisual {
  desde: string;
  hasta: string;
}

interface ReservaCanalVisual {
  canal: string;
  cantidad: number;
}

interface UnidadOperativaVisual {
  nombre: string;
  total: number;
  ocupadas: number;
  reservadas: number;
  disponibles: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, BaseChartDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  dashboardFiltros: DashboardFiltrosVisual = this.crearFiltrosDashboardIniciales();
  dashboardCargando = false;
  dashboardError = '';
  dashboardResumen: DashboardResumenDTO | null = null;
  dashboardDistribucion: DashboardDistribucionFinancieraDTO | null = null;

  readonly ocupacionChartType: ChartConfiguration<'pie'>['type'] = 'pie';
  readonly ingresosTipoChartType: ChartConfiguration<'bar'>['type'] = 'bar';
  readonly mediosPagoChartType: ChartConfiguration<'bar'>['type'] = 'bar';
  readonly canalesChartType: ChartConfiguration<'bar'>['type'] = 'bar';
  readonly estanciasMensualesChartType: ChartConfiguration<'line'>['type'] = 'line';

  readonly ocupacionChartOptions: ChartOptions<'pie'> = {
    responsive: true,
    maintainAspectRatio: false,
    rotation: -90,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          boxWidth: 10,
          boxHeight: 10,
          usePointStyle: true,
          color: '#5f6b61',
          font: {
            size: 11,
            weight: 600,
          },
        },
      },
      tooltip: {
        callbacks: {
          label: (context: TooltipItem<'pie'>) =>
            `${context.label}: ${Number(context.raw ?? 0).toFixed(1)}%`,
        },
      },
    },
  };

  readonly ingresosTipoChartOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        beginAtZero: true,
        grid: {
          color: 'rgba(95, 107, 97, 0.12)',
        },
        ticks: {
          color: '#5f6b61',
          callback: (value: string | number) => this.formatearMonedaCompacta(Number(value)),
        },
      },
      y: {
        grid: {
          display: false,
        },
        ticks: {
          color: '#1f2420',
        },
      },
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: (context: TooltipItem<'bar'>) => this.formatearMoneda(context.parsed.x),
        },
      },
    },
  };

  readonly mediosPagoChartOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        beginAtZero: true,
        grid: {
          color: 'rgba(95, 107, 97, 0.12)',
        },
        ticks: {
          color: '#5f6b61',
          callback: (value: string | number) => this.formatearMonedaCompacta(Number(value)),
        },
      },
      y: {
        grid: {
          display: false,
        },
        ticks: {
          color: '#1f2420',
        },
      },
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: (context: TooltipItem<'bar'>) => this.formatearMoneda(context.parsed.x),
        },
      },
    },
  };

  readonly canalesChartOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        beginAtZero: true,
        grid: {
          color: 'rgba(95, 107, 97, 0.12)',
        },
        ticks: {
          color: '#5f6b61',
          precision: 0,
        },
      },
      y: {
        grid: {
          display: false,
        },
        ticks: {
          color: '#1f2420',
        },
      },
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: (context: TooltipItem<'bar'>) => `${Number(context.raw ?? 0)} reservas`,
        },
      },
    },
  };

  readonly estanciasMensualesChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        grid: {
          display: false,
        },
        ticks: {
          color: '#5f6b61',
        },
      },
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(95, 107, 97, 0.12)',
        },
        ticks: {
          color: '#5f6b61',
          precision: 0,
        },
      },
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: (context: TooltipItem<'line'>) => `${Number(context.raw ?? 0)} estancias`,
        },
      },
    },
    elements: {
      line: {
        tension: 0.36,
        borderWidth: 2,
      },
      point: {
        radius: 3,
        hoverRadius: 4,
      },
    },
  };

  constructor(private readonly detalleService: DetalleService) {}

  ngOnInit(): void {
    this.cargarPanelDashboard();
  }

  aplicarFiltrosPanel(): void {
    this.cargarPanelDashboard();
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

  porcentajeOcupacion(): number {
    const porcentaje = this.dashboardResumen?.operativo.ocupacionPorcentaje ?? 0;
    return this.limitarPorcentaje(porcentaje);
  }

  netoFinanciero(): number {
    return this.dashboardDistribucion?.neto ?? 0;
  }

  porcentajeReservadas(): number {
    const porcentaje = this.dashboardResumen?.operativo.reservadasPorcentaje ?? 0;
    return this.limitarPorcentaje(porcentaje);
  }

  porcentajeDisponible(): number {
    const disponible = 100 - this.porcentajeOcupacion() - this.porcentajeReservadas();
    return this.limitarPorcentaje(disponible);
  }

  estadoOcupacion(): string {
    const ocupacion = this.porcentajeOcupacion();
    if (ocupacion >= 85) {
      return 'Alta demanda';
    }
    if (ocupacion >= 60) {
      return 'Operacion estable';
    }
    return 'Baja ocupacion';
  }

  get unidadesOperativas(): UnidadOperativaVisual[] {
    const operativo = this.dashboardResumen?.operativo;
    if (!operativo) {
      return [];
    }

    return [
      {
        nombre: 'Apartamentos',
        total: Number(operativo.apartamentosTotales ?? 0),
        ocupadas: this.ocupadasVisuales(operativo.apartamentosTotales, operativo.apartamentosOcupados),
        reservadas: this.reservadasVisuales(
          operativo.apartamentosTotales,
          operativo.apartamentosOcupados,
          operativo.apartamentosReservados
        ),
        disponibles: this.disponiblesVisuales(
          operativo.apartamentosTotales,
          operativo.apartamentosOcupados,
          operativo.apartamentosReservados
        ),
      },
      {
        nombre: 'Apartaestudios',
        total: Number(operativo.apartaestudioTotales ?? 0),
        ocupadas: this.ocupadasVisuales(operativo.apartaestudioTotales, operativo.apartaestudioOcupados),
        reservadas: this.reservadasVisuales(
          operativo.apartaestudioTotales,
          operativo.apartaestudioOcupados,
          operativo.apartaestudioReservados
        ),
        disponibles: this.disponiblesVisuales(
          operativo.apartaestudioTotales,
          operativo.apartaestudioOcupados,
          operativo.apartaestudioReservados
        ),
      },
      {
        nombre: 'Habitaciones',
        total: Number(operativo.habitacionesTotales ?? 0),
        ocupadas: this.ocupadasVisuales(operativo.habitacionesTotales, operativo.habitacionesOcupadas),
        reservadas: this.reservadasVisuales(
          operativo.habitacionesTotales,
          operativo.habitacionesOcupadas,
          operativo.habitacionesReservadas
        ),
        disponibles: this.disponiblesVisuales(
          operativo.habitacionesTotales,
          operativo.habitacionesOcupadas,
          operativo.habitacionesReservadas
        ),
      },
    ];
  }

  get ocupacionChartData(): ChartData<'pie'> {
    const ocupacion = this.porcentajeOcupacion();
    const reservadas = this.porcentajeReservadas();
    const disponible = this.porcentajeDisponible();

    return {
      labels: ['Ocupadas', 'Reservadas', 'Disponibles'],
      datasets: [
        {
          data: [ocupacion, reservadas, disponible],
          backgroundColor: ['#0f766e', '#2563eb', '#cbd5e1'],
          hoverBackgroundColor: ['#0b5b54', '#1d4ed8', '#b8c4d3'],
          borderColor: ['#ffffff', '#ffffff', '#ffffff'],
          borderWidth: 2,
        },
      ],
    };
  }

  get ingresosTipoChartData(): ChartData<'bar'> {
    const items = [...(this.dashboardDistribucion?.ingresosPorTipoPago ?? [])].sort(
      (a, b) => Number(b.monto ?? 0) - Number(a.monto ?? 0)
    );

    return {
      labels: items.map((item) => this.formatearEtiqueta(item.categoria)),
      datasets: [
        {
          label: 'Monto',
          data: items.map((item) => Number(item.monto ?? 0)),
          backgroundColor: ['#0f766e', '#14b8a6', '#1d4ed8', '#0ea5e9', '#16a34a', '#22c55e'],
          borderRadius: 8,
          borderSkipped: false,
          maxBarThickness: 30,
          categoryPercentage: 0.66,
          barPercentage: 0.9,
        },
      ],
    };
  }

  get mediosPagoChartData(): ChartData<'bar'> {
    const items = [...(this.dashboardDistribucion?.ingresosPorMedioPago ?? [])].sort(
      (a, b) => Number(b.monto ?? 0) - Number(a.monto ?? 0)
    );

    return {
      labels: items.map((item) => this.formatearEtiqueta(item.categoria)),
      datasets: [
        {
          label: 'Monto',
          data: items.map((item) => Number(item.monto ?? 0)),
          backgroundColor: ['#1d4ed8', '#0f766e', '#0ea5e9', '#14b8a6', '#22c55e', '#84cc16'],
          borderRadius: 8,
          borderSkipped: false,
          maxBarThickness: 20,
          categoryPercentage: 0.74,
          barPercentage: 0.84,
        },
      ],
    };
  }

  get canalesChartData(): ChartData<'bar'> {
    const canales = this.reservasPorCanalVisual;

    return {
      labels: canales.map((canal) => this.formatearEtiqueta(canal.canal)),
      datasets: [
        {
          label: 'Reservas',
          data: canales.map((canal) => canal.cantidad),
          backgroundColor: 'rgba(15, 118, 110, 0.78)',
          borderRadius: 8,
          borderSkipped: false,
          maxBarThickness: 18,
        },
      ],
    };
  }

  get estanciasUltimos12Meses(): EstanciaMensualDTO[] {
    return this.dashboardResumen?.operativo.estanciasUltimos12Meses ?? [];
  }

  get estanciasMensualesChartData(): ChartData<'line'> {
    const estancias = this.estanciasUltimos12Meses;

    return {
      labels: estancias.map((item) => item.periodo),
      datasets: [
        {
          label: 'Estancias',
          data: estancias.map((item) => Number(item.cantidad ?? 0)),
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37, 99, 235, 0.18)',
          fill: true,
          pointBackgroundColor: '#1d4ed8',
        },
      ],
    };
  }

  get reservasPorCanalVisual(): ReservaCanalVisual[] {
    const reservasPorCanal = this.dashboardResumen?.operativo.reservasPorCanal ?? {};

    return Object.entries(reservasPorCanal)
      .map(([canal, cantidad]) => ({
        canal,
        cantidad: Number(cantidad ?? 0),
      }))
      .sort((a, b) => b.cantidad - a.cantidad);
  }

  porcentajeSobreTotal(valor: number | null | undefined, total: number | null | undefined): number {
    const totalNormalizado = Number(total ?? 0);
    if (totalNormalizado <= 0) {
      return 0;
    }

    const valorNormalizado = Number(valor ?? 0);
    return this.limitarPorcentaje((valorNormalizado / totalNormalizado) * 100);
  }

  ocupadasVisuales(total: number | null | undefined, ocupadas: number | null | undefined): number {
    const totalNormalizado = Math.max(0, Number(total ?? 0));
    if (!totalNormalizado) {
      return 0;
    }

    const ocupadasNormalizado = Math.max(0, Number(ocupadas ?? 0));
    return Math.min(ocupadasNormalizado, totalNormalizado);
  }

  reservadasVisuales(
    total: number | null | undefined,
    ocupadas: number | null | undefined,
    reservadas: number | null | undefined
  ): number {
    const totalNormalizado = Math.max(0, Number(total ?? 0));
    if (!totalNormalizado) {
      return 0;
    }

    const ocupadasAjustadas = this.ocupadasVisuales(totalNormalizado, ocupadas);
    const reservadasNormalizado = Math.max(0, Number(reservadas ?? 0));
    return Math.min(reservadasNormalizado, Math.max(totalNormalizado - ocupadasAjustadas, 0));
  }

  disponiblesVisuales(
    total: number | null | undefined,
    ocupadas: number | null | undefined,
    reservadas: number | null | undefined
  ): number {
    const totalNormalizado = Math.max(0, Number(total ?? 0));
    if (!totalNormalizado) {
      return 0;
    }

    const ocupadasAjustadas = this.ocupadasVisuales(totalNormalizado, ocupadas);
    const reservadasAjustadas = this.reservadasVisuales(totalNormalizado, ocupadasAjustadas, reservadas);
    return Math.max(totalNormalizado - ocupadasAjustadas - reservadasAjustadas, 0);
  }

  trackByUnidad(_: number, item: UnidadOperativaVisual): string {
    return item.nombre;
  }

  private cargarPanelDashboard(): void {
    this.dashboardCargando = true;
    this.dashboardError = '';

    const desde = this.formatearFechaInicioDia(this.dashboardFiltros.desde);
    const hasta = this.formatearFechaFinDia(this.dashboardFiltros.hasta);

    forkJoin({
      resumen: this.detalleService.obtenerDashboardResumen(),
      distribucion: this.detalleService.obtenerDashboardDistribucionFinanciera(desde, hasta),
    }).subscribe({
      next: (response) => {
        this.dashboardResumen = response.resumen;
        this.dashboardDistribucion = response.distribucion;
        this.dashboardCargando = false;
      },
      error: (errorResponse: unknown) => {
        this.dashboardResumen = null;
        this.dashboardDistribucion = null;
        this.dashboardError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar los indicadores del dashboard.'
        );
        this.dashboardCargando = false;
      },
    });
  }

  private crearFiltrosDashboardIniciales(): DashboardFiltrosVisual {
    return {
      desde: '',
      hasta: '',
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

  private limitarPorcentaje(valor: number): number {
    return Math.min(100, Math.max(0, Number.isFinite(valor) ? valor : 0));
  }

  private formatearMonedaCompacta(valor: number): string {
    const abs = Math.abs(valor);
    if (abs >= 1_000_000) {
      return `$${(valor / 1_000_000).toFixed(1)}M`;
    }
    if (abs >= 1_000) {
      return `$${(valor / 1_000).toFixed(0)}K`;
    }
    return `$${Math.round(valor)}`;
  }

}
