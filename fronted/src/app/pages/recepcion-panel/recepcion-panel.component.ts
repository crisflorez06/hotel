import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { HabitacionDTO } from '../../models/habitacion.model';
import { UnidadDTO } from '../../models/unidad.model';
import { EstanciaDTO } from '../../models/estancia-detalle.model';
import { EstadoOperativo, Piso, TipoUnidad } from '../../models/enums';
import { HabitacionService } from '../../services/habitacion.service';
import { UnidadService } from '../../services/unidad.service';
import { EstanciaService } from '../../services/estancia.service';

@Component({
  selector: 'app-recepcion-panel',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './recepcion-panel.component.html',
  styleUrl: './recepcion-panel.component.css',
})
export class RecepcionPanelComponent implements OnInit {
  unidad: UnidadDTO | null = null;
  cargando = true;
  error = '';

  estanciaDetalle: EstanciaDTO | null = null;
  estanciaCargando = false;
  estanciaError = '';
  eliminandoEstancia = false;
  eliminarError = '';
  mostrarPagoReservaDetalle = false;
  mostrarPagoEstanciaDetalle = false;

  private codigo = '';
  private tipo: TipoUnidad | '' = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly unidadService: UnidadService,
    private readonly habitacionService: HabitacionService,
    private readonly estanciaService: EstanciaService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.codigo = (params['codigo'] as string | undefined)?.trim() ?? '';
      this.tipo = (params['tipo'] as TipoUnidad | undefined) ?? '';
      this.cargarUnidad();
    });
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

  resumenOcupacion(unidad: UnidadDTO): string {
    const ocupadas = unidad.habitaciones.filter((habitacion) => habitacion.estado !== 'DISPONIBLE')
      .length;
    return `${ocupadas} de ${unidad.habitaciones.length} habitaciones`;
  }

  abrirHabitacion(habitacion: HabitacionDTO): void {
    this.router.navigate(['/recepcion/panel'], {
      queryParams: {
        codigo: habitacion.codigo,
        tipo: 'HABITACION',
      },
    });
  }

  verEstancia(unidad: UnidadDTO): void {
    this.estanciaCargando = true;
    this.estanciaError = '';
    this.estanciaDetalle = null;
    this.eliminarError = '';
    this.mostrarPagoReservaDetalle = false;
    this.mostrarPagoEstanciaDetalle = false;

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
      },
      error: () => {
        this.eliminandoEstancia = false;
        this.eliminarError = 'No fue posible eliminar la estancia.';
      },
    });
  }

  darSalida(): void {
    if (!this.estanciaDetalle || !this.unidad) {
      return;
    }
    this.router.navigate(['/estancias/salida'], {
      state: {
        estanciaId: this.estanciaDetalle.id,
        codigo: this.unidad.codigo,
        tipo: this.unidad.tipo,
        entrada: this.estanciaDetalle.entradaReal,
        salida: this.estanciaDetalle.salidaEstimada,
        numeroPersonas: this.estanciaDetalle.ocupantes.length,
        idPagoEstancia: this.estanciaDetalle.pagoEstancia?.id ?? null,
      },
    });
  }

  togglePagoReservaDetalle(): void {
    this.mostrarPagoReservaDetalle = !this.mostrarPagoReservaDetalle;
  }

  togglePagoEstanciaDetalle(): void {
    this.mostrarPagoEstanciaDetalle = !this.mostrarPagoEstanciaDetalle;
  }

  formatearFecha(fecha: string): string {
    const fechaObj = new Date(fecha);
    return Number.isNaN(fechaObj.getTime()) ? fecha : fechaObj.toLocaleString('es-CO');
  }

  private cargarUnidad(): void {
    this.cargando = true;
    this.error = '';
    this.unidad = null;
    this.estanciaDetalle = null;
    this.estanciaError = '';

    if (!this.codigo) {
      this.error = 'No se recibió el código de la unidad.';
      this.cargando = false;
      return;
    }

    if (this.tipo === 'HABITACION') {
      this.habitacionService.buscarHabitaciones({ codigo: this.codigo }).subscribe({
        next: (habitaciones) => {
          const unidad = this.mapearHabitaciones(habitaciones).find(
            (item) => item.codigo === this.codigo
          );
          if (!unidad) {
            this.error = 'No se encontró la habitación solicitada.';
          } else {
            this.unidad = unidad;
            if (this.esNoDisponible(unidad.estado)) {
              this.verEstancia(unidad);
            }
          }
          this.cargando = false;
        },
        error: () => {
          this.error = 'No fue posible cargar la habitación.';
          this.cargando = false;
        },
      });
      return;
    }

    this.unidadService
      .buscarUnidades({
        tipo: this.tipo || undefined,
        codigo: this.codigo,
      })
      .subscribe({
        next: (unidades) => {
          if (!unidades.length) {
            this.error = 'No se encontró la unidad solicitada.';
          } else {
            this.unidad = unidades[0];
          }
          this.cargando = false;
        },
        error: () => {
          this.error = 'No fue posible cargar la unidad.';
          this.cargando = false;
        },
      });
  }

  private mapearHabitaciones(habitaciones: HabitacionDTO[]): UnidadDTO[] {
    return habitaciones.map((habitacion) => ({
      id: habitacion.id,
      codigo: habitacion.codigo,
      tipo: 'HABITACION',
      piso: habitacion.piso as Piso,
      estado: habitacion.estado as EstadoOperativo,
      habitaciones: [habitacion],
    }));
  }
}
