import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { PageResponse } from '../../models/page.model';
import { EstadoEstancia, ModoOcupacion, TipoUnidad } from '../../models/enums';
import { EstanciaTablaFiltros, EstanciaTablaItem } from '../../models/estancia-tabla.model';
import { EstanciaService } from '../../services/estancia.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

@Component({
  selector: 'app-estancias',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './estancias.component.html',
  styleUrl: './estancias.component.css',
})
export class EstanciasComponent implements OnInit, OnDestroy {
  readonly estadosEstanciaDisponibles: EstadoEstancia[] = [
    'ACTIVA',
    'RESERVADA',
    'FINALIZADA',
    'EXCEDIDA',
    'CANCELADA',
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

  readonly reservaAsociadaOpciones: Array<{ label: string; value: '' | 'SI' | 'NO' }> = [
    { label: 'Todas', value: '' },
    { label: 'Con reserva', value: 'SI' },
    { label: 'Sin reserva', value: 'NO' },
  ];

  readonly pageSizeOptions = [10, 20, 50, 100];

  filtros: EstanciaTablaFiltros = this.crearFiltrosVacios();

  estancias: EstanciaTablaItem[] = [];
  cargando = false;
  error = '';
  accionError = '';
  accionExito = '';

  page = 0;
  size = 20;
  totalElements = 0;
  totalPages = 0;
  estanciaSeleccionadaId: number | null = null;
  eliminandoEstanciaId: number | null = null;

  private queryParamsSub: Subscription | null = null;

  constructor(
    private readonly estanciaService: EstanciaService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.queryParamsSub = this.route.queryParamMap.subscribe((params) => {
      const codigoEstancia = (params.get('codigoEstancia') ?? '').trim();
      if (codigoEstancia !== this.filtros.codigoEstancia) {
        this.filtros.codigoEstancia = codigoEstancia;
      }
      this.page = 0;
      this.cargarEstancias();
    });
  }

  ngOnDestroy(): void {
    this.queryParamsSub?.unsubscribe();
    this.queryParamsSub = null;
  }

  aplicarFiltros(): void {
    this.accionError = '';
    this.accionExito = '';
    this.page = 0;
    this.cargarEstancias();
  }

  limpiarFiltros(): void {
    this.filtros = this.crearFiltrosVacios();
    this.accionError = '';
    this.accionExito = '';
    this.page = 0;
    this.cargarEstancias();
  }

  toggleEstado(estado: EstadoEstancia, checked: boolean): void {
    this.filtros.estados = checked
      ? Array.from(new Set([...this.filtros.estados, estado]))
      : this.filtros.estados.filter((item) => item !== estado);
  }

  estaEstadoSeleccionado(estado: EstadoEstancia): boolean {
    return this.filtros.estados.includes(estado);
  }

  cambiarPageSize(size: number): void {
    this.size = size;
    this.page = 0;
    this.cargarEstancias();
  }

  irPaginaAnterior(): void {
    if (this.page <= 0 || this.cargando) {
      return;
    }

    this.page -= 1;
    this.cargarEstancias();
  }

  irPaginaSiguiente(): void {
    if (this.cargando || this.page + 1 >= this.totalPages) {
      return;
    }

    this.page += 1;
    this.cargarEstancias();
  }

  trackByEstanciaId(_: number, estancia: EstanciaTablaItem): number {
    return estancia.id;
  }

  seleccionarEstancia(estancia: EstanciaTablaItem): void {
    this.estanciaSeleccionadaId =
      this.estanciaSeleccionadaId === estancia.id ? null : estancia.id;
  }

  esEstanciaSeleccionada(estancia: EstanciaTablaItem): boolean {
    return this.estanciaSeleccionadaId === estancia.id;
  }

  editarEstanciaSeleccionada(): void {
    const estancia = this.obtenerEstanciaSeleccionada();
    if (!estancia || !this.puedeGestionarEstancia(estancia)) {
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.router.navigate(['/estancias/nueva'], {
      queryParams: {
        codigo: estancia.codigoUnidad,
        tipo: estancia.tipoUnidad,
        editar: true,
        estanciaId: estancia.id,
      },
    });
  }

  eliminarEstanciaSeleccionada(): void {
    const estancia = this.obtenerEstanciaSeleccionada();
    if (!estancia || !this.puedeGestionarEstancia(estancia) || this.eliminandoEstanciaId !== null) {
      return;
    }

    const confirmacion = window.confirm(
      `Se eliminara la estancia ${estancia.codigoEstancia}. Esta accion no se puede deshacer.`
    );
    if (!confirmacion) {
      return;
    }

    this.eliminandoEstanciaId = estancia.id;
    this.accionError = '';
    this.accionExito = '';
    this.estanciaService.eliminarEstancia(estancia.id).subscribe({
      next: () => {
        this.eliminandoEstanciaId = null;
        this.accionExito = 'Estancia eliminada correctamente.';
        this.cargarEstancias();
      },
      error: (errorResponse: unknown) => {
        this.eliminandoEstanciaId = null;
        this.accionError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible eliminar la estancia.'
        );
        this.accionExito = '';
      },
    });
  }

  irAPagosPorEstancia(estancia: EstanciaTablaItem): void {
    const codigoEstancia = estancia.codigoEstancia?.trim() ?? '';
    if (!codigoEstancia) {
      return;
    }

    this.router.navigate(['/pagos'], {
      queryParams: { codigoEstancia },
    });
  }

  puedeGestionarEstanciaSeleccionada(): boolean {
    const estancia = this.obtenerEstanciaSeleccionada();
    return !!estancia && this.puedeGestionarEstancia(estancia);
  }

  estaEliminandoEstanciaSeleccionada(): boolean {
    const estancia = this.obtenerEstanciaSeleccionada();
    return !!estancia && this.eliminandoEstanciaId === estancia.id;
  }

  irATablaReservas(estancia: EstanciaTablaItem): void {
    const codigoReserva = estancia.codigoReservaAsociada?.trim() ?? '';
    if (!codigoReserva) {
      return;
    }

    this.router.navigate(['/reservas'], {
      queryParams: { codigoReserva },
    });
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

  get indiceInicio(): number {
    return this.totalElements === 0 ? 0 : this.page * this.size + 1;
  }

  get indiceFin(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }

  private cargarEstancias(): void {
    this.cargando = true;
    this.error = '';

    const filtros: EstanciaTablaFiltros = {
      ...this.filtros,
      entradaDesde: this.normalizarFechaHora(this.filtros.entradaDesde),
      entradaHasta: this.normalizarFechaHora(this.filtros.entradaHasta),
      salidaEstimadaDesde: this.normalizarFechaHora(this.filtros.salidaEstimadaDesde),
      salidaEstimadaHasta: this.normalizarFechaHora(this.filtros.salidaEstimadaHasta),
      salidaRealDesde: this.normalizarFechaHora(this.filtros.salidaRealDesde),
      salidaRealHasta: this.normalizarFechaHora(this.filtros.salidaRealHasta),
    };

    const sort = ['entradaReal,desc'];

    this.estanciaService.obtenerTabla(filtros, this.page, this.size, sort).subscribe({
      next: (response: PageResponse<EstanciaTablaItem>) => {
        this.estancias = response.content;
        if (this.estanciaSeleccionadaId !== null) {
          const estanciaSigueDisponible = this.estancias.some(
            (estancia) => estancia.id === this.estanciaSeleccionadaId
          );
          if (!estanciaSigueDisponible) {
            this.estanciaSeleccionadaId = null;
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
          'No fue posible cargar la tabla de estancias.'
        );
        this.estancias = [];
        this.estanciaSeleccionadaId = null;
        this.eliminandoEstanciaId = null;
        this.totalElements = 0;
        this.totalPages = 0;
        this.cargando = false;
      },
    });
  }

  private crearFiltrosVacios(): EstanciaTablaFiltros {
    return {
      estados: [],
      modoOcupacion: '',
      tipoUnidad: '',
      codigoEstancia: '',
      codigoUnidad: '',
      nombreCliente: '',
      numeroDocumentoCliente: '',
      entradaDesde: '',
      entradaHasta: '',
      salidaEstimadaDesde: '',
      salidaEstimadaHasta: '',
      salidaRealDesde: '',
      salidaRealHasta: '',
      tieneReservaAsociada: '',
    };
  }

  private normalizarFechaHora(fecha: string): string {
    const valor = fecha.trim();
    if (!valor) {
      return '';
    }

    return valor.length === 16 ? `${valor}:00` : valor;
  }

  private puedeGestionarEstancia(estancia: EstanciaTablaItem): boolean {
    return estancia.estadoEstancia !== 'FINALIZADA' && estancia.estadoEstancia !== 'CANCELADA';
  }

  private obtenerEstanciaSeleccionada(): EstanciaTablaItem | null {
    if (this.estanciaSeleccionadaId === null) {
      return null;
    }

    return this.estancias.find((estancia) => estancia.id === this.estanciaSeleccionadaId) ?? null;
  }
}
