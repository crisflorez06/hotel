import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import {
  EntidadMonitor,
  EventoMonitorFiltros,
  EventoMonitorItem,
  TipoEventoMonitor,
} from '../../models/evento-monitor.model';
import { EventoService } from '../../services/evento.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

interface DetalleCampoVisual {
  etiqueta: string;
  valor?: string;
  anterior?: string;
  nuevo?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  readonly tiposEventoDisponibles: TipoEventoMonitor[] = [
    'CREACION_RESERVA',
    'MODIFICACION_RESERVA',
    'ELIMINACION_RESERVA',
    'CREACION_ESTANCIA',
    'ACTIVACION_ESTANCIA',
    'MODIFICACION_ESTANCIA',
    'ELIMINACION_ESTANCIA',
    'FINALIZACION_ESTANCIA',
    'CREACION_PAGO',
    'MODIFICACION_PAGO',
    'ELIMINACION_PAGO',
  ];
  readonly tiposEventoEstancia: TipoEventoMonitor[] = [
    'CREACION_ESTANCIA',
    'ACTIVACION_ESTANCIA',
    'MODIFICACION_ESTANCIA',
    'ELIMINACION_ESTANCIA',
    'FINALIZACION_ESTANCIA',
  ];
  readonly tiposEventoReserva: TipoEventoMonitor[] = [
    'CREACION_RESERVA',
    'MODIFICACION_RESERVA',
    'ELIMINACION_RESERVA',
  ];
  readonly tiposEventoPago: TipoEventoMonitor[] = [
    'CREACION_PAGO',
    'MODIFICACION_PAGO',
    'ELIMINACION_PAGO',
  ];

  readonly entidadesDisponibles: EntidadMonitor[] = ['ESTANCIA', 'RESERVA', 'PAGO'];
  readonly tamanoPagina = 20;

  filtros: EventoMonitorFiltros = this.crearFiltrosVacios();

  eventos: EventoMonitorItem[] = [];
  cargando = false;
  error = '';

  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  modalAbierto = false;
  eventoDetalleSeleccionado: EventoMonitorItem | null = null;
  detalleSeleccionadoCampos: DetalleCampoVisual[] = [];

  constructor(
    private readonly eventoService: EventoService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.cargarEventos(0);
  }

  aplicarFiltros(): void {
    this.cargarEventos(0);
  }

  limpiarFiltros(): void {
    this.filtros = this.crearFiltrosVacios();
    this.cargarEventos(0);
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0 || this.cargando) {
      return;
    }
    this.cargarEventos(this.paginaActual - 1);
  }

  irPaginaSiguiente(): void {
    if (this.paginaActual >= this.totalPaginas - 1 || this.cargando) {
      return;
    }
    this.cargarEventos(this.paginaActual + 1);
  }

  toggleTipoEvento(tipo: TipoEventoMonitor): void {
    if (this.filtros.tiposEvento.includes(tipo)) {
      this.filtros.tiposEvento = this.filtros.tiposEvento.filter((item) => item !== tipo);
      return;
    }

    this.filtros.tiposEvento = [...this.filtros.tiposEvento, tipo];
  }

  toggleEntidad(entidad: EntidadMonitor): void {
    if (this.filtros.entidades.includes(entidad)) {
      this.filtros.entidades = this.filtros.entidades.filter((item) => item !== entidad);
      return;
    }

    this.filtros.entidades = [...this.filtros.entidades, entidad];
  }

  estaTipoEventoSeleccionado(tipo: TipoEventoMonitor): boolean {
    return this.filtros.tiposEvento.includes(tipo);
  }

  estaEntidadSeleccionada(entidad: EntidadMonitor): boolean {
    return this.filtros.entidades.includes(entidad);
  }

  formatearEtiqueta(valor: string | null | undefined): string {
    if (!valor) {
      return '-';
    }

    return valor
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
  }

  formatearFecha(fecha: string): string {
    const fechaObj = new Date(fecha);
    return Number.isNaN(fechaObj.getTime()) ? fecha : fechaObj.toLocaleString('es-CO');
  }

  irATablaReservas(evento: EventoMonitorItem): void {
    const codigoReserva = evento.codigoReserva?.trim() ?? '';
    if (!codigoReserva) {
      return;
    }

    this.router.navigate(['/reservas'], {
      queryParams: { codigoReserva },
    });
  }

  irATablaEstancias(evento: EventoMonitorItem): void {
    const codigoEstancia = evento.codigoEstancia?.trim() ?? '';
    if (!codigoEstancia) {
      return;
    }

    this.router.navigate(['/estancias'], {
      queryParams: { codigoEstancia },
    });
  }

  abrirModalDetalle(evento: EventoMonitorItem): void {
    this.eventoDetalleSeleccionado = evento;
    this.detalleSeleccionadoCampos = this.construirCamposDetalle(evento.detalle);
    this.modalAbierto = true;
  }

  cerrarModalDetalle(): void {
    this.modalAbierto = false;
    this.eventoDetalleSeleccionado = null;
    this.detalleSeleccionadoCampos = [];
  }

  trackByEventoId(_: number, evento: EventoMonitorItem): number {
    return evento.id;
  }

  get indiceInicio(): number {
    return this.totalElementos === 0 ? 0 : this.paginaActual * this.tamanoPagina + 1;
  }

  get indiceFin(): number {
    return Math.min((this.paginaActual + 1) * this.tamanoPagina, this.totalElementos);
  }

  private cargarEventos(pagina: number): void {
    this.cargando = true;
    this.error = '';
    const filtrosConsulta = this.construirFiltrosConsulta();

    this.eventoService.obtenerEventos(filtrosConsulta, pagina, this.tamanoPagina).subscribe({
      next: (response) => {
        this.eventos = response.content;
        this.paginaActual = response.number;
        this.totalPaginas = response.totalPages;
        this.totalElementos = response.totalElements;
        this.cargando = false;
      },
      error: (errorResponse: unknown) => {
        this.eventos = [];
        this.error = extractBackendErrorMessage(errorResponse, 'No fue posible cargar los eventos.');
        this.cargando = false;
      },
    });
  }

  private crearFiltrosVacios(): EventoMonitorFiltros {
    return {
      tiposEvento: [],
      entidades: [],
      codigoReserva: '',
      codigoEstancia: '',
      fechaDesde: '',
      fechaHasta: '',
    };
  }

  private construirFiltrosConsulta(): EventoMonitorFiltros {
    return {
      ...this.filtros,
      codigoReserva: this.filtros.codigoReserva?.trim() ?? '',
      codigoEstancia: this.filtros.codigoEstancia?.trim() ?? '',
      fechaDesde: this.formatearFechaInicioDia(this.filtros.fechaDesde),
      fechaHasta: this.formatearFechaFinDia(this.filtros.fechaHasta),
    };
  }

  private formatearFechaInicioDia(fecha?: string): string {
    const fechaTrimmed = fecha?.trim() ?? '';
    if (!fechaTrimmed) {
      return '';
    }

    return `${fechaTrimmed}T00:00:00`;
  }

  private formatearFechaFinDia(fecha?: string): string {
    const fechaTrimmed = fecha?.trim() ?? '';
    if (!fechaTrimmed) {
      return '';
    }

    return `${fechaTrimmed}T23:59:59`;
  }

  private parsearDetalle(detalle: unknown): Record<string, unknown> | null {
    if (!detalle) {
      return null;
    }

    if (typeof detalle === 'object' && !Array.isArray(detalle)) {
      return detalle as Record<string, unknown>;
    }

    if (typeof detalle !== 'string') {
      return null;
    }

    const detalleTrimmed = detalle.trim();
    if (!detalleTrimmed) {
      return null;
    }

    try {
      const parsed = JSON.parse(detalleTrimmed) as unknown;
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return null;
      }

      return parsed as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private formatearValorDetalle(valor: unknown): string {
    if (valor === null || valor === undefined) {
      return '-';
    }

    if (typeof valor === 'string') {
      return this.formatearFechaDetalleSiAplica(valor);
    }

    if (typeof valor === 'number' || typeof valor === 'boolean') {
      return String(valor);
    }

    try {
      return JSON.stringify(valor);
    } catch {
      return String(valor);
    }
  }

  private formatearFechaDetalleSiAplica(valor: string): string {
    const valorTrimmed = valor.trim();
    if (!valorTrimmed) {
      return '-';
    }

    const isoDateTimePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,6})?)?(Z|[+\-]\d{2}:\d{2})?$/;
    if (!isoDateTimePattern.test(valorTrimmed)) {
      return valorTrimmed;
    }

    const parsed = new Date(valorTrimmed);
    if (Number.isNaN(parsed.getTime())) {
      return valorTrimmed;
    }

    return parsed.toLocaleString('es-CO');
  }

  private construirCamposDetalle(detalle: unknown): DetalleCampoVisual[] {
    const detalleJson = this.parsearDetalle(detalle);
    if (detalleJson) {
      return this.mapearObjetoACampos(detalleJson);
    }

    if (typeof detalle !== 'string') {
      return [{ etiqueta: 'Detalle', valor: '-' }];
    }

    const detalleTrimmed = detalle.trim();
    if (!detalleTrimmed) {
      return [{ etiqueta: 'Detalle', valor: '-' }];
    }

    const segmentos = detalleTrimmed
      .split('|')
      .map((segmento) => segmento.trim())
      .filter((segmento) => !!segmento);

    if (!segmentos.length) {
      return [{ etiqueta: 'Detalle', valor: detalleTrimmed }];
    }

    const campos = segmentos.map((segmento) => {
      const indexSeparador = segmento.indexOf(':');
      if (indexSeparador < 0) {
        return {
          etiqueta: 'Detalle',
          valor: segmento,
        };
      }

      const etiqueta = segmento.slice(0, indexSeparador).trim();
      const valorRaw = segmento.slice(indexSeparador + 1).trim();
      const cambio = this.parsearCambioDesdeValorRaw(valorRaw);

      if (cambio) {
        return {
          etiqueta: this.formatearEtiqueta(etiqueta),
          anterior: cambio.anterior,
          nuevo: cambio.nuevo,
        };
      }

      return {
        etiqueta: this.formatearEtiqueta(etiqueta),
        valor: valorRaw || '-',
      };
    });

    return campos.length ? campos : [{ etiqueta: 'Detalle', valor: detalleTrimmed }];
  }

  private mapearObjetoACampos(objeto: Record<string, unknown>): DetalleCampoVisual[] {
    return Object.entries(objeto).map(([clave, valor]) => {
      const cambio = this.parsearCambioDesdeValorRaw(valor);

      if (cambio) {
        return {
          etiqueta: this.formatearEtiqueta(clave),
          anterior: cambio.anterior,
          nuevo: cambio.nuevo,
        };
      }

      return {
        etiqueta: this.formatearEtiqueta(clave),
        valor: this.formatearValorDetalle(valor),
      };
    });
  }

  private parsearCambioDesdeValorRaw(valor: unknown): { anterior: string; nuevo: string } | null {
    let valorObj: unknown = valor;

    if (typeof valor === 'string') {
      const valorTrimmed = valor.trim();
      if (!valorTrimmed) {
        return null;
      }

      if (valorTrimmed.startsWith('{') && valorTrimmed.endsWith('}')) {
        try {
          valorObj = JSON.parse(valorTrimmed) as unknown;
        } catch {
          valorObj = valor;
        }
      }
    }

    if (!valorObj || typeof valorObj !== 'object' || Array.isArray(valorObj)) {
      return null;
    }

    const obj = valorObj as Record<string, unknown>;
    if (!('nuevo' in obj) && !('anterior' in obj)) {
      return null;
    }

    return {
      anterior: this.formatearValorDetalle(obj['anterior']),
      nuevo: this.formatearValorDetalle(obj['nuevo']),
    };
  }
}
