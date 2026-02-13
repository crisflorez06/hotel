import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';

import { EstadoPago, MedioPago, TipoPago } from '../../models/enums';
import { PagoDTO } from '../../models/pago-detalle.model';
import { PagoService } from '../../services/pago.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

@Component({
  selector: 'app-pagos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pagos.component.html',
  styleUrl: './pagos.component.css',
})
export class PagosComponent implements OnInit, OnDestroy {
  pagos: PagoDTO[] = [];
  cargando = false;
  error = '';

  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  readonly tamanoPagina = 10;

  filtroEstados: EstadoPago[] = [];
  filtroMediosPago: MedioPago[] = [];
  filtroTipoPago: TipoPago | '' = '';
  filtroCodigoEstancia = '';
  filtroFechaDesde = '';
  filtroFechaHasta = '';

  readonly estadosPago: EstadoPago[] = [
    'PENDIENTE',
    'COMPLETADO',
    'FALLIDO',
    'ELIMINADO',
    'MODIFICADO',
  ];
  readonly mediosPago: MedioPago[] = [
    'EFECTIVO',
    'TARJETA_CREDITO',
    'TARJETA_DEBITO',
    'TRANSFERENCIA_BANCARIA',
    'PLATAFORMA',
  ];
  readonly tiposPago: TipoPago[] = ['ANTICIPO_RESERVA', 'ANTICIPO_ESTANCIA', 'ESTANCIA_COMPLETADA'];
  private filtroCambioTimeout: ReturnType<typeof setTimeout> | null = null;
  private queryParamsSub: Subscription | null = null;

  constructor(
    private readonly pagoService: PagoService,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.queryParamsSub = this.route.queryParamMap.subscribe((params) => {
      const codigoEstancia = (params.get('codigoEstancia') ?? '').trim();
      const tipoPago = this.parsearTipoPago(params.get('tipoPago'));

      if (codigoEstancia !== this.filtroCodigoEstancia) {
        this.filtroCodigoEstancia = codigoEstancia;
      }
      if (tipoPago !== this.filtroTipoPago) {
        this.filtroTipoPago = tipoPago;
      }
      this.cargarPagos(0);
    });
  }

  ngOnDestroy(): void {
    if (this.filtroCambioTimeout) {
      clearTimeout(this.filtroCambioTimeout);
      this.filtroCambioTimeout = null;
    }
    this.queryParamsSub?.unsubscribe();
    this.queryParamsSub = null;
  }

  cargarPagos(pagina = this.paginaActual): void {
    this.cargando = true;
    this.error = '';

    this.pagoService
      .buscarPagos({
        pageable: {
          page: pagina,
          size: this.tamanoPagina,
          sort: ['fecha,desc'],
        },
        estados: this.filtroEstados.length ? this.filtroEstados : undefined,
        mediosPago: this.filtroMediosPago.length ? this.filtroMediosPago : undefined,
        tipoPago: this.filtroTipoPago || undefined,
        codigoEstancia: this.filtroCodigoEstancia.trim() || undefined,
        fechaDesde: this.normalizarFechaFiltro(this.filtroFechaDesde),
        fechaHasta: this.normalizarFechaFiltro(this.filtroFechaHasta),
      })
      .subscribe({
        next: (response) => {
          this.pagos = response.content;
          this.paginaActual = response.number;
          this.totalPaginas = response.totalPages;
          this.totalElementos = response.totalElements;
          this.cargando = false;
        },
        error: (errorResponse: unknown) => {
          this.pagos = [];
          this.error = extractBackendErrorMessage(errorResponse, 'No fue posible cargar los pagos.');
          this.cargando = false;
        },
      });
  }

  aplicarFiltros(): void {
    this.cargarPagos(0);
  }

  onFiltroCambio(): void {
    if (this.filtroCambioTimeout) {
      clearTimeout(this.filtroCambioTimeout);
    }

    this.filtroCambioTimeout = setTimeout(() => {
      this.aplicarFiltros();
      this.filtroCambioTimeout = null;
    }, 350);
  }

  limpiarFiltros(): void {
    if (this.filtroCambioTimeout) {
      clearTimeout(this.filtroCambioTimeout);
      this.filtroCambioTimeout = null;
    }

    this.filtroEstados = [];
    this.filtroMediosPago = [];
    this.filtroTipoPago = '';
    this.filtroCodigoEstancia = '';
    this.filtroFechaDesde = '';
    this.filtroFechaHasta = '';
    this.cargarPagos(0);
  }

  toggleEstado(estado: EstadoPago, checked: boolean): void {
    this.filtroEstados = checked
      ? Array.from(new Set([...this.filtroEstados, estado]))
      : this.filtroEstados.filter((item) => item !== estado);
    this.aplicarFiltros();
  }

  toggleMedioPago(medio: MedioPago, checked: boolean): void {
    this.filtroMediosPago = checked
      ? Array.from(new Set([...this.filtroMediosPago, medio]))
      : this.filtroMediosPago.filter((item) => item !== medio);
    this.aplicarFiltros();
  }

  estaEstadoSeleccionado(estado: EstadoPago): boolean {
    return this.filtroEstados.includes(estado);
  }

  estaMedioSeleccionado(medio: MedioPago): boolean {
    return this.filtroMediosPago.includes(medio);
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0 || this.cargando) {
      return;
    }
    this.cargarPagos(this.paginaActual - 1);
  }

  irPaginaSiguiente(): void {
    if (this.paginaActual >= this.totalPaginas - 1 || this.cargando) {
      return;
    }
    this.cargarPagos(this.paginaActual + 1);
  }

  formatearFecha(fecha: string): string {
    const fechaObj = new Date(fecha);
    return Number.isNaN(fechaObj.getTime()) ? fecha : fechaObj.toLocaleString('es-CO');
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

  trackByPagoId(_: number, pago: PagoDTO): number {
    return pago.id;
  }

  obtenerTipoPago(pago: PagoDTO): string {
    return pago.tipoPago ?? '-';
  }

  obtenerMonto(pago: PagoDTO): number | null {
    return pago.monto ?? null;
  }

  get indiceInicio(): number {
    return this.totalElementos === 0 ? 0 : this.paginaActual * this.tamanoPagina + 1;
  }

  get indiceFin(): number {
    return Math.min((this.paginaActual + 1) * this.tamanoPagina, this.totalElementos);
  }

  private normalizarFechaFiltro(fecha: string): string | undefined {
    if (!fecha) {
      return undefined;
    }

    const fechaLocal = fecha.length === 16 ? `${fecha}:00` : fecha;
    const fechaDate = new Date(fechaLocal);

    if (Number.isNaN(fechaDate.getTime())) {
      return fechaLocal;
    }

    return fechaDate.toISOString();
  }

  private parsearTipoPago(valor: string | null): TipoPago | '' {
    if (!valor) {
      return '';
    }

    return this.tiposPago.includes(valor as TipoPago) ? (valor as TipoPago) : '';
  }
}
