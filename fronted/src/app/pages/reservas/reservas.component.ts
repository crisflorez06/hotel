import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';

import { PageResponse } from '../../models/page.model';
import { CanalReserva, EstadoReserva, ModoOcupacion, TipoUnidad } from '../../models/enums';
import { ReservaTablaFiltros, ReservaTablaItem } from '../../models/reserva-tabla.model';
import { ReservaService } from '../../services/reserva.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

@Component({
  selector: 'app-reservas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './reservas.component.html',
  styleUrl: './reservas.component.css',
})
export class ReservasComponent implements OnInit, OnDestroy {
  readonly estadosReservaDisponibles: EstadoReserva[] = [
    'PENDIENTE',
    'CONFIRMADA',
    'COMPLETADA',
    'CANCELADA',
    'EXPIRADA',
  ];

  readonly canalesDisponibles: CanalReserva[] = [
    'PLATAFORMA_AIRBINB',
    'PLATAFORMA_BOOKING',
    'PLATAFORMA_EXPEDIA',
    'WHATSAPP',
    'TELEFONO',
    'EMAIL',
    'MOSTRADOR',
  ];

  readonly modosOcupacionDisponibles: Array<{ label: string; value: ModoOcupacion | '' }> = [
    { label: 'Todos', value: '' },
    { label: 'Completo', value: 'COMPLETO' },
    { label: 'Individual', value: 'INDIVIDUAL' },
  ];

  readonly tiposUnidadDisponibles: Array<{ label: string; value: TipoUnidad | '' }> = [
    { label: 'Todos', value: '' },
    { label: 'Apartamento', value: 'APARTAMENTO' },
    { label: 'Apartaestudio', value: 'APARTAESTUDIO' },
    { label: 'Habitacion', value: 'HABITACION' },
  ];

  readonly pageSizeOptions = [10, 20, 50, 100];

  filtros: ReservaTablaFiltros = this.crearFiltrosVacios();

  reservas: ReservaTablaItem[] = [];
  cargando = false;
  error = '';
  accionError = '';
  accionExito = '';

  page = 0;
  size = 20;
  totalElements = 0;
  totalPages = 0;

  eliminandoReservaId: number | null = null;
  reservaSeleccionadaId: number | null = null;

  private filtroCambioTimeout: ReturnType<typeof setTimeout> | null = null;
  private queryParamsSub: Subscription | null = null;

  constructor(
    private readonly reservaService: ReservaService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.queryParamsSub = this.route.queryParamMap.subscribe((params) => {
      const codigoReserva = (params.get('codigoReserva') ?? '').trim();
      if (codigoReserva !== this.filtros.codigoReserva) {
        this.filtros.codigoReserva = codigoReserva;
      }
      this.page = 0;
      this.cargarReservas();
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

  aplicarFiltros(): void {
    this.accionError = '';
    this.accionExito = '';
    this.page = 0;
    this.cargarReservas();
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
    this.filtros = this.crearFiltrosVacios();
    this.page = 0;
    this.accionError = '';
    this.accionExito = '';
    this.cargarReservas();
  }

  toggleEstado(estado: EstadoReserva, checked: boolean): void {
    this.filtros.estados = checked
      ? Array.from(new Set([...this.filtros.estados, estado]))
      : this.filtros.estados.filter((item) => item !== estado);
    this.aplicarFiltros();
  }

  toggleCanal(canal: CanalReserva, checked: boolean): void {
    this.filtros.canales = checked
      ? Array.from(new Set([...this.filtros.canales, canal]))
      : this.filtros.canales.filter((item) => item !== canal);
    this.aplicarFiltros();
  }

  estaEstadoSeleccionado(estado: EstadoReserva): boolean {
    return this.filtros.estados.includes(estado);
  }

  estaCanalSeleccionado(canal: CanalReserva): boolean {
    return this.filtros.canales.includes(canal);
  }

  cambiarPageSize(size: number): void {
    this.size = size;
    this.page = 0;
    this.cargarReservas();
  }

  irPaginaAnterior(): void {
    if (this.page <= 0 || this.cargando) {
      return;
    }

    this.page -= 1;
    this.cargarReservas();
  }

  irPaginaSiguiente(): void {
    if (this.cargando || this.page + 1 >= this.totalPages) {
      return;
    }

    this.page += 1;
    this.cargarReservas();
  }

  trackByReservaId(_: number, reserva: ReservaTablaItem): number {
    return reserva.id;
  }

  seleccionarReserva(reserva: ReservaTablaItem): void {
    this.reservaSeleccionadaId =
      this.reservaSeleccionadaId === reserva.id ? null : reserva.id;
  }

  esReservaSeleccionada(reserva: ReservaTablaItem): boolean {
    return this.reservaSeleccionadaId === reserva.id;
  }

  editarReservaSeleccionada(): void {
    const reserva = this.obtenerReservaSeleccionada();
    if (!reserva) {
      return;
    }

    this.editarReserva(reserva);
  }

  eliminarReservaSeleccionada(): void {
    const reserva = this.obtenerReservaSeleccionada();
    if (!reserva) {
      return;
    }

    this.eliminarReserva(reserva);
  }

  puedeGestionarReservaSeleccionada(): boolean {
    const reserva = this.obtenerReservaSeleccionada();
    return !!reserva && this.puedeGestionarReserva(reserva);
  }

  estaEliminandoReservaSeleccionada(): boolean {
    const reserva = this.obtenerReservaSeleccionada();
    return !!reserva && this.eliminandoReservaId === reserva.id;
  }

  formatearEtiqueta(valor: string | null | undefined): string {
    return (valor ?? '')
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
  }

  formatearFecha(fecha: string | null | undefined): string {
    if (!fecha) {
      return '-';
    }

    const date = new Date(fecha);
    if (Number.isNaN(date.getTime())) {
      return fecha;
    }

    return new Intl.DateTimeFormat('es-CO', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
  }

  formatearMontoPago(monto: number | null | undefined): string {
    const valor = monto ?? 0;
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0,
    }).format(valor);
  }

  tienePagosModificados(reserva: ReservaTablaItem): boolean {
    return (reserva.cantidadPagosModificadosOEliminados ?? 0) > 0;
  }

  irAPagosAnticipoReserva(reserva: ReservaTablaItem): void {
    const codigoEstancia = reserva.codigoEstancia?.trim() ?? '';
    if (!codigoEstancia) {
      this.accionError = 'La reserva no tiene codigo de estancia asociado para consultar pagos.';
      this.accionExito = '';
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.router.navigate(['/pagos'], {
      queryParams: {
        codigoEstancia,
        tipoPago: 'ANTICIPO_RESERVA',
      },
    });
  }

  irATablaEstancias(reserva: ReservaTablaItem): void {
    const codigoEstancia = reserva.codigoEstancia?.trim() ?? '';
    if (!codigoEstancia) {
      this.accionError = 'La reserva no tiene codigo de estancia asociado.';
      this.accionExito = '';
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.router.navigate(['/estancias'], {
      queryParams: { codigoEstancia },
    });
  }

  get indiceInicio(): number {
    return this.totalElements === 0 ? 0 : this.page * this.size + 1;
  }

  get indiceFin(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }

  puedeGestionarReserva(reserva: ReservaTablaItem): boolean {
    return reserva.estadoReserva !== 'COMPLETADA' && reserva.estadoReserva !== 'CANCELADA';
  }

  editarReserva(reserva: ReservaTablaItem): void {
    if (!this.puedeGestionarReserva(reserva)) {
      this.accionError = 'No se pueden editar reservas completadas o canceladas.';
      this.accionExito = '';
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.router.navigate(['/reservas/nueva'], {
      state: {
        editMode: true,
        returnTo: '/reservas',
        reserva: {
          id: reserva.id,
          codigo: reserva.codigoUnidad,
          tipoUnidad: reserva.tipoUnidad,
          idOcupante: reserva.idCliente,
          nombreCliente: reserva.nombreCliente,
          numeroPersonas: reserva.numeroPersonas,
          canalReserva: reserva.canalReserva,
          entradaEstimada: reserva.entradaEstimada,
          salidaEstimada: reserva.salidaEstimada,
        },
      },
    });
  }

  eliminarReserva(reserva: ReservaTablaItem): void {
    if (!this.puedeGestionarReserva(reserva)) {
      this.accionError = 'No se pueden eliminar reservas completadas o canceladas.';
      this.accionExito = '';
      return;
    }

    const confirmacion = window.confirm(
      `Se cancelara la reserva ${reserva.codigoReserva}. Esta accion no se puede deshacer.`
    );
    if (!confirmacion) {
      return;
    }

    this.eliminandoReservaId = reserva.id;
    this.accionError = '';
    this.accionExito = '';

    this.reservaService.eliminarReserva(reserva.id).subscribe({
      next: () => {
        this.eliminandoReservaId = null;
        this.accionExito = 'Reserva cancelada correctamente.';
        this.cargarReservas();
      },
      error: (errorResponse: unknown) => {
        this.eliminandoReservaId = null;
        this.accionError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible eliminar la reserva.'
        );
        this.accionExito = '';
      },
    });
  }

  private cargarReservas(): void {
    this.cargando = true;
    this.error = '';

    const filtros: ReservaTablaFiltros = {
      ...this.filtros,
      fechaCreacionDesde: this.normalizarFechaHora(this.filtros.fechaCreacionDesde),
      fechaCreacionHasta: this.normalizarFechaHora(this.filtros.fechaCreacionHasta),
      entradaDesde: this.normalizarFechaHora(this.filtros.entradaDesde),
      entradaHasta: this.normalizarFechaHora(this.filtros.entradaHasta),
      salidaDesde: this.normalizarFechaHora(this.filtros.salidaDesde),
      salidaHasta: this.normalizarFechaHora(this.filtros.salidaHasta),
    };

    const sort = ['fechaCreacion,desc'];

    this.reservaService.obtenerTabla(filtros, this.page, this.size, sort).subscribe({
      next: (response: PageResponse<ReservaTablaItem>) => {
        this.reservas = response.content;
        if (this.reservaSeleccionadaId !== null) {
          const reservaSigueDisponible = this.reservas.some(
            (reserva) => reserva.id === this.reservaSeleccionadaId
          );
          if (!reservaSigueDisponible) {
            this.reservaSeleccionadaId = null;
          }
        }
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.page = response.number ?? this.page;
        this.size = response.size;
        this.cargando = false;
      },
      error: (errorResponse: unknown) => {
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar la tabla de reservas.'
        );
        this.reservas = [];
        this.reservaSeleccionadaId = null;
        this.totalElements = 0;
        this.totalPages = 0;
        this.cargando = false;
      },
    });
  }

  private crearFiltrosVacios(): ReservaTablaFiltros {
    return {
      estados: [],
      canales: [],
      modoOcupacion: '',
      tipoUnidad: '',
      codigoReserva: '',
      codigoUnidad: '',
      nombreCliente: '',
      numeroDocumentoCliente: '',
      fechaCreacionDesde: '',
      fechaCreacionHasta: '',
      entradaDesde: '',
      entradaHasta: '',
      salidaDesde: '',
      salidaHasta: '',
    };
  }

  private normalizarFechaHora(fecha: string): string {
    const valor = fecha.trim();
    if (!valor) {
      return '';
    }

    return valor.length === 16 ? `${valor}:00` : valor;
  }

  obtenerReservaSeleccionada(): ReservaTablaItem | null {
    if (this.reservaSeleccionadaId === null) {
      return null;
    }

    return this.reservas.find((reserva) => reserva.id === this.reservaSeleccionadaId) ?? null;
  }

}
