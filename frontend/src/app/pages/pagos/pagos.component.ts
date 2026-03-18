import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, Subject, catchError, finalize, switchMap, takeUntil, tap } from 'rxjs';

import { EstadoPago, MedioPago, TipoPago } from '../../models/enums';
import { PageResponse } from '../../models/page.model';
import { PagoDTO } from '../../models/pago-detalle.model';
import { PagoNuevoRequest } from '../../models/pago.model';
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
  exito = '';

  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  readonly tamanoPagina = 10;
  pagoSeleccionadoId: number | null = null;

  filtroEstados: EstadoPago[] = [];
  filtroMediosPago: MedioPago[] = [];
  filtroTipoPago: TipoPago | '' = '';
  filtroCodigoReserva = '';
  filtroCodigoEstancia = '';
  filtroFechaDesde = '';
  filtroFechaHasta = '';

  readonly estadosPago: EstadoPago[] = [
    'PENDIENTE',
    'COMPLETADO',
    'ELIMINADO',
  ];
  readonly mediosPago: MedioPago[] = [
    'EFECTIVO',
    'TARJETA_CREDITO',
    'TARJETA_DEBITO',
    'TRANSFERENCIA_BANCARIA',
    'PLATAFORMA',
  ];
  readonly tiposPago: TipoPago[] = [
    'ANTICIPO_RESERVA',
    'ANTICIPO_ESTANCIA',
    'ESTANCIA_COMPLETADA',
  ];

  mostrarModalEliminarPago = false;
  mostrarModalReemplazoPagoEstancia = false;
  mostrarModalConfirmarPagoPendiente = false;
  eliminandoPago = false;
  pagandoPendiente = false;
  modoPagarPendientes = false;
  pagoPendienteObjetivo: PagoDTO | null = null;
  pagoNotasModal: PagoDTO | null = null;

  reemplazoPagoEstancia = {
    monto: '',
    medioPago: 'EFECTIVO' as MedioPago,
    fecha: '',
    notas: '',
    estado: 'COMPLETADO' as EstadoPago,
  };
  private readonly destroy$ = new Subject<void>();
  private readonly recargaTabla$ = new Subject<void>();
  private idPagoFiltro: number | null = null;

  constructor(
    private readonly pagoService: PagoService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.inicializarCargaTabla();

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const idPago = this.parsearIdFiltro(params.get('idPago'));
      const codigoReserva = (params.get('codigoReserva') ?? '').trim();
      const codigoEstancia = (params.get('codigoEstancia') ?? '').trim();
      const tipoPago = this.parsearTipoPago(params.get('tipoPago'));

      if (idPago !== this.idPagoFiltro) {
        this.idPagoFiltro = idPago;
      }

      if (codigoReserva !== this.filtroCodigoReserva) {
        this.filtroCodigoReserva = codigoReserva;
      }
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
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarPagos(pagina = this.paginaActual): void {
    this.paginaActual = pagina;
    this.solicitarRecargaTabla();
  }

  aplicarFiltros(): void {
    this.cargarPagos(0);
  }

  limpiarFiltros(): void {
    this.modoPagarPendientes = false;
    this.pagoPendienteObjetivo = null;
    this.mostrarModalConfirmarPagoPendiente = false;
    this.filtroEstados = [];
    this.filtroMediosPago = [];
    this.filtroTipoPago = '';
    this.filtroCodigoReserva = '';
    this.filtroCodigoEstancia = '';
    this.filtroFechaDesde = '';
    this.filtroFechaHasta = '';
    this.cargarPagos(0);
  }

  abrirModalEliminarPago(): void {
    if (
      !this.puedeEliminarPagoSeleccionado() ||
      this.eliminandoPago ||
      this.pagandoPendiente
    ) {
      return;
    }

    const pago = this.obtenerPagoSeleccionado();
    if (!pago) {
      return;
    }

    this.error = '';
    this.exito = '';

    if (pago.tipoPago === 'ESTANCIA_COMPLETADA') {
      this.abrirModalReemplazoPagoEstancia(pago);
      return;
    }

    this.mostrarModalEliminarPago = true;
  }

  cerrarModalEliminarPago(): void {
    if (this.eliminandoPago) {
      return;
    }
    this.mostrarModalEliminarPago = false;
  }

  confirmarEliminarPago(): void {
    const pago = this.obtenerPagoSeleccionado();
    if (!pago || !this.puedeEliminarPago(pago) || this.eliminandoPago) {
      return;
    }

    this.eliminandoPago = true;
    this.error = '';
    this.exito = '';
    this.pagoService.eliminarPago(pago.id).subscribe({
      next: () => {
        this.eliminandoPago = false;
        this.mostrarModalEliminarPago = false;
        this.exito = 'Pago eliminado correctamente.';
        this.cargarPagos(this.paginaActual);
      },
      error: (errorResponse: unknown) => {
        this.eliminandoPago = false;
        this.mostrarModalEliminarPago = false;
        this.error = extractBackendErrorMessage(errorResponse, 'No fue posible eliminar el pago.');
      },
    });
  }

  cerrarModalReemplazoPagoEstancia(): void {
    if (this.eliminandoPago) {
      return;
    }

    this.mostrarModalReemplazoPagoEstancia = false;
  }

  confirmarReemplazoPagoEstancia(): void {
    const pago = this.obtenerPagoSeleccionado();
    if (!pago || pago.tipoPago !== 'ESTANCIA_COMPLETADA' || this.eliminandoPago) {
      return;
    }

    const monto = Number.parseFloat(this.reemplazoPagoEstancia.monto);
    const fecha = this.normalizarFechaHoraLocal(this.reemplazoPagoEstancia.fecha);
    if (!Number.isFinite(monto) || monto <= 0) {
      this.error = 'El monto del nuevo pago debe ser mayor a cero.';
      return;
    }

    if (!fecha) {
      this.error = 'La fecha del nuevo pago es obligatoria.';
      return;
    }

    const request: PagoNuevoRequest = {
      tipoPago: 'ESTANCIA_COMPLETADA',
      monto,
      medioPago: this.reemplazoPagoEstancia.medioPago,
      fecha,
      notas: this.reemplazoPagoEstancia.notas.trim() || undefined,
      estado: this.reemplazoPagoEstancia.estado,
    };

    this.eliminandoPago = true;
    this.error = '';
    this.exito = '';
    this.pagoService.eliminarPagoEstanciaCompletada(pago.id, request).subscribe({
      next: () => {
        this.eliminandoPago = false;
        this.mostrarModalReemplazoPagoEstancia = false;
        this.exito = 'Pago de estancia completada reemplazado correctamente.';
        this.cargarPagos(this.paginaActual);
      },
      error: (errorResponse: unknown) => {
        this.eliminandoPago = false;
        this.mostrarModalReemplazoPagoEstancia = false;
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible reemplazar el pago de estancia completada.'
        );
      },
    });
  }

  activarFiltroPagarPendientes(): void {
    if (this.cargando || this.eliminandoPago || this.pagandoPendiente) {
      return;
    }

    this.modoPagarPendientes = true;
    this.filtroEstados = ['PENDIENTE'];
    this.filtroMediosPago = [];
    this.filtroTipoPago = 'ESTANCIA_COMPLETADA';
    this.filtroCodigoReserva = '';
    this.filtroCodigoEstancia = '';
    this.filtroFechaDesde = '';
    this.filtroFechaHasta = '';
    this.pagoSeleccionadoId = null;
    this.error = '';
    this.exito = '';
    this.cargarPagos(0);
  }

  cancelarModoPagarPendientes(): void {
    if (this.pagandoPendiente) {
      return;
    }

    this.modoPagarPendientes = false;
    this.pagoPendienteObjetivo = null;
    this.mostrarModalConfirmarPagoPendiente = false;
    this.restablecerFiltrosVistaPorDefecto();
    this.cargarPagos(0);
  }

  abrirModalConfirmarPagoPendiente(pago: PagoDTO): void {
    if (!this.puedePagarPendiente(pago) || this.pagandoPendiente) {
      return;
    }

    this.pagoPendienteObjetivo = pago;
    this.error = '';
    this.exito = '';
    this.mostrarModalConfirmarPagoPendiente = true;
  }

  cerrarModalConfirmarPagoPendiente(): void {
    if (this.pagandoPendiente) {
      return;
    }

    this.mostrarModalConfirmarPagoPendiente = false;
    this.pagoPendienteObjetivo = null;
  }

  confirmarPagoPendienteDesdeModal(): void {
    const pago = this.pagoPendienteObjetivo;
    if (!pago || !this.puedePagarPendiente(pago) || this.pagandoPendiente) {
      return;
    }

    this.pagandoPendiente = true;
    this.error = '';
    this.exito = '';
    this.pagoService.pagarPagoPendiente(pago.id).subscribe({
      next: () => {
        this.pagandoPendiente = false;
        this.mostrarModalConfirmarPagoPendiente = false;
        this.pagoPendienteObjetivo = null;
        this.modoPagarPendientes = false;
        this.exito = 'Pago pendiente marcado como completado.';
        this.restablecerFiltrosVistaPorDefecto();
        this.cargarPagos(0);
      },
      error: (errorResponse: unknown) => {
        this.pagandoPendiente = false;
        this.mostrarModalConfirmarPagoPendiente = false;
        this.pagoPendienteObjetivo = null;
        this.error = extractBackendErrorMessage(errorResponse, 'No fue posible pagar el pendiente.');
      },
    });
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

  seleccionarPago(pago: PagoDTO): void {
    if (this.modoPagarPendientes && this.puedePagarPendiente(pago)) {
      this.abrirModalConfirmarPagoPendiente(pago);
      return;
    }

    this.pagoSeleccionadoId = this.pagoSeleccionadoId === pago.id ? null : pago.id;
  }

  esPagoSeleccionado(pago: PagoDTO): boolean {
    return this.pagoSeleccionadoId === pago.id;
  }

  puedeEliminarPagoSeleccionado(): boolean {
    const pago = this.obtenerPagoSeleccionado();
    return !!pago && this.puedeEliminarPago(pago);
  }

  obtenerTooltipEliminarPagoSeleccionado(): string | null {
    const pago = this.obtenerPagoSeleccionado();
    if (!pago) {
      return 'Selecciona un pago para eliminar.';
    }

    if (!this.puedeEliminarPago(pago)) {
      return 'Solo se pueden eliminar pagos COMPLETADO o PENDIENTE.';
    }

    if (pago.tipoPago === 'ESTANCIA_COMPLETADA') {
      return 'Se abrira un modal para reemplazar el pago de estancia completada.';
    }

    return null;
  }

  obtenerTooltipPagarPendiente(): string | null {
    if (this.modoPagarPendientes) {
      return 'Haz clic en una fila para pagar ese pendiente.';
    }

    return 'Filtra pagos PENDIENTE de tipo ESTANCIA_COMPLETADA para pagarlos por fila.';
  }

  obtenerTipoPago(pago: PagoDTO): string {
    return pago.tipoPago ?? '-';
  }

  obtenerMonto(pago: PagoDTO): number | null {
    return pago.monto ?? null;
  }

  abrirModalNotasPago(pago: PagoDTO): void {
    if (!this.tieneNotasPago(pago)) {
      return;
    }

    this.pagoNotasModal = pago;
  }

  cerrarModalNotasPago(): void {
    this.pagoNotasModal = null;
  }

  tieneNotasPago(pago: PagoDTO): boolean {
    return !!pago.notas?.trim();
  }

  obtenerTooltipNotasPago(pago: PagoDTO): string {
    return this.tieneNotasPago(pago) ? 'Ver notas' : 'Sin notas';
  }

  obtenerNotasPagoModal(pago: PagoDTO): string {
    const notas = pago.notas?.trim();
    return notas ? notas : 'Sin notas';
  }

  get indiceInicio(): number {
    return this.totalElementos === 0 ? 0 : this.paginaActual * this.tamanoPagina + 1;
  }

  get indiceFin(): number {
    return Math.min((this.paginaActual + 1) * this.tamanoPagina, this.totalElementos);
  }

  irATablaReservas(pago: PagoDTO): void {
    const codigoReserva = pago.codigoReserva?.trim() ?? '';
    if (!codigoReserva) {
      return;
    }

    this.router.navigate(['/reservas'], {
      queryParams: { codigoReserva },
    });
  }

  irATablaEstancias(pago: PagoDTO): void {
    const codigoEstancia = pago.codigoEstancia?.trim() ?? '';
    if (!codigoEstancia) {
      return;
    }

    this.router.navigate(['/estancias'], {
      queryParams: { codigoEstancia },
    });
  }

  formatearSoloFecha(fecha: string | null | undefined): string {
    if (!fecha) {
      return '-';
    }

    const date = new Date(fecha);
    if (Number.isNaN(date.getTime())) {
      return fecha;
    }

    return new Intl.DateTimeFormat('es-CO', {
      dateStyle: 'medium',
    }).format(date);
  }

  formatearSoloHora(fecha: string | null | undefined): string {
    if (!fecha) {
      return '';
    }

    const date = new Date(fecha);
    if (Number.isNaN(date.getTime())) {
      return '';
    }

    return new Intl.DateTimeFormat('es-CO', {
      timeStyle: 'short',
    }).format(date);
  }

  private obtenerPagoSeleccionado(): PagoDTO | undefined {
    if (this.pagoSeleccionadoId === null) {
      return undefined;
    }

    return this.pagos.find((pago) => pago.id === this.pagoSeleccionadoId);
  }

  private puedeEliminarPago(pago: PagoDTO): boolean {
    const estadoValido = pago.estado === 'COMPLETADO' || pago.estado === 'PENDIENTE';
    const tipoEliminable = pago.tipoPago !== 'CAMBIO_UNIDAD';
    return estadoValido && tipoEliminable;
  }

  private puedePagarPendiente(pago: PagoDTO): boolean {
    return pago.estado === 'PENDIENTE' && pago.tipoPago === 'ESTANCIA_COMPLETADA';
  }

  private abrirModalReemplazoPagoEstancia(pago: PagoDTO): void {
    this.reemplazoPagoEstancia = {
      monto: `${pago.monto ?? ''}`,
      medioPago: pago.medioPago ?? 'EFECTIVO',
      fecha: this.formatearFechaHoraInputDesdeFecha(pago.fecha),
      notas: this.normalizarNotasPagoParaInput(pago.notas),
      estado: pago.estado === 'PENDIENTE' ? 'PENDIENTE' : 'COMPLETADO',
    };
    this.mostrarModalReemplazoPagoEstancia = true;
  }

  private normalizarNotasPagoParaInput(notas: string | null | undefined): string {
    const texto = notas?.trim();
    if (!texto) {
      return '';
    }

    return texto.replace(/^\s*-\s?/, '');
  }

  private formatearFechaHoraInputDesdeFecha(fecha: string | null | undefined): string {
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

  private normalizarFechaHoraLocal(valor: string): string {
    const texto = valor.trim();
    if (!texto) {
      return '';
    }

    const normalizado = texto.replace(' ', 'T');
    if (/^\d{4}-\d{2}-\d{2}$/.test(normalizado)) {
      return `${normalizado}T00:00:00`;
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(normalizado)) {
      return `${normalizado}:00`;
    }

    return normalizado;
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

  private normalizarFechaFiltro(fecha: string, limite: 'desde' | 'hasta'): string | undefined {
    if (!fecha) {
      return undefined;
    }

    const valor = fecha.trim();
    if (!valor) {
      return undefined;
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) {
      return limite === 'desde' ? `${valor}T00:00:00` : `${valor}T23:59:59`;
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(valor)) {
      return limite === 'desde' ? `${valor}:00` : `${valor}:59`;
    }

    const fechaDate = new Date(valor);

    if (Number.isNaN(fechaDate.getTime())) {
      return valor;
    }

    const iso = fechaDate.toISOString();
    return limite === 'desde' ? iso : iso.replace(/\.\d{3}Z$/, '.999Z');
  }

  private parsearTipoPago(valor: string | null): TipoPago | '' {
    if (!valor) {
      return '';
    }

    return this.tiposPago.includes(valor as TipoPago) ? (valor as TipoPago) : '';
  }

  private restablecerFiltrosVistaPorDefecto(): void {
    this.filtroEstados = [];
    this.filtroMediosPago = [];
    this.filtroTipoPago = '';
    this.filtroCodigoReserva = '';
    this.filtroCodigoEstancia = '';
    this.filtroFechaDesde = '';
    this.filtroFechaHasta = '';
    this.pagoSeleccionadoId = null;
  }

  private inicializarCargaTabla(): void {
    this.recargaTabla$
      .pipe(
        takeUntil(this.destroy$),
        tap(() => {
          this.cargando = true;
          this.error = '';
        }),
        switchMap(() =>
          this.pagoService.buscarPagos(this.construirParametrosBusqueda()).pipe(
            finalize(() => {
              this.cargando = false;
            }),
            catchError((errorResponse: unknown) => {
              this.manejarErrorCargaTabla(errorResponse);
              return EMPTY;
            })
          )
        )
      )
      .subscribe((response) => {
        this.actualizarTabla(response);
      });
  }

  private solicitarRecargaTabla(): void {
    this.recargaTabla$.next();
  }

  private construirParametrosBusqueda() {
    return {
      pageable: {
        page: this.paginaActual,
        size: this.tamanoPagina,
        sort: ['fecha,desc'],
      },
      idPago: this.idPagoFiltro ?? undefined,
      estados: this.filtroEstados.length ? this.filtroEstados : undefined,
      mediosPago: this.filtroMediosPago.length ? this.filtroMediosPago : undefined,
      tipoPago: this.filtroTipoPago || undefined,
      codigoReserva: this.filtroCodigoReserva.trim() || undefined,
      codigoEstancia: this.filtroCodigoEstancia.trim() || undefined,
      fechaDesde: this.normalizarFechaFiltro(this.filtroFechaDesde, 'desde'),
      fechaHasta: this.normalizarFechaFiltro(this.filtroFechaHasta, 'hasta'),
    };
  }

  private actualizarTabla(response: PageResponse<PagoDTO>): void {
    this.pagos = response.content;
    if (this.pagoSeleccionadoId !== null) {
      const pagoSigueDisponible = this.pagos.some((pago) => pago.id === this.pagoSeleccionadoId);
      if (!pagoSigueDisponible) {
        this.pagoSeleccionadoId = null;
      }
    }
    this.paginaActual = response.number;
    this.totalPaginas = response.totalPages;
    this.totalElementos = response.totalElements;
  }

  private manejarErrorCargaTabla(errorResponse: unknown): void {
    this.pagos = [];
    this.pagoSeleccionadoId = null;
    this.totalElementos = 0;
    this.totalPaginas = 0;
    this.error = extractBackendErrorMessage(errorResponse, 'No fue posible cargar los pagos.');
  }

  private parsearIdFiltro(value: string | null): number | null {
    if (!value) {
      return null;
    }
    const parsed = Number.parseInt(value.trim(), 10);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }
}
