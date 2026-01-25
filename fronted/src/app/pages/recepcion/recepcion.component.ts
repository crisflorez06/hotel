import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { HabitacionDTO } from '../../models/habitacion.model';
import { UnidadDTO } from '../../models/unidad.model';
import { HabitacionService } from '../../services/habitacion.service';
import { EstanciaService } from '../../services/estancia.service';
import { UnidadService } from '../../services/unidad.service';
import { EstadoOperativo, Piso, TipoUnidad } from '../../models/enums';
import { EstanciaDTO } from '../../models/estancia-detalle.model';

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
  unidadSeleccionada: UnidadDTO | null = null;
  estanciaDetalle: EstanciaDTO | null = null;
  estanciaCargando = false;
  estanciaError = '';
  eliminandoEstancia = false;
  eliminarError = '';

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
    private readonly habitacionService: HabitacionService,
    private readonly estanciaService: EstanciaService
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

  verEstancia(unidad: UnidadDTO): void {
    this.estanciaCargando = true;
    this.estanciaError = '';
    this.estanciaDetalle = null;
    this.eliminarError = '';

    this.estanciaService.obtenerEstanciaActiva(unidad.codigo, unidad.tipo).subscribe({
      next: (estancia) => {
        this.estanciaDetalle = estancia;
        this.estanciaCargando = false;
      },
      error: () => {
        this.estanciaError = 'No hay estancia activa para esta unidad.';
        this.estanciaCargando = false;
      },
    });
  }

  abrirDetalle(unidad: UnidadDTO): void {
    this.unidadSeleccionada = unidad;
    this.estanciaDetalle = null;
    this.estanciaError = '';
    this.eliminarError = '';
  }

  cerrarDetalle(): void {
    this.unidadSeleccionada = null;
    this.estanciaDetalle = null;
    this.estanciaError = '';
    this.eliminarError = '';
  }

  mantenerModal(event: MouseEvent): void {
    event.stopPropagation();
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

  eliminarEstancia(): void {
    if (!this.estanciaDetalle || this.eliminandoEstancia) {
      return;
    }

    const confirmar = window.confirm('¿Seguro que deseas eliminar la estancia activa?');
    if (!confirmar) {
      return;
    }

    this.eliminandoEstancia = true;
    this.eliminarError = '';

    this.estanciaService.eliminarEstancia(this.estanciaDetalle.id).subscribe({
      next: () => {
        this.estanciaDetalle = null;
        this.eliminandoEstancia = false;
        this.buscarUnidades();
      },
      error: () => {
        this.eliminandoEstancia = false;
        this.eliminarError = 'No fue posible eliminar la estancia.';
      },
    });
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
