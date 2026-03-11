import { CommonModule, Location } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FlatpickrModule } from 'angularx-flatpickr';

import { HabitacionDTO } from '../../models/habitacion.model';
import { UnidadDTO } from '../../models/unidad.model';
import { EstanciaDTO } from '../../models/estancia-detalle.model';
import { OcupanteDTO } from '../../models/ocupante.model';
import { EstadoOperativo, MedioPago, Piso, TipoUnidad } from '../../models/enums';
import { PagoNuevoRequest } from '../../models/pago.model';
import { ReservaDTO } from '../../models/reserva.model';
import { HabitacionService } from '../../services/habitacion.service';
import { UnidadService } from '../../services/unidad.service';
import { EstanciaService } from '../../services/estancia.service';
import { PagoService } from '../../services/pago.service';
import { ReservaService } from '../../services/reserva.service';
import { catchError, map, of, switchMap } from 'rxjs';
import { formatDateOnly, formatDateTimeNoSeconds, getCurrentDateInput } from '../../core/utils/date-time.util';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import { parseJsonSafe, parsePositiveId } from '../../core/utils/json.util';
import {
  getPreviousNavigationUrl,
  readReturnToFromState,
} from '../../core/utils/navigation-return.util';
import { FeedbackToastService } from '../../core/services/feedback-toast.service';

interface InformacionAdicionalUnidadPanel {
  ESTANCIA?: {
    idEstancia?: number | string;
    idReserva?: number | string;
  };
  RESERVA?: {
    id?: number | string;
    idReserva?: number | string;
  };
}

interface ContextoOperacionPanel {
  idEstancia: number | null;
  idReserva: number | null;
}

@Component({
  selector: 'app-recepcion-panel',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, FlatpickrModule],
  templateUrl: './recepcion-panel.component.html',
  styleUrl: './recepcion-panel.component.css',
})
export class RecepcionPanelComponent implements OnInit {
  unidad: UnidadDTO | null = null;
  cargando = true;
  error = '';

  estanciaDetalle: EstanciaDTO | null = null;
  reservaDetalle: ReservaDTO | null = null;
  estanciaCargando = false;
  estanciaError = '';
  eliminandoEstancia = false;
  mostrarModalEliminarEstancia = false;
  eliminarError = '';
  mostrarModalPago = false;
  guardandoPago = false;
  procesandoReserva = false;
  mostrarModalEliminarReserva = false;
  eliminandoPagoId: number | null = null;
  pagoPendienteEliminacionId: number | null = null;
  pagoModalError = '';
  montoPago: number | null = null;
  medioPago: MedioPago = 'EFECTIVO';
  fechaPago = '';
  totalCalculado: number | null = null;
  calculandoPago = false;
  calculoError = '';

  readonly mediosPago: MedioPago[] = [
    'EFECTIVO',
    'TARJETA_CREDITO',
    'TARJETA_DEBITO',
    'TRANSFERENCIA_BANCARIA',
    'PLATAFORMA',
  ];

  private codigo = '';
  private tipo: TipoUnidad | '' = '';
  private rutaRetorno = '/recepcion';
  private urlOrigen: string | null = null;
  private retornoExplicito = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly location: Location,
    private readonly unidadService: UnidadService,
    private readonly habitacionService: HabitacionService,
    private readonly estanciaService: EstanciaService,
    private readonly pagoService: PagoService,
    private readonly reservaService: ReservaService,
    private readonly feedbackToastService: FeedbackToastService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.actualizarRutaRetorno();
      this.codigo = (params['codigo'] as string | undefined)?.trim() ?? '';
      this.tipo = (params['tipo'] as TipoUnidad | undefined) ?? '';
      this.cargarUnidad();
    });
  }

  volver(): void {
    if (this.retornoExplicito) {
      this.router.navigateByUrl(this.rutaRetorno);
      return;
    }

    if (this.urlOrigen) {
      this.location.back();
      return;
    }

    this.router.navigateByUrl(this.rutaRetorno);
  }

  estadoClass(estado: string) {
    switch (estado) {
      case 'DISPONIBLE':
        return 'estado--disponible';
      case 'PARCIALMENTE':
        return 'estado--parcial';
      case 'OCUPADO':
        return 'estado--ocupado';
      case 'RESERVADO':
        return 'estado--reservado';
      default:
        return '';
    }
  }

  esNoDisponible(estado: string): boolean {
    return estado === 'OCUPADO' || estado === 'RESERVADO' || estado === 'PARCIALMENTE';
  }

  resumenOcupacion(unidad: UnidadDTO): string {
    const ocupadas = unidad.habitaciones.filter((habitacion) => habitacion.estado !== 'DISPONIBLE')
      .length;
    return `${ocupadas} de ${unidad.habitaciones.length} habitaciones`;
  }

  get esVistaHabitacion(): boolean {
    return this.tipo === 'HABITACION';
  }

  textoEstadoHabitacion(habitacion: HabitacionDTO): string {
    if (this.unidad?.tipo !== 'APARTAMENTO' || !this.esModoCompletoPadre()) {
      return habitacion.estado;
    }

    if (habitacion.estado === 'OCUPADO') {
      return 'OCUPACION COMPLETO';
    }

    if (habitacion.estado === 'RESERVADO') {
      return 'RESERVADO COMPLETO';
    }

    return habitacion.estado;
  }

  abrirHabitacion(habitacion: HabitacionDTO): void {
    this.router.navigate(['/recepcion/panel'], {
      queryParams: {
        codigo: habitacion.codigo,
        tipo: 'HABITACION',
      },
      state: {
        returnTo: this.router.url,
      },
    });
  }

  consultarEstanciasUnidad(): void {
    if (!this.unidad?.codigo) {
      return;
    }

    this.router.navigate(['/estancias'], {
      queryParams: { codigoUnidad: this.unidad.codigo },
      state: { returnTo: this.router.url },
    });
  }

  consultarReservasUnidad(): void {
    if (!this.unidad?.codigo) {
      return;
    }

    this.router.navigate(['/reservas'], {
      queryParams: { codigoUnidad: this.unidad.codigo },
      state: { returnTo: this.router.url },
    });
  }

  irAClientesPorReserva(reserva: ReservaDTO): void {
    const idClienteReserva = reserva.cliente?.id ?? null;
    const clienteOcupante = this.obtenerOcupantesEstancia(this.estanciaDetalle).find(
      (ocupante) => ocupante.tipoOcupante === 'CLIENTE' && ocupante.id === idClienteReserva
    );

    const nombreCliente = this.obtenerNombreClienteReserva(reserva);

    const params = this.construirFiltrosCliente(
      clienteOcupante?.nombres ?? nombreCliente,
      clienteOcupante?.apellidos ?? '',
      clienteOcupante?.tipoDocumento,
      clienteOcupante?.numeroDocumento
    );

    this.router.navigate(['/ocupantes/tabla-clientes'], {
      queryParams: params,
      state: { returnTo: this.router.url },
    });
  }

  irAClientesPorOcupante(ocupante: OcupanteDTO): void {
    const params = this.construirFiltrosCliente(
      ocupante.nombres,
      ocupante.apellidos,
      ocupante.tipoDocumento,
      ocupante.numeroDocumento
    );

    this.router.navigate(['/ocupantes/tabla-clientes'], {
      queryParams: params,
      state: { returnTo: this.router.url },
    });
  }

  verEstancia(unidad: UnidadDTO): void {
    this.estanciaCargando = true;
    this.estanciaError = '';
    this.estanciaDetalle = null;
    this.reservaDetalle = null;
    this.eliminarError = '';
    const contexto = this.obtenerContextoOperacionDesdeUnidad(unidad);
    const idEstancia = contexto.idEstancia;
    const idReserva = contexto.idReserva;

    if (!idEstancia && !idReserva) {
      this.estanciaCargando = false;
      return;
    }

    if (!idEstancia && idReserva) {
      this.reservaService.obtenerReservaPorId(idReserva).subscribe({
        next: (reserva) => {
          this.estanciaDetalle = null;
          this.reservaDetalle = reserva;
          this.estanciaCargando = false;
        },
        error: (errorResponse: unknown) => {
          this.estanciaError = extractBackendErrorMessage(
            errorResponse,
            'No fue posible cargar la reserva asociada.'
          );
          this.estanciaCargando = false;
        },
      });
      return;
    }

    this.estanciaService
      .obtenerEstanciaPorId(idEstancia as number)
      .pipe(
        switchMap((estancia) => {
          if (this.esEstanciaSinInformacion(estancia)) {
            if (!idReserva) {
              return of({ estancia: null as EstanciaDTO | null, reserva: null as ReservaDTO | null });
            }

            return this.reservaService.obtenerReservaPorId(idReserva).pipe(
              map((reserva) => ({ estancia: null as EstanciaDTO | null, reserva })),
              catchError(() => of({ estancia: null as EstanciaDTO | null, reserva: null as ReservaDTO | null }))
            );
          }

          if (!idReserva) {
            return of({ estancia, reserva: null as ReservaDTO | null });
          }

          return this.reservaService.obtenerReservaPorId(idReserva).pipe(
            map((reserva) => ({ estancia, reserva })),
            catchError(() => of({ estancia, reserva: null as ReservaDTO | null }))
          );
        })
      )
      .subscribe({
        next: (resultado) => {
          if (!resultado.estancia) {
            this.estanciaDetalle = null;
            this.reservaDetalle = null;
            this.estanciaCargando = false;
            return;
          }

          this.estanciaDetalle = resultado.estancia;
          this.reservaDetalle = resultado.reserva;
          this.estanciaCargando = false;
        },
        error: (errorResponse: unknown) => {
          this.estanciaError = extractBackendErrorMessage(
            errorResponse,
            'No hay estancia activa para esta unidad.'
          );
          this.estanciaCargando = false;
        },
      });
  }

  private obtenerContextoOperacionDesdeUnidad(unidad: UnidadDTO): ContextoOperacionPanel {
    const usarReservaDesdeNodoReserva = unidad.estado === 'RESERVADO';
    const fuentes =
      this.tipo === 'HABITACION'
        ? [
            ...unidad.habitaciones.map((habitacion) => habitacion.informacionAdicional),
            unidad.informacionAdicional,
          ]
        : [unidad.informacionAdicional];

    for (const informacion of fuentes) {
      const info = this.parsearInformacionAdicional(informacion);
      const idEstancia = parsePositiveId(info?.ESTANCIA?.idEstancia);
      const idReserva = usarReservaDesdeNodoReserva
        ? parsePositiveId(info?.RESERVA?.idReserva ?? info?.RESERVA?.id)
        : parsePositiveId(info?.ESTANCIA?.idReserva);

      if (idEstancia || idReserva) {
        return {
          idEstancia,
          idReserva,
        };
      }
    }

    return { idEstancia: null, idReserva: null };
  }

  private construirFiltrosCliente(
    nombre: string,
    apellido: string,
    tipoDocumento?: string,
    numeroDocumento?: string
  ): Record<string, string> {
    const params: Record<string, string> = {};
    const numero = (numeroDocumento ?? '').trim();

    if (numero) {
      params['numeroDocumento'] = numero;
      if (tipoDocumento?.trim()) {
        params['tipoDocumento'] = tipoDocumento.trim();
      }
      return params;
    }

    const nombreNormalizado = (nombre ?? '').trim();
    const apellidoNormalizado = (apellido ?? '').trim();

    if (nombreNormalizado) {
      const partes = nombreNormalizado.split(/\s+/);
      params['nombre'] = partes.shift() ?? nombreNormalizado;
      if (!apellidoNormalizado && partes.length) {
        params['apellido'] = partes.join(' ');
      }
    }

    if (apellidoNormalizado) {
      params['apellido'] = apellidoNormalizado;
    }

    return params;
  }

  private parsearInformacionAdicional(
    informacionAdicional: string | null | undefined
  ): InformacionAdicionalUnidadPanel | null {
    return parseJsonSafe<InformacionAdicionalUnidadPanel>(informacionAdicional);
  }

  eliminarEstancia(): void {
    if (!this.estanciaDetalle || this.eliminandoEstancia) {
      return;
    }

    this.mostrarModalEliminarEstancia = true;
  }

  cerrarModalEliminarEstancia(): void {
    if (this.eliminandoEstancia) {
      return;
    }

    this.mostrarModalEliminarEstancia = false;
  }

  confirmarEliminarEstancia(): void {
    if (!this.estanciaDetalle || this.eliminandoEstancia) {
      return;
    }

    this.eliminandoEstancia = true;
    this.eliminarError = '';

    this.estanciaService.eliminarEstancia(this.estanciaDetalle.id).subscribe({
      next: () => {
        this.estanciaDetalle = null;
        this.reservaDetalle = null;
        this.mostrarModalEliminarEstancia = false;
        this.eliminandoEstancia = false;
        this.router.navigate(['/recepcion']);
      },
      error: (errorResponse: unknown) => {
        this.eliminandoEstancia = false;
        this.mostrarModalEliminarEstancia = false;
        this.eliminarError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible eliminar la estancia.'
        );
      },
    });
  }

  darSalida(): void {
    if (!this.estanciaDetalle || !this.unidad) {
      return;
    }

    const ocupantes = this.obtenerOcupantesEstancia(this.estanciaDetalle);
    const cliente = ocupantes.find((ocupante) => ocupante.tipoOcupante === 'CLIENTE');
    const nombreCliente = [cliente?.nombres, cliente?.apellidos]
      .filter((valor) => Boolean(valor?.trim()))
      .join(' ');

    this.router.navigate(['/estancias/salida'], {
      state: {
        returnTo: this.router.url,
        estanciaId: this.estanciaDetalle.id,
        codigo: this.unidad.codigo,
        tipo: this.unidad.tipo,
        entrada: this.estanciaDetalle.entradaReal,
        salida: this.estanciaDetalle.salidaEstimada,
        numeroPersonas: ocupantes.length,
        nombreCliente,
      },
    });
  }

  registrarEntradaReserva(): void {
    if (!this.unidad || !this.reservaDetalle || !this.puedeRegistrarEntradaReserva) {
      return;
    }

    this.router.navigate(['/estancias/nueva'], {
      state: {
        returnTo: this.router.url,
        flujo: 'INGRESO',
        idReserva: this.reservaDetalle.id,
        idEstancia: this.reservaDetalle.idEstancia ?? undefined,
        idCliente: this.reservaDetalle.cliente?.id ?? undefined,
        codigoReserva: this.reservaDetalle.codigoReserva ?? '',
        nombreCliente: this.obtenerNombreClienteReserva(this.reservaDetalle),
        codigo: this.unidad.codigo,
        tipo: this.unidad.tipo,
        entrada: this.reservaDetalle.entradaEstimada,
        salida: this.reservaDetalle.salidaEstimada,
        numeroPersonasReserva: this.reservaDetalle.numeroPersonas,
      },
    });
  }

  editarReserva(): void {
    if (!this.unidad || !this.reservaDetalle) {
      return;
    }

    this.router.navigate(['/reservas/nueva'], {
      state: {
        editMode: true,
        returnTo: this.router.url,
        reserva: {
          id: this.reservaDetalle.id,
          codigo: this.unidad.codigo,
          tipoUnidad: this.unidad.tipo,
          idOcupante: this.reservaDetalle.cliente?.id ?? undefined,
          nombreCliente: this.obtenerNombreClienteReserva(this.reservaDetalle),
          numeroPersonas: this.reservaDetalle.numeroPersonas,
          canalReserva: this.reservaDetalle.canalReserva,
          entradaEstimada: this.reservaDetalle.entradaEstimada,
          salidaEstimada: this.reservaDetalle.salidaEstimada,
        },
      },
    });
  }

  eliminarReserva(): void {
    if (!this.unidad || !this.reservaDetalle || this.procesandoReserva) {
      return;
    }

    this.mostrarModalEliminarReserva = true;
  }

  cerrarModalEliminarReserva(): void {
    if (this.procesandoReserva) {
      return;
    }

    this.mostrarModalEliminarReserva = false;
  }

  confirmarEliminarReserva(): void {
    if (!this.unidad || !this.reservaDetalle || this.procesandoReserva) {
      return;
    }

    this.procesandoReserva = true;
    this.reservaService.eliminarReserva(this.reservaDetalle.id).subscribe({
      next: () => {
        this.feedbackToastService.showSuccess('Reserva eliminada con exito.');
        this.procesandoReserva = false;
        this.mostrarModalEliminarReserva = false;
        this.cargarUnidad();
      },
      error: (errorResponse: unknown) => {
        this.procesandoReserva = false;
        this.mostrarModalEliminarReserva = false;
        this.estanciaError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible eliminar la reserva.'
        );
      },
    });
  }

  abrirModalPago(): void {
    if (!this.estanciaDetalle) {
      return;
    }

    this.mostrarModalPago = true;
    this.guardandoPago = false;
    this.pagoModalError = '';
    this.montoPago = null;
    this.medioPago = 'EFECTIVO';
    this.fechaPago = this.obtenerFechaActual();
    this.recalcularPago();
  }

  cerrarModalPago(): void {
    if (this.guardandoPago) {
      return;
    }

    this.mostrarModalPago = false;
    this.pagoModalError = '';
    this.totalCalculado = null;
    this.calculoError = '';
  }

  guardarPagoModal(): void {
    if (!this.estanciaDetalle) {
      this.pagoModalError = 'No hay una estancia activa para registrar el pago.';
      return;
    }

    if (
      this.montoPago === null ||
      Number.isNaN(Number(this.montoPago)) ||
      Number(this.montoPago) <= 0 ||
      !this.fechaPago
    ) {
      this.pagoModalError = 'Completa los campos de pago obligatorios.';
      return;
    }

    const request: PagoNuevoRequest = {
      tipoPago: 'ANTICIPO_ESTANCIA',
      monto: Number(this.montoPago),
      medioPago: this.medioPago,
      fecha: this.normalizarFechaPago(this.fechaPago),
      estado: 'COMPLETADO',
    };

    this.guardandoPago = true;
    this.pagoModalError = '';

    this.pagoService.crearPago(this.estanciaDetalle.id, request).subscribe({
      next: () => {
        this.guardandoPago = false;
        this.mostrarModalPago = false;
        this.feedbackToastService.showSuccess('Pago registrado con exito.');
        if (this.unidad) {
          this.verEstancia(this.unidad);
        }
      },
      error: (errorResponse: unknown) => {
        this.guardandoPago = false;
        this.pagoModalError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible registrar el pago.'
        );
      },
    });
  }

  mantenerModal(event: MouseEvent): void {
    event.stopPropagation();
  }

  eliminarPago(idPago: number): void {
    if (this.eliminandoPagoId === idPago) {
      return;
    }

    this.pagoPendienteEliminacionId = idPago;
  }

  cerrarModalEliminarPago(): void {
    if (this.eliminandoPagoId !== null) {
      return;
    }

    this.pagoPendienteEliminacionId = null;
  }

  confirmarEliminarPago(): void {
    if (!this.unidad || this.pagoPendienteEliminacionId === null) {
      return;
    }

    const idPago = this.pagoPendienteEliminacionId;
    this.eliminandoPagoId = idPago;
    this.pagoModalError = '';

    this.pagoService.eliminarPago(idPago).subscribe({
      next: () => {
        this.eliminandoPagoId = null;
        this.pagoPendienteEliminacionId = null;
        this.feedbackToastService.showSuccess('Pago eliminado con exito.');
        this.verEstancia(this.unidad as UnidadDTO);
      },
      error: (errorResponse: unknown) => {
        this.eliminandoPagoId = null;
        this.pagoPendienteEliminacionId = null;
        this.estanciaError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible eliminar el pago.'
        );
      },
    });
  }

  recalcularPago(): void {
    if (!this.unidad || !this.estanciaDetalle) {
      this.totalCalculado = null;
      this.calculoError = '';
      return;
    }

    const numeroPersonas = this.reservaDetalle?.numeroPersonas ?? this.obtenerOcupantesEstancia(this.estanciaDetalle).length;
    const fechaEntrada = this.reservaDetalle?.entradaEstimada ?? this.estanciaDetalle.entradaReal;
    const fechaSalida = this.reservaDetalle?.salidaEstimada ?? this.estanciaDetalle.salidaEstimada;

    this.calculoError = '';
    this.totalCalculado = null;

    if (!numeroPersonas || numeroPersonas < 1) {
      this.calculoError = 'El numero de personas debe ser al menos 1.';
      return;
    }

    this.calculandoPago = true;

    this.pagoService
      .calcularTotal({
        tipoUnidad: this.unidad.tipo,
        numeroPersonas,
        fechaEntrada: this.normalizarFechaInicio(fechaEntrada),
        fechaSalida: this.normalizarFechaInicio(fechaSalida),
        idEstancia: this.estanciaDetalle.id,
      })
      .subscribe({
        next: (total) => {
          this.totalCalculado = total;
          this.montoPago = total;
          this.calculandoPago = false;
        },
        error: (errorResponse: unknown) => {
          this.calculoError = extractBackendErrorMessage(
            errorResponse,
            'No fue posible calcular el pago.'
          );
          this.calculandoPago = false;
        },
      });
  }

  formatearFecha(fecha: string): string {
    const fechaObj = new Date(fecha);
    return Number.isNaN(fechaObj.getTime()) ? fecha : fechaObj.toLocaleString('es-CO');
  }

  formatearFechaSinSegundos(fecha: string): string {
    return formatDateTimeNoSeconds(fecha);
  }

  formatearCop(valor: number): string {
    const texto = new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      currencyDisplay: 'code',
      maximumFractionDigits: 0,
    }).format(valor);

    return texto.replace('COP', 'COP  ');
  }

  formatearSoloFecha(fecha: string): string {
    return formatDateOnly(fecha);
  }

  formatearCanalReserva(canal: string | null | undefined): string {
    if (!canal) {
      return '-';
    }

    const canalSinPrefijo = canal.startsWith('PLATAFORMA_')
      ? canal.replace('PLATAFORMA_', '')
      : canal;

    return canalSinPrefijo
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
  }

  totalPagosEstancia(): number {
    return this.sumarPagos(
      this.obtenerPagosContables().filter((pago) => pago.tipoPago !== 'ANTICIPO_RESERVA')
    );
  }

  totalPagosReserva(): number {
    return this.sumarPagos(
      this.obtenerPagosContables().filter((pago) => pago.tipoPago === 'ANTICIPO_RESERVA')
    );
  }

  totalPagos(): number {
    return this.totalPagosEstancia() + this.totalPagosReserva();
  }

  obtenerOrigenPago(tipoPago?: string | null): string {
    return tipoPago === 'ANTICIPO_RESERVA' ? 'Reserva' : 'Estancia';
  }

  obtenerMontoAbsoluto(monto: number | string | null | undefined): number {
    return Math.abs(Number(monto ?? 0));
  }

  obtenerPagosCambioUnidad() {
    return (this.estanciaDetalle?.pagos ?? []).filter(
      (pago) =>
        pago.tipoPago === 'CAMBIO_UNIDAD' &&
        (pago.estado === 'COMPLETADO' || pago.estado === 'PENDIENTE') &&
        this.obtenerMontoAbsoluto(pago.monto) > 0
    );
  }

  obtenerPagosMostrados() {
    return (this.estanciaDetalle?.pagos ?? []).filter(
      (pago) =>
        pago.tipoPago !== 'CAMBIO_UNIDAD' &&
        (pago.estado === 'COMPLETADO' || pago.estado === 'PENDIENTE')
    );
  }

  get textoDiasCalculoPago(): string {
    const fechaEntrada = this.reservaDetalle?.entradaEstimada ?? this.estanciaDetalle?.entradaReal;
    const fechaSalida = this.reservaDetalle?.salidaEstimada ?? this.estanciaDetalle?.salidaEstimada;

    if (!fechaEntrada || !fechaSalida) {
      return '-';
    }

    const inicio = new Date(this.normalizarFechaInicio(fechaEntrada));
    const fin = new Date(this.normalizarFechaInicio(fechaSalida));
    const diferencia = fin.getTime() - inicio.getTime();

    if (Number.isNaN(diferencia) || diferencia < 0) {
      return '-';
    }

    const dias = Math.max(1, Math.round(diferencia / (1000 * 60 * 60 * 24)));
    return `${dias}`;
  }

  get textoPersonasCalculoPago(): string {
    const numeroPersonas = this.reservaDetalle?.numeroPersonas ?? this.obtenerOcupantesEstancia(this.estanciaDetalle).length;
    return numeroPersonas ? `${numeroPersonas}` : '-';
  }

  private obtenerPagosContables() {
    return (this.estanciaDetalle?.pagos ?? []).filter(
      (pago) => pago.estado === 'COMPLETADO' && pago.tipoPago !== 'CAMBIO_UNIDAD'
    );
  }

  private sumarPagos(pagos: Array<{ monto: number | string | null | undefined }>): number {
    return pagos.reduce((total, pago) => {
      const monto = Number(pago.monto ?? 0);
      return total + (Number.isFinite(monto) ? monto : 0);
    }, 0);
  }

  private normalizarFechaHora(valor: string): string {
    return valor.replace(' ', 'T');
  }

  private normalizarFecha(valor: string): string {
    return valor.replace(' ', 'T').slice(0, 10);
  }

  private normalizarFechaInicio(valor: string): string {
    return `${this.normalizarFecha(valor)}T00:00`;
  }

  private esModoCompletoPadre(): boolean {
    return this.estanciaDetalle?.modoOcupacion === 'COMPLETO' || this.reservaDetalle?.modoOcupacion === 'COMPLETO';
  }

  private normalizarFechaPago(valor: string): string {
    if (valor.includes(' ')) {
      return this.normalizarFechaHora(valor);
    }

    if (valor.includes('T')) {
      return valor;
    }

    const ahora = new Date();
    const horas = `${ahora.getHours()}`.padStart(2, '0');
    const minutos = `${ahora.getMinutes()}`.padStart(2, '0');
    return `${valor}T${horas}:${minutos}`;
  }

  private obtenerFechaActual(): string {
    return getCurrentDateInput();
  }

  get puedeRegistrarEntradaReserva(): boolean {
    if (!this.reservaDetalle) {
      return false;
    }

    if (this.estanciaDetalle) {
      return false;
    }

    return this.reservaDetalle.estadoReserva === 'CONFIRMADA';
  }

  puedeGestionarReserva(reserva: ReservaDTO): boolean {
    return reserva.estadoReserva !== 'COMPLETADA';
  }

  private esEstanciaSinInformacion(estancia: EstanciaDTO | null | undefined): boolean {
    if (!estancia || estancia.modoOcupacion !== 'INDIVIDUAL') {
      return false;
    }

    const idVacio = estancia.id == null;
    const folioVacio = !estancia.codigoFolio;
    const entradaVacia = !estancia.entradaReal;
    const salidaVacia = !estancia.salidaEstimada;
    const notasVacias = estancia.notas == null || estancia.notas === '';
    const ocupantesVacios = this.obtenerOcupantesEstancia(estancia).length === 0;
    const pagosVacios = !estancia.pagos || estancia.pagos.length === 0;

    return (
      idVacio &&
      folioVacio &&
      entradaVacia &&
      salidaVacia &&
      notasVacias &&
      ocupantesVacios &&
      pagosVacios
    );
  }

  obtenerNombreClienteReserva(reserva: ReservaDTO | null | undefined): string {
    if (!reserva) {
      return '';
    }

    const nombres = reserva.cliente?.nombres?.trim() ?? '';
    const apellidos = reserva.cliente?.apellidos?.trim() ?? '';
    return `${nombres} ${apellidos}`.trim();
  }

  obtenerOcupantesEstancia(estancia: EstanciaDTO | null | undefined): OcupanteDTO[] {
    if (!estancia) {
      return [];
    }

    const cliente = estancia.cliente ? [estancia.cliente] : [];
    return [...cliente, ...(estancia.acompanantes ?? [])];
  }

  private cargarUnidad(): void {
    this.cargando = true;
    this.error = '';
    this.unidad = null;
    this.estanciaDetalle = null;
    this.reservaDetalle = null;
    this.estanciaError = '';

    if (!this.codigo) {
      this.error = 'No se recibió el código de la unidad.';
      this.cargando = false;
      return;
    }

    if (this.tipo === 'HABITACION') {
      this.habitacionService.buscarHabitaciones({ codigo: this.codigo }).subscribe({
        next: (habitaciones) => {
          const unidad = this.mapearHabitaciones(habitaciones).find(
            (item) => item.codigo === this.codigo
          );
          if (!unidad) {
            this.error = 'No se encontró la habitación solicitada.';
          } else {
            this.unidad = unidad;
            if (this.esNoDisponible(unidad.estado)) {
              this.verEstancia(unidad);
            }
          }
          this.cargando = false;
        },
        error: (errorResponse: unknown) => {
          this.error = extractBackendErrorMessage(
            errorResponse,
            'No fue posible cargar la habitación.'
          );
          this.cargando = false;
        },
      });
      return;
    }

    this.unidadService
      .buscarUnidades({
        tipo: this.tipo || undefined,
        codigo: this.codigo,
      })
      .subscribe({
        next: (unidades) => {
          if (!unidades.length) {
            this.error = 'No se encontró la unidad solicitada.';
          } else {
            this.unidad = unidades[0];
            if (this.esNoDisponible(this.unidad.estado)) {
              this.verEstancia(this.unidad);
            }
          }
          this.cargando = false;
        },
        error: (errorResponse: unknown) => {
          this.error = extractBackendErrorMessage(
            errorResponse,
            'No fue posible cargar la unidad.'
          );
          this.cargando = false;
        },
      });
  }

  private mapearHabitaciones(habitaciones: HabitacionDTO[]): UnidadDTO[] {
    return habitaciones.map((habitacion) => ({
      id: habitacion.id,
      codigo: habitacion.codigo,
      tipo: 'HABITACION',
      piso: habitacion.piso as Piso,
      estado: habitacion.estado as EstadoOperativo,
      habitaciones: [habitacion],
    }));
  }

  private resolverRutaRetornoDesdeOrigen(): void {
    if (this.retornoExplicito) {
      return;
    }

    const urlAnterior = getPreviousNavigationUrl(this.router, { ignoreSameAsCurrent: true });

    if (!urlAnterior) {
      return;
    }

    this.urlOrigen = urlAnterior;
    this.rutaRetorno = urlAnterior;
  }

  private actualizarRutaRetorno(): void {
    const returnToState = readReturnToFromState(this.router);

    if (returnToState) {
      this.rutaRetorno = returnToState;
      this.retornoExplicito = true;
      return;
    }

    this.retornoExplicito = false;
    this.resolverRutaRetornoDesdeOrigen();
  }
}
