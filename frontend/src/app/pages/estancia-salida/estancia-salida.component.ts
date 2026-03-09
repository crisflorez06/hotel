import { CommonModule, Location } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FlatpickrModule } from 'angularx-flatpickr';

import { EstadoPago, MedioPago, TipoPago, TipoUnidad } from '../../models/enums';
import { SalidaEstanciaRequest } from '../../models/estancia.model';
import { EstanciaDTO } from '../../models/estancia-detalle.model';
import { EstanciaService } from '../../services/estancia.service';
import { PagoService } from '../../services/pago.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import { FeedbackToastService } from '../../core/services/feedback-toast.service';

@Component({
  selector: 'app-estancia-salida',
  standalone: true,
  imports: [CommonModule, FormsModule, FlatpickrModule],
  templateUrl: './estancia-salida.component.html',
  styleUrl: './estancia-salida.component.css',
})
export class EstanciaSalidaComponent implements OnInit {
  estanciaId: number | null = null;
  codigo = '';
  nombreCliente = '';
  tipoUnidad: TipoUnidad | '' = '';
  entradaReal = '';
  salidaEstimada = '';
  numeroPersonas = 1;

  fechaSalidaReal = '';
  notasSalida = '';

  tipoPago: TipoPago = 'ESTANCIA_COMPLETADA';
  monto: number | null = null;
  totalCalculado: number | null = null;
  totalAnticipoReserva = 0;
  totalAnticipoEstancia = 0;
  totalPagoCambioUnidad = 0;
  calculandoPago = false;
  calculoError = '';
  medioPago: MedioPago = 'EFECTIVO';
  fechaPago: string | Date = '';
  estadoPago: EstadoPago = 'PENDIENTE';

  guardando = false;
  mostrarModalConfirmarSalida = false;
  error = '';
  exito = '';
  rutaRetorno = '/recepcion';
  private urlOrigen: string | null = null;
  private retornoExplicito = false;

  mediosPago: MedioPago[] = [
    'EFECTIVO',
    'TARJETA_CREDITO',
    'TARJETA_DEBITO',
    'TRANSFERENCIA_BANCARIA',
    'PLATAFORMA',
  ];
  estadosPago: EstadoPago[] = ['PENDIENTE', 'COMPLETADO'];

  constructor(
    private readonly router: Router,
    private readonly location: Location,
    private readonly estanciaService: EstanciaService,
    private readonly pagoService: PagoService,
    private readonly feedbackToastService: FeedbackToastService
  ) {}

  ngOnInit(): void {
    const state = (this.router.getCurrentNavigation()?.extras.state ??
      history.state) as Partial<Record<string, unknown>> | null;

    const estanciaIdParam = state?.['estanciaId'] as number | string | undefined;
    const codigoParam = (state?.['codigo'] as string | undefined) ?? '';
    const nombreClienteParam = (state?.['nombreCliente'] as string | undefined) ?? '';
    const tipoParam = (state?.['tipo'] as TipoUnidad | undefined) ?? '';
    const entradaParam = state?.['entrada'] as string | undefined;
    const salidaParam = state?.['salida'] as string | undefined;
    const numeroPersonasParam = state?.['numeroPersonas'] as number | string | undefined;
    const returnToParam = state?.['returnTo'] as string | undefined;

    if (returnToParam?.trim()) {
      this.rutaRetorno = returnToParam;
      this.retornoExplicito = true;
    }

    this.resolverRutaRetornoDesdeOrigen();

    if (estanciaIdParam !== undefined && estanciaIdParam !== null && estanciaIdParam !== '') {
      const parsed =
        typeof estanciaIdParam === 'number'
          ? estanciaIdParam
          : Number.parseInt(estanciaIdParam, 10);
      this.estanciaId = Number.isNaN(parsed) ? null : parsed;
    }

    this.codigo = codigoParam;
    this.nombreCliente = nombreClienteParam.trim();
    this.tipoUnidad = tipoParam;

    if (numeroPersonasParam !== undefined && numeroPersonasParam !== null && numeroPersonasParam !== '') {
      const parsed =
        typeof numeroPersonasParam === 'number'
          ? numeroPersonasParam
          : Number.parseInt(numeroPersonasParam, 10);
      this.numeroPersonas = Number.isNaN(parsed) || parsed < 1 ? 1 : parsed;
    }

    if (entradaParam) {
      this.entradaReal = this.formatearFechaHora(entradaParam);
    }

    if (salidaParam) {
      this.salidaEstimada = this.formatearFechaHora(salidaParam);
    }

    if (!this.fechaSalidaReal) {
      this.fechaSalidaReal = this.formatearFechaHoraLocal(new Date());
    }

    if (!this.fechaPago) {
      this.fechaPago = this.formatearFechaHoraLocal(new Date());
    }

    if (!this.estanciaId) {
      this.error = 'No se encontro la estancia para finalizar.';
      return;
    }

    this.cargarAnticiposRegistrados();
    this.recalcularPago();
  }

  guardar(): void {
    if (!this.estanciaId) {
      this.error = 'No se encontro la estancia para finalizar.';
      return;
    }

    if (!this.fechaSalidaReal) {
      this.error = 'La fecha de salida real es obligatoria.';
      return;
    }

    const fechaPagoNormalizada = this.obtenerFechaPagoNormalizada();
    if (!fechaPagoNormalizada) {
      this.error = 'Completa los datos del pago.';
      return;
    }

    if (this.monto === null || !Number.isFinite(Number(this.monto))) {
      this.monto = this.totalCalculado ?? 0;
    }

    const montoPago = Number(this.monto);
    this.monto = montoPago;
    this.fechaPago = fechaPagoNormalizada;

    const request: SalidaEstanciaRequest = {
      idEstancia: this.estanciaId,
      fechaSalidaReal: this.normalizarFechaHora(this.fechaSalidaReal),
      notasSalida: this.notasSalida?.trim() ? this.notasSalida.trim() : undefined,
      pagoEstancia: this.buildPago(),
    };

    this.guardando = true;
    this.error = '';
    this.exito = '';

    this.estanciaService.finalizarEstancia(request).subscribe({
      next: () => {
        this.guardando = false;
        this.exito = 'Estancia finalizada con exito.';
        this.feedbackToastService.showSuccess('Estancia finalizada con exito.');
        this.router.navigateByUrl(this.rutaRetorno);
      },
      error: (errorResponse: unknown) => {
        this.guardando = false;
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible finalizar la estancia.'
        );
      },
    });
  }

  solicitarConfirmacionFinalizar(): void {
    if (this.guardando || this.calculandoPago || !this.estanciaId) {
      return;
    }

    this.mostrarModalConfirmarSalida = true;
  }

  cerrarModalConfirmarSalida(): void {
    if (this.guardando) {
      return;
    }

    this.mostrarModalConfirmarSalida = false;
  }

  confirmarFinalizarEstancia(): void {
    this.mostrarModalConfirmarSalida = false;
    this.guardar();
  }

  mantenerModal(event: MouseEvent): void {
    event.stopPropagation();
  }

  recalcularPago(): void {
    this.calculoError = '';
    this.totalCalculado = null;

    if (!this.tipoUnidad || !this.entradaReal || !this.fechaSalidaReal) {
      return;
    }

    if (this.numeroPersonas < 1) {
      this.calculoError = 'El numero de personas debe ser al menos 1.';
      return;
    }

    this.calculandoPago = true;

    this.pagoService
      .calcularTotal({
        tipoUnidad: this.tipoUnidad as TipoUnidad,
        numeroPersonas: this.numeroPersonas,
        fechaEntrada: this.normalizarFechaHora(this.entradaReal),
        fechaSalida: this.normalizarFechaHora(this.fechaSalidaReal),
        ...(this.estanciaId ? { idEstancia: this.estanciaId } : {}),
      })
      .subscribe({
        next: (total) => {
          this.totalCalculado = total;
          this.monto = total;
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

  get textoDiasCalculoPago(): string {
    if (!this.entradaReal || !this.fechaSalidaReal) {
      return '-';
    }

    const inicio = new Date(this.normalizarFechaHora(this.entradaReal));
    const fin = new Date(this.normalizarFechaHora(this.fechaSalidaReal));
    const diferencia = fin.getTime() - inicio.getTime();

    if (Number.isNaN(diferencia) || diferencia < 0) {
      return '-';
    }

    const dias = Math.max(1, Math.round(diferencia / (1000 * 60 * 60 * 24)));
    return `${dias}`;
  }

  get totalAnticiposRegistrados(): number {
    return this.totalAnticipoReserva + this.totalAnticipoEstancia;
  }

  get totalPagadoAcumulado(): number {
    const montoActual = Number(this.monto ?? 0);
    return this.totalAnticiposRegistrados + (Number.isFinite(montoActual) ? montoActual : 0);
  }

  get totalPagoSalidaResumen(): number {
    const montoActual = Number(this.monto ?? 0);
    return Number.isFinite(montoActual) ? montoActual : 0;
  }

  get mostrarPagoCambioUnidad(): boolean {
    return this.totalPagoCambioUnidad !== 0;
  }

  get textoPagoPendienteCambioUnidad(): string {
    const valor = Number(this.totalPagoCambioUnidad ?? 0);
    if (!Number.isFinite(valor) || valor === 0) {
      return '$ 0';
    }

    const signo = valor < 0 ? '-' : '+';
    const montoFormateado = new Intl.NumberFormat('es-CO', {
      maximumFractionDigits: 0,
    }).format(Math.abs(valor));
    return `${signo} $ ${montoFormateado}`;
  }

  get saldoEstimadoSalida(): number | null {
    if (this.totalCalculado === null) {
      return null;
    }

    return this.totalCalculado - this.totalAnticiposRegistrados;
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

  private buildPago() {
    return {
      tipoPago: this.tipoPago,
      monto: Number(this.monto ?? 0),
      medioPago: this.medioPago,
      fecha: this.normalizarFechaHora(this.fechaPago),
      estado: this.estadoPago,
    };
  }

  private normalizarFechaHora(valor: string | Date): string {
    if (valor instanceof Date) {
      return this.formatearFechaHoraLocal(valor).replace(' ', 'T');
    }

    return `${valor}`.trim().replace(' ', 'T');
  }

  private formatearFechaHora(valor: string): string {
    const normalizado = valor.replace(' ', 'T');
    const [fecha, hora] = normalizado.split('T');
    if (!fecha || !hora) {
      return valor;
    }
    return `${fecha} ${hora.slice(0, 5)}`;
  }

  private formatearFechaHoraLocal(fecha: Date): string {
    const year = fecha.getFullYear();
    const month = String(fecha.getMonth() + 1).padStart(2, '0');
    const day = String(fecha.getDate()).padStart(2, '0');
    const hour = String(fecha.getHours()).padStart(2, '0');
    const minute = String(fecha.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hour}:${minute}`;
  }

  private obtenerFechaPagoNormalizada(): string {
    if (!this.fechaPago) {
      return this.formatearFechaHoraLocal(new Date());
    }

    if (this.fechaPago instanceof Date) {
      return this.formatearFechaHoraLocal(this.fechaPago);
    }

    const texto = `${this.fechaPago}`.trim();
    return texto || this.formatearFechaHoraLocal(new Date());
  }

  private cargarAnticiposRegistrados(): void {
    if (!this.estanciaId) {
      this.totalAnticipoReserva = 0;
      this.totalAnticipoEstancia = 0;
      this.totalPagoCambioUnidad = 0;
      return;
    }

    this.estanciaService.obtenerEstanciaPorId(this.estanciaId).subscribe({
      next: (estancia) => {
        this.totalAnticipoReserva = this.sumarPagosPorTipo(estancia, 'ANTICIPO_RESERVA');
        this.totalAnticipoEstancia = this.sumarPagosPorTipo(estancia, 'ANTICIPO_ESTANCIA');
        this.totalPagoCambioUnidad = this.sumarPagosCambioUnidad(estancia);
      },
      error: () => {
        this.totalAnticipoReserva = 0;
        this.totalAnticipoEstancia = 0;
        this.totalPagoCambioUnidad = 0;
      },
    });
  }

  private sumarPagosPorTipo(estancia: EstanciaDTO, tipoPago: TipoPago): number {
    return (estancia.pagos ?? [])
      .filter((pago) => pago.estado === 'COMPLETADO' && pago.tipoPago === tipoPago)
      .reduce((total, pago) => total + Number(pago.monto ?? 0), 0);
  }

  private sumarPagosCambioUnidad(estancia: EstanciaDTO): number {
    return (estancia.pagos ?? [])
      .filter(
        (pago) =>
          pago.tipoPago === 'CAMBIO_UNIDAD' &&
          (pago.estado === 'COMPLETADO' || pago.estado === 'PENDIENTE')
      )
      .reduce((total, pago) => total + Number(pago.monto ?? 0), 0);
  }

  private resolverRutaRetornoDesdeOrigen(): void {
    if (this.retornoExplicito) {
      return;
    }

    const currentNavigation = this.router.getCurrentNavigation();
    const urlAnterior = currentNavigation?.previousNavigation?.finalUrl?.toString() ?? null;

    if (!urlAnterior || urlAnterior.startsWith('/estancias/salida')) {
      return;
    }

    this.urlOrigen = urlAnterior;
    this.rutaRetorno = urlAnterior;
  }
}
