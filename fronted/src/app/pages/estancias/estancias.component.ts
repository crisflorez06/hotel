import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { PageResponse } from '../../models/page.model';
import { EstadoEstancia, ModoOcupacion, TipoUnidad } from '../../models/enums';
import { EstanciaTablaFiltros, EstanciaTablaItem } from '../../models/estancia-tabla.model';
import { EstanciaService } from '../../services/estancia.service';

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

  page = 0;
  size = 20;
  totalElements = 0;
  totalPages = 0;

  private filtroCambioTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(private readonly estanciaService: EstanciaService) {}

  ngOnInit(): void {
    this.cargarEstancias();
  }

  ngOnDestroy(): void {
    if (this.filtroCambioTimeout) {
      clearTimeout(this.filtroCambioTimeout);
      this.filtroCambioTimeout = null;
    }
  }

  aplicarFiltros(): void {
    this.page = 0;
    this.cargarEstancias();
  }

  onFiltroCambio(): void {
    if (this.filtroCambioTimeout) {
      clearTimeout(this.filtroCambioTimeout);
    }

    this.filtroCambioTimeout = setTimeout(() => {
      this.aplicarFiltros();
      this.filtroCambioTimeout = null;
    }, 350);
  }

  limpiarFiltros(): void {
    this.filtros = this.crearFiltrosVacios();
    this.page = 0;
    this.cargarEstancias();
  }

  toggleEstado(estado: EstadoEstancia, checked: boolean): void {
    this.filtros.estados = checked
      ? Array.from(new Set([...this.filtros.estados, estado]))
      : this.filtros.estados.filter((item) => item !== estado);
    this.aplicarFiltros();
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
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.page = response.number ?? this.page;
        this.size = response.size;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No fue posible cargar la tabla de estancias.';
        this.estancias = [];
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
}
