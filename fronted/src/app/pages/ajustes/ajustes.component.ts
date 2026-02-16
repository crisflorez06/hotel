import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Temporada, TipoUnidad } from '../../models/enums';
import { AjusteTemporadaService } from '../../services/ajuste-temporada.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import { TarifaBaseActualizacionDTO, TarifaBaseDTO } from '../../models/tarifa-base.model';
import { TarifaBaseService } from '../../services/tarifa-base.service';

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
  exito = '';
  tarifasBase: TarifaBaseActualizacionDTO[] = [];
  cargandoTarifas = false;
  guardandoTarifas = false;
  errorTarifas = '';
  exitoTarifas = '';

  constructor(
    private readonly ajusteTemporadaService: AjusteTemporadaService,
    private readonly tarifaBaseService: TarifaBaseService
  ) {}

  ngOnInit(): void {
    this.cargarTemporadaActiva();
    this.cargarTarifasBase();
  }

  activarTemporada(): void {
    if (!this.temporadaSeleccionada) {
      this.error = 'Selecciona una temporada para activar.';
      this.exito = '';
      return;
    }

    this.guardando = true;
    this.error = '';
    this.exito = '';

    this.ajusteTemporadaService.cambiarTemporada(this.temporadaSeleccionada).subscribe({
      next: () => {
        this.temporadaActiva = this.temporadaSeleccionada as Temporada;
        this.exito = `La temporada ${this.temporadaSeleccionada.toLowerCase()} fue activada.`;
        this.guardando = false;
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
    this.exito = '';
  }

  guardarTarifasBase(): void {
    if (!this.tarifasBase.length) {
      this.errorTarifas = 'No hay tarifas disponibles para guardar.';
      this.exitoTarifas = '';
      return;
    }

    this.guardandoTarifas = true;
    this.errorTarifas = '';
    this.exitoTarifas = '';

    this.tarifaBaseService.actualizarTarifasBase(this.tarifasBase).subscribe({
      next: (tarifasActualizadas) => {
        this.tarifasBase = this.ordenarTarifas(tarifasActualizadas).map((tarifa) => ({ ...tarifa }));
        this.exitoTarifas = 'Las tarifas base fueron actualizadas correctamente.';
        this.guardandoTarifas = false;
      },
      error: (errorResponse: unknown) => {
        this.errorTarifas = extractBackendErrorMessage(
          errorResponse,
          'No fue posible actualizar las tarifas base. Intenta nuevamente.'
        );
        this.guardandoTarifas = false;
      },
    });
  }

  limpiarMensajesTarifas(): void {
    this.errorTarifas = '';
    this.exitoTarifas = '';
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
    this.ajusteTemporadaService.obtenerTemporadaActiva().subscribe({
      next: (temporada) => {
        this.temporadaActiva = temporada;
        this.temporadaSeleccionada = temporada;
        this.cargandoActual = false;
      },
      error: (errorResponse: unknown) => {
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar la temporada activa.'
        );
        this.cargandoActual = false;
      },
    });
  }

  private cargarTarifasBase(): void {
    this.cargandoTarifas = true;
    this.errorTarifas = '';

    this.tarifaBaseService.listarTarifasBase().subscribe({
      next: (tarifas) => {
        this.tarifasBase = this.ordenarTarifas(tarifas).map((tarifa) => ({ ...tarifa }));
        this.cargandoTarifas = false;
      },
      error: (errorResponse: unknown) => {
        this.errorTarifas = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar las tarifas base.'
        );
        this.cargandoTarifas = false;
      },
    });
  }

  private ordenarTarifas(tarifas: TarifaBaseDTO[]): TarifaBaseDTO[] {
    const prioridades = new Map(this.tiposTarifaBase.map((tipo, indice) => [tipo, indice]));
    return [...tarifas].sort(
      (a, b) => (prioridades.get(a.tipoUnidad) ?? 999) - (prioridades.get(b.tipoUnidad) ?? 999)
    );
  }
}
