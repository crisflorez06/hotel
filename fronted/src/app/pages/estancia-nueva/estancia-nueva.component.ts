import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FlatpickrModule } from 'angularx-flatpickr';

import { EstanciaService } from '../../services/estancia.service';
import { EstadoPago, MedioPago, TipoPago, TipoUnidad } from '../../models/enums';
import { EstanciaNuevoRequest } from '../../models/estancia.model';

@Component({
  selector: 'app-estancia-nueva',
  standalone: true,
  imports: [CommonModule, FormsModule, FlatpickrModule],
  templateUrl: './estancia-nueva.component.html',
  styleUrl: './estancia-nueva.component.css',
})
export class EstanciaNuevaComponent implements OnInit {
  codigo = '';
  tipoUnidad: TipoUnidad | '' = '';

  idCliente: number | null = null;
  entradaReal = '';
  salidaEstimada = '';
  idAcompanantes = '';
  notas = '';

  conPago = false;
  tipoPago: TipoPago = 'ESTANCIA';
  monto: number | null = null;
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

  estadosPago: EstadoPago[] = ['PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO'];
  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly estanciaService: EstanciaService
  ) {}

  ngOnInit(): void {
    this.codigo = this.route.snapshot.queryParamMap.get('codigo') ?? '';
    this.tipoUnidad = (this.route.snapshot.queryParamMap.get('tipo') as TipoUnidad) ?? '';
  }

  guardar(): void {
    if (
      !this.tipoUnidad ||
      !this.codigo ||
      this.idCliente === null ||
      Number.isNaN(Number(this.idCliente)) ||
      !this.entradaReal ||
      !this.salidaEstimada
    ) {
      this.error = 'Completa los campos obligatorios.';
      return;
    }

    this.guardando = true;
    this.error = '';
    this.exito = '';

    const request: EstanciaNuevoRequest = {
      tipoUnidad: this.tipoUnidad,
      codigo: this.codigo,
      idCliente: this.idCliente,
      entradaReal: this.normalizarFechaHora(this.entradaReal),
      salidaEstimada: this.normalizarFechaHora(this.salidaEstimada),
      idAcompanantes: this.parseAcompanantes(),
      notas: this.notas || undefined,
      pago: this.conPago ? this.buildPago() : null,
    };

    this.estanciaService.crearEstancia(request).subscribe({
      next: () => {
        this.guardando = false;
        this.exito = 'Estancia registrada con exito.';
        this.mostrarToastExito('Estancia registrada con exito.');
      },
      error: () => {
        this.guardando = false;
        this.error = 'No fue posible registrar la estancia.';
      },
    });
  }

  volver(): void {
    this.router.navigate(['/recepcion']);
  }

  private parseAcompanantes(): number[] | undefined {
    const ids = this.idAcompanantes
      .split(',')
      .map((value) => Number(value.trim()))
      .filter((value) => !Number.isNaN(value) && value > 0);

    return ids.length ? ids : undefined;
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

  private mostrarToastExito(mensaje: string): void {
    this.toastMensaje = mensaje;
    this.mostrarToast = true;
    setTimeout(() => {
      this.mostrarToast = false;
      this.router.navigate(['/recepcion']);
    }, 1500);
  }
}
