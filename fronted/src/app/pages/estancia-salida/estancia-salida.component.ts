import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FlatpickrModule } from 'angularx-flatpickr';

import { EstadoPago, MedioPago, TipoPago, TipoUnidad } from '../../models/enums';
import { SalidaEstanciaRequest } from '../../models/estancia.model';
import { EstanciaService } from '../../services/estancia.service';
import { PagoService } from '../../services/pago.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

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
  tipoUnidad: TipoUnidad | '' = '';
  entradaReal = '';
  salidaEstimada = '';
  numeroPersonas = 1;
  idPagoEstancia: number | null = null;

  fechaSalidaReal = '';
  notasSalida = '';

  conPago = false;
  tipoPago: TipoPago = 'ESTANCIA_COMPLETADA';
  monto: number | null = null;
  totalCalculado: number | null = null;
  calculandoPago = false;
  calculoError = '';
  medioPago: MedioPago = 'EFECTIVO';
  fechaPago = '';
  estadoPago: EstadoPago = 'PENDIENTE';

  guardando = false;
  error = '';
  exito = '';
  mostrarToast = false;
  toastMensaje = '';

  mediosPago: MedioPago[] = [
    'EFECTIVO',
    'TARJETA_CREDITO',
    'TARJETA_DEBITO',
    'TRANSFERENCIA_BANCARIA',
    'PLATAFORMA',
  ];
  estadosPago: EstadoPago[] = ['PENDIENTE', 'COMPLETADO', 'FALLIDO'];

  constructor(
    private readonly router: Router,
    private readonly estanciaService: EstanciaService,
    private readonly pagoService: PagoService
  ) {}

  ngOnInit(): void {
    const state = (this.router.getCurrentNavigation()?.extras.state ??
      history.state) as Partial<Record<string, unknown>> | null;

    const estanciaIdParam = state?.['estanciaId'] as number | string | undefined;
    const codigoParam = (state?.['codigo'] as string | undefined) ?? '';
    const tipoParam = (state?.['tipo'] as TipoUnidad | undefined) ?? '';
    const entradaParam = state?.['entrada'] as string | undefined;
    const salidaParam = state?.['salida'] as string | undefined;
    const numeroPersonasParam = state?.['numeroPersonas'] as number | string | undefined;
    const idPagoEstanciaParam = state?.['idPagoEstancia'] as number | string | undefined;

    if (estanciaIdParam !== undefined && estanciaIdParam !== null && estanciaIdParam !== '') {
      const parsed =
        typeof estanciaIdParam === 'number'
          ? estanciaIdParam
          : Number.parseInt(estanciaIdParam, 10);
      this.estanciaId = Number.isNaN(parsed) ? null : parsed;
    }

    this.codigo = codigoParam;
    this.tipoUnidad = tipoParam;

    if (numeroPersonasParam !== undefined && numeroPersonasParam !== null && numeroPersonasParam !== '') {
      const parsed =
        typeof numeroPersonasParam === 'number'
          ? numeroPersonasParam
          : Number.parseInt(numeroPersonasParam, 10);
      this.numeroPersonas = Number.isNaN(parsed) || parsed < 1 ? 1 : parsed;
    }

    if (idPagoEstanciaParam !== undefined && idPagoEstanciaParam !== null && idPagoEstanciaParam !== '') {
      const parsed =
        typeof idPagoEstanciaParam === 'number'
          ? idPagoEstanciaParam
          : Number.parseInt(idPagoEstanciaParam, 10);
      this.idPagoEstancia = Number.isNaN(parsed) ? null : parsed;
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

    if (!this.estanciaId) {
      this.error = 'No se encontro la estancia para finalizar.';
    }
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

    if (this.conPago) {
      if (!this.monto || this.monto <= 0 || !this.fechaPago) {
        this.error = 'Completa los datos del pago.';
        return;
      }
    }

    const request: SalidaEstanciaRequest = {
      idEstancia: this.estanciaId,
      fechaSalidaReal: this.normalizarFechaHora(this.fechaSalidaReal),
      notasSalida: this.notasSalida?.trim() ? this.notasSalida.trim() : undefined,
      pagoEstancia: this.conPago ? this.buildPago() : null,
    };

    this.guardando = true;
    this.error = '';
    this.exito = '';

    this.estanciaService.finalizarEstancia(request).subscribe({
      next: () => {
        this.guardando = false;
        this.exito = 'Estancia finalizada con exito.';
        this.mostrarToastExito('Estancia finalizada con exito.', true);
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

  recalcularPago(): void {
    if (!this.conPago) {
      this.totalCalculado = null;
      this.calculoError = '';
      return;
    }

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
        ...(this.idPagoEstancia ? { idPagoEstancia: this.idPagoEstancia } : {}),
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

  volver(): void {
    this.router.navigate(['/recepcion']);
  }

  private buildPago() {
    if (!this.monto || !this.fechaPago) {
      return undefined;
    }

    return {
      tipoPago: this.tipoPago,
      monto: this.monto,
      medioPago: this.medioPago,
      fecha: this.normalizarFechaHora(this.fechaPago),
      estado: this.estadoPago,
    };
  }

  private normalizarFechaHora(valor: string): string {
    return valor.replace(' ', 'T');
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

  private mostrarToastExito(mensaje: string, navegar = false): void {
    this.toastMensaje = mensaje;
    this.mostrarToast = true;
    setTimeout(() => {
      this.mostrarToast = false;
      if (navegar) {
        this.router.navigate(['/recepcion']);
      }
    }, 1500);
  }
}
