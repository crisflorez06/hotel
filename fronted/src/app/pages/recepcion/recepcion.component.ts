import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { HabitacionDTO } from '../../models/habitacion.model';
import { UnidadDTO } from '../../models/unidad.model';
import { HabitacionService } from '../../services/habitacion.service';
import { UnidadService } from '../../services/unidad.service';
import { EstadoOperativo, Piso, TipoUnidad } from '../../models/enums';

@Component({
  selector: 'app-recepcion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './recepcion.component.html',
  styleUrl: './recepcion.component.css',
})
export class RecepcionComponent implements OnInit {
  unidades: UnidadDTO[] = [];
  cargando = true;
  error = '';
  private codigoTimer?: ReturnType<typeof setTimeout>;

  filtroTipo: TipoUnidad | '' = '';
  filtroEstados: EstadoOperativo[] = [];
  filtroPisos: Piso[] = [];
  filtroCodigo = '';

  tiposUnidad = [
    { label: 'UNIDAD', value: 'APARTAMENTO' as TipoUnidad },
    { label: 'HABITACION', value: 'HABITACION' as TipoUnidad },
    { label: 'APARTAESTUDIO', value: 'APARTAESTUDIO' as TipoUnidad },
  ];
  estadosOperativos: EstadoOperativo[] = ['DISPONIBLE', 'PARCIALMENTE', 'OCUPADO', 'RESERVADO'];
  pisos: Piso[] = ['PISO_1', 'PISO_2', 'PISO_3'];

  constructor(
    private readonly unidadService: UnidadService,
    private readonly habitacionService: HabitacionService
  ) {}

  ngOnInit(): void {
    this.buscarUnidades();
  }

  trackByUnidad(_: number, item: UnidadDTO) {
    return item.id;
  }

  trackByHabitacion(_: number, item: HabitacionDTO) {
    return item.id;
  }

  esTipoSeleccionado(tipo: TipoUnidad | ''): boolean {
    return this.filtroTipo === tipo;
  }

  setTipo(tipo: TipoUnidad | ''): void {
    this.filtroTipo = tipo;
    this.buscarUnidades();
  }

  esEstadoSeleccionado(estado: EstadoOperativo): boolean {
    return this.filtroEstados.includes(estado);
  }

  toggleEstado(estado: EstadoOperativo): void {
    this.filtroEstados = this.filtroEstados.includes(estado)
      ? this.filtroEstados.filter((item) => item !== estado)
      : [...this.filtroEstados, estado];
    this.buscarUnidades();
  }

  esPisoSeleccionado(piso: Piso): boolean {
    return this.filtroPisos.includes(piso);
  }

  togglePiso(piso: Piso): void {
    this.filtroPisos = this.filtroPisos.includes(piso)
      ? this.filtroPisos.filter((item) => item !== piso)
      : [...this.filtroPisos, piso];
    this.buscarUnidades();
  }

  onCodigoChange(valor: string): void {
    this.filtroCodigo = valor;
    if (this.codigoTimer) {
      clearTimeout(this.codigoTimer);
    }
    this.codigoTimer = setTimeout(() => {
      this.buscarUnidades();
    }, 250);
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

  esApartamento(unidad: UnidadDTO): boolean {
    return unidad.tipo === 'APARTAMENTO';
  }

  habitacionesNoDisponibles(unidad: UnidadDTO): number {
    return unidad.habitaciones.filter(
      (habitacion) => habitacion.estado !== 'DISPONIBLE'
    ).length;
  }

  buscarUnidades(): void {
    this.cargando = true;
    this.error = '';

    const estados = this.filtroEstados.length ? this.filtroEstados : undefined;
    const pisos = this.filtroPisos.length ? this.filtroPisos : undefined;
    const codigo = this.filtroCodigo.trim() || undefined;

    if (this.filtroTipo === 'HABITACION') {
      this.habitacionService.buscarHabitaciones({ estados, pisos, codigo }).subscribe({
        next: (habitaciones) => {
          this.unidades = this.mapearHabitaciones(habitaciones);
          this.cargando = false;
        },
        error: () => {
          this.error = 'No fue posible cargar las habitaciones.';
          this.cargando = false;
        },
      });
      return;
    }

    this.unidadService
      .buscarUnidades({
        tipo: this.filtroTipo || undefined,
        estados,
        pisos,
        codigo,
      })
      .subscribe({
        next: (unidades) => {
          this.unidades = unidades;
          this.cargando = false;
        },
        error: () => {
          this.error = 'No fue posible cargar las unidades.';
          this.cargando = false;
        },
      });
  }

  limpiarFiltros(): void {
    this.filtroTipo = '';
    this.filtroEstados = [];
    this.filtroPisos = [];
    this.filtroCodigo = '';
    this.buscarUnidades();
  }

  private mapearHabitaciones(habitaciones: HabitacionDTO[]): UnidadDTO[] {
    return habitaciones.map((habitacion) => ({
      id: habitacion.id,
      codigo: habitacion.codigo,
      tipo: 'HABITACION',
      piso: habitacion.piso,
      estado: habitacion.estado,
      habitaciones: [habitacion],
    }));
  }

}
