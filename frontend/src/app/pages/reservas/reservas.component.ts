import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription, Subject, Observable, of, catchError, finalize, switchMap, takeUntil, tap } from 'rxjs';

import { PageResponse } from '../../models/page.model';
import { CanalReserva, EstadoReserva, TipoUnidad } from '../../models/enums';
import { ReservaTablaFiltros, ReservaTablaItem } from '../../models/reserva-tabla.model';
import { ReservaService } from '../../services/reserva.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

type ResumenFiltroClave = 'tipoUnidad' | 'estados' | 'canales' | 'creacion' | 'entrada' | 'salida';

interface ResumenFiltroItem {
  clave: ResumenFiltroClave;
  texto: string;
}

type RangoRapido = 'HOY' | 'MANANA' | 'ESTA_SEMANA' | 'PROXIMOS_7_DIAS' | 'ESTE_MES';
type RangoRapidoCampo = 'rangoGeneral' | 'creacion' | 'entrada' | 'salida';
type AccionReserva = 'editar' | 'eliminar' | 'dar entrada';

@Component({
  selector: 'app-reservas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './reservas.component.html',
  styleUrl: './reservas.component.css',
})
export class ReservasComponent implements OnInit, OnDestroy {
  readonly estadosReservaDisponibles: EstadoReserva[] = [
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

  readonly tiposUnidadDisponibles: TipoUnidad[] = ['APARTAMENTO', 'APARTAESTUDIO', 'HABITACION'];

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
  reservaPendienteEliminacion: ReservaTablaItem | null = null;
  reservaSeleccionadaId: number | null = null;
  filtrosAvanzadosAbiertos = false;
  rangoRapidoSeleccionado: Record<RangoRapidoCampo, RangoRapido | null> = {
    rangoGeneral: null,
    creacion: null,
    entrada: null,
    salida: null,
  };

  private queryParamsSub: Subscription | null = null;
  private readonly recargaTabla$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly reservaService: ReservaService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.recargaTabla$
      .pipe(
        switchMap(() => this.ejecutarCargaReservas()),
        takeUntil(this.destroy$)
      )
      .subscribe();

    this.queryParamsSub = this.route.queryParamMap.subscribe((params) => {
      const codigoReserva = (params.get('codigoReserva') ?? '').trim();
      const codigoUnidad = (params.get('codigoUnidad') ?? '').trim();
      if (codigoReserva !== this.filtros.codigoReserva) {
        this.filtros.codigoReserva = codigoReserva;
      }
      if (codigoUnidad !== this.filtros.codigoUnidad) {
        this.filtros.codigoUnidad = codigoUnidad;
      }
      this.page = 0;
      this.cargarReservas();
    });
  }

  ngOnDestroy(): void {
    this.queryParamsSub?.unsubscribe();
    this.queryParamsSub = null;
    this.destroy$.next();
    this.destroy$.complete();
    this.recargaTabla$.complete();
  }

  aplicarFiltros(): void {
    this.accionError = '';
    this.accionExito = '';
    this.page = 0;
    this.cargarReservas();
  }

  limpiarFiltros(): void {
    this.filtros = this.crearFiltrosVacios();
    this.rangoRapidoSeleccionado = {
      rangoGeneral: null,
      creacion: null,
      entrada: null,
      salida: null,
    };
    this.page = 0;
    this.accionError = '';
    this.accionExito = '';
    this.cargarReservas();
  }

  toggleEstado(estado: EstadoReserva, checked: boolean): void {
    this.filtros.estados = checked
      ? Array.from(new Set([...this.filtros.estados, estado]))
      : this.filtros.estados.filter((item) => item !== estado);
  }

  toggleCanal(canal: CanalReserva, checked: boolean): void {
    this.filtros.canales = checked
      ? Array.from(new Set([...this.filtros.canales, canal]))
      : this.filtros.canales.filter((item) => item !== canal);
  }

  estaEstadoSeleccionado(estado: EstadoReserva): boolean {
    return this.filtros.estados.includes(estado);
  }

  estaCanalSeleccionado(canal: CanalReserva): boolean {
    return this.filtros.canales.includes(canal);
  }

  seleccionarTipoUnidad(tipo: TipoUnidad): void {
    this.filtros.tipoUnidad = this.filtros.tipoUnidad === tipo ? '' : tipo;
  }

  esTipoUnidadSeleccionado(tipo: TipoUnidad): boolean {
    return this.filtros.tipoUnidad === tipo;
  }

  onToggleFiltrosAvanzados(event: Event): void {
    const details = event.target as HTMLDetailsElement | null;
    this.filtrosAvanzadosAbiertos = !!details?.open;
  }

  get filtrosAvanzadosResumen(): ResumenFiltroItem[] {
    const items: ResumenFiltroItem[] = [];

    if (this.filtros.tipoUnidad) {
      items.push({
        clave: 'tipoUnidad',
        texto: `Tipo unidad: ${this.formatearEtiqueta(this.filtros.tipoUnidad)}`,
      });
    }

    if (this.filtros.estados.length) {
      const estados = this.filtros.estados.map((estado) => this.formatearEtiqueta(estado)).join(', ');
      items.push({ clave: 'estados', texto: `Estado: ${estados}` });
    }

    if (this.filtros.canales.length) {
      const canales = this.filtros.canales.map((canal) => this.formatearCanal(canal)).join(', ');
      items.push({ clave: 'canales', texto: `Canal: ${canales}` });
    }

    const creacion = this.construirResumenRango(
      'Creacion',
      this.filtros.fechaCreacionDesde,
      this.filtros.fechaCreacionHasta
    );
    if (creacion) {
      items.push({ clave: 'creacion', texto: creacion });
    }

    const entrada = this.construirResumenRango(
      'Entrada',
      this.filtros.entradaDesde,
      this.filtros.entradaHasta
    );
    if (entrada) {
      items.push({ clave: 'entrada', texto: entrada });
    }

    const salida = this.construirResumenRango('Salida', this.filtros.salidaDesde, this.filtros.salidaHasta);
    if (salida) {
      items.push({ clave: 'salida', texto: salida });
    }

    return items;
  }

  quitarFiltroAvanzado(clave: ResumenFiltroClave): void {
    switch (clave) {
      case 'tipoUnidad':
        this.filtros.tipoUnidad = '';
        break;
      case 'estados':
        this.filtros.estados = [];
        break;
      case 'canales':
        this.filtros.canales = [];
        break;
      case 'creacion':
        this.filtros.fechaCreacionDesde = '';
        this.filtros.fechaCreacionHasta = '';
        this.rangoRapidoSeleccionado.creacion = null;
        break;
      case 'entrada':
        this.filtros.entradaDesde = '';
        this.filtros.entradaHasta = '';
        this.rangoRapidoSeleccionado.entrada = null;
        break;
      case 'salida':
        this.filtros.salidaDesde = '';
        this.filtros.salidaHasta = '';
        this.rangoRapidoSeleccionado.salida = null;
        break;
      default:
        return;
    }

    this.aplicarFiltros();
  }

  trackByResumenFiltro(_: number, item: ResumenFiltroItem): ResumenFiltroClave {
    return item.clave;
  }

  cambiarPageSize(size: number): void {
    this.size = size;
    this.page = 0;
    this.cargarReservas();
  }

  irANuevaReserva(): void {
    this.router.navigate(['/reservas/nueva'], {
      state: { returnTo: this.router.url },
    });
  }

  aplicarRangoRapido(rango: RangoRapido, campo: RangoRapidoCampo = 'rangoGeneral'): void {
    if (this.rangoRapidoSeleccionado[campo] === rango) {
      this.limpiarRangoCampo(campo);
      this.rangoRapidoSeleccionado[campo] = null;
      return;
    }

    const ahora = new Date();
    let desde: Date;
    let hasta: Date;

    switch (rango) {
      case 'HOY':
        desde = this.inicioDelDia(ahora);
        hasta = this.finDelDia(ahora);
        break;
      case 'MANANA': {
        const manana = new Date(ahora);
        manana.setDate(manana.getDate() + 1);
        desde = this.inicioDelDia(manana);
        hasta = this.finDelDia(manana);
        break;
      }
      case 'ESTA_SEMANA': {
        const inicioSemana = new Date(ahora);
        const diaSemana = inicioSemana.getDay();
        const desplazamientoLunes = diaSemana === 0 ? -6 : 1 - diaSemana;
        inicioSemana.setDate(inicioSemana.getDate() + desplazamientoLunes);
        const finSemana = new Date(inicioSemana);
        finSemana.setDate(finSemana.getDate() + 6);
        desde = this.inicioDelDia(inicioSemana);
        hasta = this.finDelDia(finSemana);
        break;
      }
      case 'PROXIMOS_7_DIAS': {
        const fin = new Date(ahora);
        fin.setDate(fin.getDate() + 6);
        desde = this.inicioDelDia(ahora);
        hasta = this.finDelDia(fin);
        break;
      }
      case 'ESTE_MES':
      default: {
        const inicioMes = new Date(ahora.getFullYear(), ahora.getMonth(), 1);
        const finMes = new Date(ahora.getFullYear(), ahora.getMonth() + 1, 0);
        desde = this.inicioDelDia(inicioMes);
        hasta = this.finDelDia(finMes);
        break;
      }
    }

    const desdeTexto = this.formatearFechaInput(desde);
    const hastaTexto = this.formatearFechaInput(hasta);

    if (campo === 'rangoGeneral') {
      this.filtros.rangoGeneralDesde = desdeTexto;
      this.filtros.rangoGeneralHasta = hastaTexto;
    } else if (campo === 'creacion') {
      this.filtros.fechaCreacionDesde = desdeTexto;
      this.filtros.fechaCreacionHasta = hastaTexto;
    } else if (campo === 'entrada') {
      this.filtros.entradaDesde = desdeTexto;
      this.filtros.entradaHasta = hastaTexto;
    } else {
      this.filtros.salidaDesde = desdeTexto;
      this.filtros.salidaHasta = hastaTexto;
    }

    this.rangoRapidoSeleccionado[campo] = rango;
  }

  esRangoRapidoSeleccionado(campo: RangoRapidoCampo, rango: RangoRapido): boolean {
    return this.rangoRapidoSeleccionado[campo] === rango;
  }

  limpiarSeleccionRangoRapido(campo: RangoRapidoCampo): void {
    this.rangoRapidoSeleccionado[campo] = null;
  }

  private limpiarRangoCampo(campo: RangoRapidoCampo): void {
    if (campo === 'rangoGeneral') {
      this.filtros.rangoGeneralDesde = '';
      this.filtros.rangoGeneralHasta = '';
      return;
    }

    if (campo === 'creacion') {
      this.filtros.fechaCreacionDesde = '';
      this.filtros.fechaCreacionHasta = '';
      return;
    }

    if (campo === 'entrada') {
      this.filtros.entradaDesde = '';
      this.filtros.entradaHasta = '';
      return;
    }

    this.filtros.salidaDesde = '';
    this.filtros.salidaHasta = '';
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

  darEntradaReservaSeleccionada(): void {
    const reserva = this.obtenerReservaSeleccionada();
    if (!reserva) {
      return;
    }

    this.darEntradaReserva(reserva);
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

  puedeDarEntradaReservaSeleccionada(): boolean {
    const reserva = this.obtenerReservaSeleccionada();
    return !!reserva && this.puedeDarEntradaReserva(reserva);
  }

  estaEliminandoReservaSeleccionada(): boolean {
    const reserva = this.obtenerReservaSeleccionada();
    return !!reserva && this.eliminandoReservaId === reserva.id;
  }

  obtenerTooltipEditarSeleccionada(): string | null {
    return this.obtenerTooltipAccionSeleccionada('editar');
  }

  obtenerTooltipEliminarSeleccionada(): string | null {
    return this.obtenerTooltipAccionSeleccionada('eliminar');
  }

  obtenerTooltipDarEntradaSeleccionada(): string | null {
    return this.obtenerTooltipAccionSeleccionada('dar entrada');
  }

  formatearEtiqueta(valor: string | null | undefined): string {
    return (valor ?? '')
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
  }

  formatearCanal(canal: CanalReserva | null | undefined): string {
    if (!canal) {
      return '';
    }

    const canalSinPrefijo = canal.startsWith('PLATAFORMA_')
      ? canal.replace('PLATAFORMA_', '')
      : canal;

    return this.formatearEtiqueta(canalSinPrefijo);
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

  obtenerTooltipPagosModificados(reserva: ReservaTablaItem): string {
    const cantidad = reserva.cantidadPagosModificadosOEliminados ?? 0;
    const etiqueta = cantidad === 1 ? 'pago modificado' : 'pagos modificados';
    return `Tiene ${cantidad} ${etiqueta}`;
  }

  tienePagoAsociado(reserva: ReservaTablaItem): boolean {
    const totalPagoReserva = reserva.totalPagoReserva ?? 0;
    if (totalPagoReserva > 0) {
      return true;
    }

    return this.tienePagosModificados(reserva);
  }

  irAPagosAnticipoReserva(reserva: ReservaTablaItem): void {
    if (!this.tienePagoAsociado(reserva)) {
      this.accionError = 'La reserva no tiene pago asociado para consultar.';
      this.accionExito = '';
      return;
    }

    const codigoReserva = reserva.codigoReserva?.trim() ?? '';
    if (!codigoReserva) {
      this.accionError = 'La reserva no tiene codigo de reserva asociado para consultar pagos.';
      this.accionExito = '';
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.router.navigate(['/pagos'], {
      queryParams: {
        codigoReserva,
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

  irATablaClientes(reserva: ReservaTablaItem): void {
    const numeroDocumento = reserva.numeroDocumentoCliente?.trim() ?? '';
    if (!numeroDocumento) {
      this.accionError = 'La reserva no tiene documento de cliente para buscar.';
      this.accionExito = '';
      return;
    }

    this.accionError = '';
    this.accionExito = '';

    this.router.navigate(['/ocupantes/tabla-clientes'], {
      queryParams: {
        numeroDocumento,
      },
    });
  }

  get indiceInicio(): number {
    return this.totalElements === 0 ? 0 : this.page * this.size + 1;
  }

  get indiceFin(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }

  puedeGestionarReserva(reserva: ReservaTablaItem): boolean {
    return this.estaReservaEnEstadoEditable(reserva);
  }

  puedeDarEntradaReserva(reserva: ReservaTablaItem): boolean {
    return this.estaReservaEnEstadoEditable(reserva);
  }

  darEntradaReserva(reserva: ReservaTablaItem): void {
    if (!this.puedeDarEntradaReserva(reserva)) {
      this.accionError = this.obtenerMensajeEstadoInvalido('dar entrada');
      this.accionExito = '';
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.router.navigate(['/estancias/nueva'], {
      state: {
        returnTo: this.router.url,
        flujo: 'INGRESO',
        idReserva: reserva.id,
        idCliente: reserva.idCliente ?? undefined,
        codigoReserva: reserva.codigoReserva ?? '',
        nombreCliente: reserva.nombreCliente ?? '',
        codigo: reserva.codigoUnidad,
        tipo: reserva.tipoUnidad,
        entrada: reserva.entradaEstimada,
        salida: reserva.salidaEstimada,
        numeroPersonasReserva: reserva.numeroPersonas,
      },
    });
  }

  editarReserva(reserva: ReservaTablaItem): void {
    if (!this.puedeGestionarReserva(reserva)) {
      this.accionError = this.obtenerMensajeEstadoInvalido('editar');
      this.accionExito = '';
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.router.navigate(['/reservas/nueva'], {
      state: {
        editMode: true,
        returnTo: this.router.url,
        reserva: {
          id: reserva.id,
          codigo: reserva.codigoUnidad,
          tipoUnidad: reserva.tipoUnidad,
          idOcupante: reserva.idCliente ?? undefined,
          nombreCliente: reserva.nombreCliente ?? '',
          numeroPersonas: reserva.numeroPersonas,
          canalReserva: reserva.canalReserva,
          entradaEstimada: reserva.entradaEstimada,
          salidaEstimada: reserva.salidaEstimada,
        },
      },
    });
  }

  eliminarReserva(reserva: ReservaTablaItem): void {
    this.abrirModalEliminarReserva(reserva);
  }

  cerrarModalEliminarReserva(): void {
    if (this.eliminandoReservaId !== null) {
      return;
    }
    this.reservaPendienteEliminacion = null;
  }

  confirmarEliminarReserva(): void {
    const reserva = this.reservaPendienteEliminacion;
    if (!reserva) {
      return;
    }

    this.eliminandoReservaId = reserva.id;
    this.accionError = '';
    this.accionExito = '';

    this.reservaService.eliminarReserva(reserva.id).subscribe({
      next: () => {
        this.eliminandoReservaId = null;
        this.reservaPendienteEliminacion = null;
        this.accionExito = 'Reserva cancelada correctamente.';
        this.cargarReservas();
      },
      error: (errorResponse: unknown) => {
        this.eliminandoReservaId = null;
        this.reservaPendienteEliminacion = null;
        this.accionError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible eliminar la reserva.'
        );
        this.accionExito = '';
      },
    });
  }

  private abrirModalEliminarReserva(reserva: ReservaTablaItem): void {
    if (!this.puedeGestionarReserva(reserva)) {
      this.accionError = this.obtenerMensajeEstadoInvalido('eliminar');
      this.accionExito = '';
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.reservaPendienteEliminacion = reserva;
  }

  private cargarReservas(): void {
    this.recargaTabla$.next();
  }

  private ejecutarCargaReservas(): Observable<PageResponse<ReservaTablaItem> | null> {
    this.cargando = true;
    this.error = '';

    const filtros: ReservaTablaFiltros = {
      ...this.filtros,
      rangoGeneralDesde: this.normalizarFechaHora(this.filtros.rangoGeneralDesde, 'desde'),
      rangoGeneralHasta: this.normalizarFechaHora(this.filtros.rangoGeneralHasta, 'hasta'),
      fechaCreacionDesde: this.normalizarFechaHora(this.filtros.fechaCreacionDesde, 'desde'),
      fechaCreacionHasta: this.normalizarFechaHora(this.filtros.fechaCreacionHasta, 'hasta'),
      entradaDesde: this.normalizarFechaHora(this.filtros.entradaDesde, 'desde'),
      entradaHasta: this.normalizarFechaHora(this.filtros.entradaHasta, 'hasta'),
      salidaDesde: this.normalizarFechaHora(this.filtros.salidaDesde, 'desde'),
      salidaHasta: this.normalizarFechaHora(this.filtros.salidaHasta, 'hasta'),
    };

    const sort = ['fechaCreacion,desc'];

    return this.reservaService.obtenerTabla(filtros, this.page, this.size, sort).pipe(
      tap((response: PageResponse<ReservaTablaItem>) => {
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
      }),
      catchError((errorResponse: unknown) => {
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar la tabla de reservas.'
        );
        this.reservas = [];
        this.reservaSeleccionadaId = null;
        this.totalElements = 0;
        this.totalPages = 0;
        return of(null);
      }),
      finalize(() => {
        this.cargando = false;
      })
    );
  }

  private obtenerTooltipAccionSeleccionada(accion: AccionReserva): string | null {
    const reserva = this.obtenerReservaSeleccionada();
    if (!reserva) {
      return `Selecciona una reserva para ${accion}.`;
    }

    if (this.eliminandoReservaId === reserva.id) {
      return 'Se esta eliminando la reserva seleccionada.';
    }

    if (!this.puedeAccionarReserva(reserva, accion)) {
      return this.obtenerMensajeEstadoInvalido(accion);
    }

    return null;
  }

  private puedeAccionarReserva(reserva: ReservaTablaItem, accion: AccionReserva): boolean {
    if (accion === 'dar entrada') {
      return this.puedeDarEntradaReserva(reserva);
    }

    return this.puedeGestionarReserva(reserva);
  }

  private estaReservaEnEstadoEditable(reserva: ReservaTablaItem): boolean {
    return reserva.estadoReserva === 'CONFIRMADA' || reserva.estadoReserva === 'EXPIRADA';
  }

  private obtenerMensajeEstadoInvalido(accion: AccionReserva): string {
    if (accion === 'dar entrada') {
      return 'Solo se puede dar entrada a reservas confirmadas o expiradas.';
    }

    return `Solo se pueden ${accion} reservas confirmadas o expiradas.`;
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
      rangoGeneralDesde: '',
      rangoGeneralHasta: '',
      fechaCreacionDesde: '',
      fechaCreacionHasta: '',
      entradaDesde: '',
      entradaHasta: '',
      salidaDesde: '',
      salidaHasta: '',
    };
  }

  private normalizarFechaHora(fecha: string, limite: 'desde' | 'hasta'): string {
    const valor = fecha.trim();
    if (!valor) {
      return '';
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) {
      return limite === 'desde' ? `${valor}T00:00:00` : `${valor}T23:59:59`;
    }

    return valor.length === 16 ? `${valor}:00` : valor;
  }

  private inicioDelDia(fecha: Date): Date {
    return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate(), 0, 0, 0, 0);
  }

  private finDelDia(fecha: Date): Date {
    return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate(), 23, 59, 0, 0);
  }

  private formatearFechaInput(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = `${fecha.getMonth() + 1}`.padStart(2, '0');
    const dia = `${fecha.getDate()}`.padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }

  private construirResumenRango(etiqueta: string, desde: string, hasta: string): string | null {
    const desdeTexto = this.formatearFechaResumen(desde);
    const hastaTexto = this.formatearFechaResumen(hasta);

    if (!desdeTexto && !hastaTexto) {
      return null;
    }

    if (desdeTexto && hastaTexto) {
      return `${etiqueta}: ${desdeTexto} -> ${hastaTexto}`;
    }

    return `${etiqueta}: ${desdeTexto || hastaTexto}`;
  }

  private formatearFechaResumen(valor: string): string {
    const fecha = valor?.trim();
    if (!fecha) {
      return '';
    }

    const soloFecha = fecha.slice(0, 10);
    const [anio, mes, dia] = soloFecha.split('-');
    if (!anio || !mes || !dia) {
      return fecha;
    }

    return `${dia}/${mes}/${anio}`;
  }

  obtenerReservaSeleccionada(): ReservaTablaItem | null {
    if (this.reservaSeleccionadaId === null) {
      return null;
    }

    return this.reservas.find((reserva) => reserva.id === this.reservaSeleccionadaId) ?? null;
  }

}
