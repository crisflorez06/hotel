import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { EstadoPago, MedioPago, TipoPago } from '../../models/enums';
import { PagoDTO } from '../../models/pago-detalle.model';
import { PagoService } from '../../services/pago.service';

@Component({
  selector: 'app-pagos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pagos.component.html',
  styleUrl: './pagos.component.css',
})
export class PagosComponent implements OnInit {
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
  filtroFechaDesde = '';
  filtroFechaHasta = '';

  readonly estadosPago: EstadoPago[] = ['PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO'];
  readonly mediosPago: MedioPago[] = [
    'EFECTIVO',
    'TARJETA_CREDITO',
    'TARJETA_DEBITO',
    'TRANSFERENCIA_BANCARIA',
    'PLATAFORMA',
  ];
  readonly tiposPago: TipoPago[] = ['RESERVA', 'ESTANCIA'];

  constructor(private readonly pagoService: PagoService) {}

  ngOnInit(): void {
    this.cargarPagos();
  }

  cargarPagos(pagina = this.paginaActual): void {
    this.cargando = true;
    this.error = '';

    this.pagoService
      .buscarPagos({
        page: pagina,
        size: this.tamanoPagina,
        estados: this.filtroEstados.length ? this.filtroEstados : undefined,
        mediosPago: this.filtroMediosPago.length ? this.filtroMediosPago : undefined,
        tipoPago: this.filtroTipoPago || undefined,
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
        error: () => {
          this.pagos = [];
          this.error = 'No fue posible cargar los pagos.';
          this.cargando = false;
        },
      });
  }

  aplicarFiltros(): void {
    this.cargarPagos(0);
  }

  limpiarFiltros(): void {
    this.filtroEstados = [];
    this.filtroMediosPago = [];
    this.filtroTipoPago = '';
    this.filtroFechaDesde = '';
    this.filtroFechaHasta = '';
    this.cargarPagos(0);
  }

  toggleEstado(estado: EstadoPago, checked: boolean): void {
    this.filtroEstados = checked
      ? Array.from(new Set([...this.filtroEstados, estado]))
      : this.filtroEstados.filter((item) => item !== estado);
  }

  toggleMedioPago(medio: MedioPago, checked: boolean): void {
    this.filtroMediosPago = checked
      ? Array.from(new Set([...this.filtroMediosPago, medio]))
      : this.filtroMediosPago.filter((item) => item !== medio);
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

  obtenerTipoPago(pago: PagoDTO): string {
    return pago.tipoPago ?? '-';
  }

  obtenerMonto(pago: PagoDTO): number | null {
    return pago.monto ?? null;
  }

  private normalizarFechaFiltro(fecha: string): string | undefined {
    if (!fecha) {
      return undefined;
    }
    if (fecha.length === 16) {
      return `${fecha}:00`;
    }
    return fecha;
  }
}
