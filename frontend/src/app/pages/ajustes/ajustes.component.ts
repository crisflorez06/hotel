import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { Temporada, TipoUnidad } from '../../models/enums';
import { AjusteTemporadaService } from '../../services/ajuste-temporada.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import { TarifaBaseActualizacionDTO, TarifaBaseDTO } from '../../models/tarifa-base.model';
import { TarifaBaseService } from '../../services/tarifa-base.service';
import { FeedbackToastService } from '../../core/services/feedback-toast.service';

@Component({
  selector: 'app-ajustes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ajustes.component.html',
  styleUrl: './ajustes.component.css',
})
export class AjustesComponent implements OnInit {
  readonly tiposTarifaBase: TipoUnidad[] = ['HABITACION', 'APARTAESTUDIO', 'APARTAMENTO'];

  temporadas = [
    { label: 'Temporada alta', value: 'ALTA' as Temporada },
    { label: 'Temporada baja', value: 'BAJA' as Temporada },
  ];

  temporadaSeleccionada: Temporada | '' = '';
  temporadaActiva: Temporada | null = null;
  cargandoActual = false;
  guardando = false;
  error = '';
  tarifasBase: TarifaBaseActualizacionDTO[] = [];
  cargandoTarifas = false;
  errorTarifas = '';
  modalEditarTarifaAbierto = false;
  guardandoTarifa = false;
  tarifaEditando: TarifaBaseActualizacionDTO | null = null;
  tarifaOriginalEditando: TarifaBaseActualizacionDTO | null = null;

  constructor(
    private readonly ajusteTemporadaService: AjusteTemporadaService,
    private readonly tarifaBaseService: TarifaBaseService,
    private readonly feedbackToast: FeedbackToastService
  ) {}

  ngOnInit(): void {
    this.cargarTemporadaActiva();
    this.cargarTarifasBase();
  }

  activarTemporada(): void {
    if (!this.temporadaSeleccionada) {
      this.error = 'Selecciona una temporada para activar.';
      return;
    }

    if (!this.puedeActivarTemporada) {
      return;
    }

    this.guardando = true;
    this.error = '';

    this.ajusteTemporadaService.cambiarTemporada(this.temporadaSeleccionada).subscribe({
      next: () => {
        this.temporadaActiva = this.temporadaSeleccionada as Temporada;
        this.guardando = false;
        this.feedbackToast.showSuccess('Temporada activa actualizada correctamente.');
      },
      error: (errorResponse: unknown) => {
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible activar la temporada. Intenta nuevamente.'
        );
        this.guardando = false;
      },
    });
  }

  limpiarMensajes(): void {
    this.error = '';
  }

  get puedeActivarTemporada(): boolean {
    return Boolean(this.temporadaSeleccionada) && this.temporadaSeleccionada !== this.temporadaActiva;
  }

  limpiarMensajesTarifas(): void {
    this.errorTarifas = '';
  }

  abrirModalEditarTarifa(tarifa: TarifaBaseActualizacionDTO): void {
    this.limpiarMensajesTarifas();
    this.tarifaEditando = { ...tarifa };
    this.tarifaOriginalEditando = { ...tarifa };
    this.modalEditarTarifaAbierto = true;
  }

  cerrarModalEditarTarifa(): void {
    if (this.guardandoTarifa) {
      return;
    }

    this.reiniciarModalEditarTarifa();
  }

  confirmarEditarTarifa(): void {
    if (!this.tarifaEditando || this.guardandoTarifa) {
      return;
    }

    if (!this.huboCambiosTarifa(this.tarifaEditando, this.tarifaOriginalEditando)) {
      this.reiniciarModalEditarTarifa();
      return;
    }

    this.guardandoTarifa = true;
    this.errorTarifas = '';
    const tarifaObjetivo = { ...this.tarifaEditando };

    this.tarifaBaseService.actualizarTarifaBase(tarifaObjetivo).subscribe({
      next: (tarifasActualizadas) => {
        this.aplicarTarifaActualizada(tarifasActualizadas, tarifaObjetivo);
        this.guardandoTarifa = false;
        this.reiniciarModalEditarTarifa();
        this.feedbackToast.showSuccess('Tarifa base actualizada correctamente.');
      },
      error: (errorResponse: unknown) => {
        this.errorTarifas = extractBackendErrorMessage(
          errorResponse,
          'No fue posible actualizar la tarifa de la unidad.'
        );
        this.guardandoTarifa = false;
      },
    });
  }

  trackByTipoUnidad(_: number, tarifa: TarifaBaseActualizacionDTO): TipoUnidad {
    return tarifa.tipoUnidad;
  }

  formatearEtiquetaUnidad(tipo: TipoUnidad): string {
    return tipo.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (letra) => letra.toUpperCase());
  }

  private cargarTemporadaActiva(): void {
    this.cargandoActual = true;
    this.error = '';
    this.ajusteTemporadaService
      .obtenerTemporadaActiva()
      .pipe(
        finalize(() => {
          this.cargandoActual = false;
        })
      )
      .subscribe({
        next: (temporada) => {
          this.temporadaActiva = temporada;
          this.temporadaSeleccionada = temporada;
        },
        error: (errorResponse: unknown) => {
          this.error = extractBackendErrorMessage(
            errorResponse,
            'No fue posible cargar la temporada activa.'
          );
        },
      });
  }

  private cargarTarifasBase(): void {
    this.cargandoTarifas = true;
    this.errorTarifas = '';

    this.tarifaBaseService
      .listarTarifasBase()
      .pipe(
        finalize(() => {
          this.cargandoTarifas = false;
        })
      )
      .subscribe({
        next: (tarifas) => {
          this.tarifasBase = this.ordenarTarifas(tarifas).map((tarifa) => ({ ...tarifa }));
        },
        error: (errorResponse: unknown) => {
          this.errorTarifas = extractBackendErrorMessage(
            errorResponse,
            'No fue posible cargar las tarifas base.'
          );
        },
      });
  }

  private ordenarTarifas(tarifas: TarifaBaseDTO[]): TarifaBaseDTO[] {
    const prioridades = new Map(this.tiposTarifaBase.map((tipo, indice) => [tipo, indice]));
    return [...tarifas].sort(
      (a, b) => (prioridades.get(a.tipoUnidad) ?? 999) - (prioridades.get(b.tipoUnidad) ?? 999)
    );
  }

  private aplicarTarifaActualizada(
    respuesta: TarifaBaseDTO[],
    tarifaFallback: TarifaBaseActualizacionDTO
  ): void {
    if (respuesta.length > 1) {
      this.tarifasBase = this.ordenarTarifas(respuesta).map((tarifa) => ({ ...tarifa }));
      return;
    }

    const tarifaActualizada = respuesta[0] ?? tarifaFallback;
    this.tarifasBase = this.tarifasBase.map((tarifa) =>
      tarifa.tipoUnidad === tarifaActualizada.tipoUnidad ? { ...tarifaActualizada } : tarifa
    );
  }

  private reiniciarModalEditarTarifa(): void {
    this.modalEditarTarifaAbierto = false;
    this.tarifaEditando = null;
    this.tarifaOriginalEditando = null;
  }

  private huboCambiosTarifa(
    tarifaActual: TarifaBaseActualizacionDTO,
    tarifaOriginal: TarifaBaseActualizacionDTO | null
  ): boolean {
    if (!tarifaOriginal) {
      return true;
    }

    return (
      tarifaActual.precioDiaTemBaja !== tarifaOriginal.precioDiaTemBaja ||
      tarifaActual.precioDiaTemAlta !== tarifaOriginal.precioDiaTemAlta ||
      tarifaActual.precioEstadiaCorta !== tarifaOriginal.precioEstadiaCorta ||
      tarifaActual.precioPersonaAdicionalTemBaja !== tarifaOriginal.precioPersonaAdicionalTemBaja ||
      tarifaActual.precioPersonaAdicionalTemAlta !== tarifaOriginal.precioPersonaAdicionalTemAlta ||
      tarifaActual.precioEstadiaPersonaAdicionalCorta !== tarifaOriginal.precioEstadiaPersonaAdicionalCorta
    );
  }
}
