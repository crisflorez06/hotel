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
import { GastoDTO, GastoRequest } from '../../models/gasto.model';
import { DetalleService } from '../../services/detalle.service';
import { GastoService } from '../../services/gasto.service';

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

interface GastoFormVisual {
  concepto: string;
  descripcion: string;
  monto: string;
  fecha: string;
  metodoPago: string;
  referencia: string;
}

interface GastoFiltrosVisual {
  concepto: string;
  metodoPago: string;
  fechaDesde: string;
  fechaHasta: string;
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

  gastos: GastoDTO[] = [];
  gastosCargando = false;
  gastosError = '';
  gastosExito = '';
  gastoSeleccionadoId: number | null = null;
  modalGastoAbierto = false;
  modalEliminarGastoAbierto = false;
  guardandoGasto = false;
  eliminandoGasto = false;
  modalGastoModo: 'crear' | 'editar' = 'crear';
  gastoForm: GastoFormVisual = this.crearFormularioGastoVacio();
  gastoFiltros: GastoFiltrosVisual = this.crearFiltrosGastoVacios();

  readonly metodosPagoGasto: readonly string[] = [
    'EFECTIVO',
    'TARJETA_CREDITO',
    'TARJETA_DEBITO',
    'TRANSFERENCIA',
    'PLATAFORMA',
    'OTRO',
  ];
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

  constructor(
    private readonly detalleService: DetalleService,
    private readonly gastoService: GastoService
  ) {}

  ngOnInit(): void {
    this.cargarPanelDashboard();
    this.cargarGastos();
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

  trackByGastoId(_: number, gasto: GastoDTO): number {
    return gasto.id;
  }

  seleccionarGasto(gasto: GastoDTO): void {
    this.gastoSeleccionadoId = this.gastoSeleccionadoId === gasto.id ? null : gasto.id;
    this.gastosExito = '';
    this.gastosError = '';
  }

  esGastoSeleccionado(gasto: GastoDTO): boolean {
    return this.gastoSeleccionadoId === gasto.id;
  }

  abrirModalCrearGasto(): void {
    if (this.guardandoGasto || this.eliminandoGasto) {
      return;
    }
    this.modalGastoModo = 'crear';
    this.gastoForm = this.crearFormularioGastoVacio();
    this.modalGastoAbierto = true;
    this.gastosError = '';
    this.gastosExito = '';
  }

  abrirModalEditarGasto(): void {
    const gasto = this.obtenerGastoSeleccionado();
    if (!gasto || this.guardandoGasto || this.eliminandoGasto) {
      return;
    }
    this.modalGastoModo = 'editar';
    this.gastoForm = {
      concepto: gasto.concepto ?? '',
      descripcion: gasto.descripcion ?? '',
      monto: `${gasto.monto ?? ''}`,
      fecha: this.formatearFechaHoraInputDesdeIso(gasto.fecha),
      metodoPago: gasto.metodoPago ?? 'EFECTIVO',
      referencia: gasto.referencia ?? '',
    };
    this.modalGastoAbierto = true;
    this.gastosError = '';
    this.gastosExito = '';
  }

  cerrarModalGasto(): void {
    if (this.guardandoGasto) {
      return;
    }
    this.modalGastoAbierto = false;
  }

  guardarGasto(): void {
    if (this.guardandoGasto) {
      return;
    }

    const concepto = this.gastoForm.concepto.trim();
    const metodoPago = this.gastoForm.metodoPago.trim();
    const fecha = this.normalizarFechaHoraLocal(this.gastoForm.fecha);
    const monto = Number.parseFloat(this.gastoForm.monto);

    if (!concepto) {
      this.gastosError = 'El concepto del gasto es obligatorio.';
      return;
    }
    if (!Number.isFinite(monto) || monto <= 0) {
      this.gastosError = 'El monto debe ser mayor a cero.';
      return;
    }
    if (!fecha) {
      this.gastosError = 'La fecha del gasto es obligatoria.';
      return;
    }
    if (!metodoPago) {
      this.gastosError = 'El metodo de pago es obligatorio.';
      return;
    }
    const gastoSeleccionado = this.obtenerGastoSeleccionado();

    const request: GastoRequest = {
      concepto,
      descripcion: this.gastoForm.descripcion.trim() || undefined,
      monto,
      fecha,
      metodoPago,
      referencia: this.gastoForm.referencia.trim() || undefined,
      estado: this.modalGastoModo === 'editar' && gastoSeleccionado
        ? (gastoSeleccionado.estado ?? 'ACTIVO')
        : 'ACTIVO',
    };

    this.guardandoGasto = true;
    this.gastosError = '';
    this.gastosExito = '';

    const operacion$ =
      this.modalGastoModo === 'editar' && gastoSeleccionado
        ? this.gastoService.actualizar(gastoSeleccionado.id, request)
        : this.gastoService.crear(request);

    operacion$.subscribe({
      next: () => {
        this.guardandoGasto = false;
        this.modalGastoAbierto = false;
        this.gastosExito =
          this.modalGastoModo === 'editar'
            ? 'Gasto actualizado correctamente.'
            : 'Gasto creado correctamente.';
        this.cargarGastos();
        this.cargarPanelDashboard();
      },
      error: (errorResponse: unknown) => {
        this.guardandoGasto = false;
        this.gastosError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible guardar el gasto.'
        );
      },
    });
  }

  abrirModalEliminarGasto(): void {
    if (!this.obtenerGastoSeleccionado() || this.eliminandoGasto || this.guardandoGasto) {
      return;
    }
    this.modalEliminarGastoAbierto = true;
    this.gastosError = '';
    this.gastosExito = '';
  }

  cerrarModalEliminarGasto(): void {
    if (this.eliminandoGasto) {
      return;
    }
    this.modalEliminarGastoAbierto = false;
  }

  confirmarEliminarGasto(): void {
    const gasto = this.obtenerGastoSeleccionado();
    if (!gasto || this.eliminandoGasto) {
      return;
    }

    this.eliminandoGasto = true;
    this.gastosError = '';
    this.gastosExito = '';
    this.gastoService.eliminar(gasto.id).subscribe({
      next: () => {
        this.eliminandoGasto = false;
        this.modalEliminarGastoAbierto = false;
        this.gastosExito = 'Gasto eliminado correctamente.';
        this.gastoSeleccionadoId = null;
        this.cargarGastos();
        this.cargarPanelDashboard();
      },
      error: (errorResponse: unknown) => {
        this.eliminandoGasto = false;
        this.gastosError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible eliminar el gasto.'
        );
      },
    });
  }

  puedeEditarGasto(): boolean {
    return this.obtenerGastoSeleccionado() !== null;
  }

  puedeEliminarGasto(): boolean {
    return this.obtenerGastoSeleccionado() !== null;
  }

  obtenerTooltipEditarGasto(): string | null {
    if (!this.puedeEditarGasto()) {
      return 'Selecciona un gasto para editar.';
    }
    return null;
  }

  obtenerTooltipEliminarGasto(): string | null {
    if (!this.puedeEliminarGasto()) {
      return 'Selecciona un gasto para eliminar.';
    }
    return null;
  }

  formatearFechaHora(fecha: string | null | undefined): string {
    if (!fecha) {
      return '-';
    }
    const date = new Date(fecha);
    if (Number.isNaN(date.getTime())) {
      return fecha;
    }
    return date.toLocaleString('es-CO');
  }

  get gastosFiltrados(): GastoDTO[] {
    const concepto = this.gastoFiltros.concepto.trim().toLowerCase();
    const metodo = this.gastoFiltros.metodoPago.trim();
    const desde = this.gastoFiltros.fechaDesde.trim();
    const hasta = this.gastoFiltros.fechaHasta.trim();

    return this.gastos.filter((gasto) => {
      if (concepto) {
        const texto = `${gasto.concepto ?? ''} ${gasto.descripcion ?? ''} ${gasto.referencia ?? ''}`.toLowerCase();
        if (!texto.includes(concepto)) {
          return false;
        }
      }

      if (metodo && gasto.metodoPago !== metodo) {
        return false;
      }

      const fecha = new Date(gasto.fecha ?? '');
      if (Number.isNaN(fecha.getTime())) {
        return false;
      }

      if (desde) {
        const fechaDesde = new Date(`${desde}T00:00:00`);
        if (fecha < fechaDesde) {
          return false;
        }
      }

      if (hasta) {
        const fechaHasta = new Date(`${hasta}T23:59:59`);
        if (fecha > fechaHasta) {
          return false;
        }
      }

      return true;
    });
  }

  limpiarFiltrosGastos(): void {
    this.gastoFiltros = this.crearFiltrosGastoVacios();
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

  private cargarGastos(): void {
    this.gastosCargando = true;
    this.gastosError = '';

    this.gastoService.listar().subscribe({
      next: (gastos) => {
        this.gastos = [...gastos].sort((a, b) => {
          const fechaA = new Date(a.fecha ?? '').getTime();
          const fechaB = new Date(b.fecha ?? '').getTime();
          return fechaB - fechaA;
        });

        if (
          this.gastoSeleccionadoId !== null &&
          !this.gastos.some((gasto) => gasto.id === this.gastoSeleccionadoId)
        ) {
          this.gastoSeleccionadoId = null;
        }
        this.gastosCargando = false;
      },
      error: (errorResponse: unknown) => {
        this.gastos = [];
        this.gastosCargando = false;
        this.gastoSeleccionadoId = null;
        this.gastosError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar los gastos.'
        );
      },
    });
  }

  private obtenerGastoSeleccionado(): GastoDTO | null {
    if (this.gastoSeleccionadoId === null) {
      return null;
    }
    return this.gastosFiltrados.find((gasto) => gasto.id === this.gastoSeleccionadoId) ?? null;
  }

  private crearFormularioGastoVacio(): GastoFormVisual {
    return {
      concepto: '',
      descripcion: '',
      monto: '',
      fecha: this.obtenerFechaHoraActualInput(),
      metodoPago: 'EFECTIVO',
      referencia: '',
    };
  }

  private crearFiltrosGastoVacios(): GastoFiltrosVisual {
    return {
      concepto: '',
      metodoPago: '',
      fechaDesde: '',
      fechaHasta: '',
    };
  }

  private obtenerFechaHoraActualInput(): string {
    const ahora = new Date();
    const year = ahora.getFullYear();
    const month = `${ahora.getMonth() + 1}`.padStart(2, '0');
    const day = `${ahora.getDate()}`.padStart(2, '0');
    const hours = `${ahora.getHours()}`.padStart(2, '0');
    const minutes = `${ahora.getMinutes()}`.padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  private normalizarFechaHoraLocal(valor: string): string {
    const texto = valor.trim();
    if (!texto) {
      return '';
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test(texto)) {
      return `${texto}T00:00:00`;
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(texto)) {
      return `${texto}:00`;
    }

    return texto;
  }

  private formatearFechaHoraInputDesdeIso(fecha: string | null | undefined): string {
    if (!fecha) {
      return this.obtenerFechaHoraActualInput();
    }

    const date = new Date(fecha);
    if (Number.isNaN(date.getTime())) {
      return this.obtenerFechaHoraActualInput();
    }

    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    const hours = `${date.getHours()}`.padStart(2, '0');
    const minutes = `${date.getMinutes()}`.padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
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
