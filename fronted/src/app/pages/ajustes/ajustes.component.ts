import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Temporada } from '../../models/enums';
import { AjusteTemporadaService } from '../../services/ajuste-temporada.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

@Component({
  selector: 'app-ajustes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ajustes.component.html',
  styleUrl: './ajustes.component.css',
})
export class AjustesComponent implements OnInit {
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

  constructor(private readonly ajusteTemporadaService: AjusteTemporadaService) {}

  ngOnInit(): void {
    this.cargarTemporadaActiva();
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
}
