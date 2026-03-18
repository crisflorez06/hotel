import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import {
  EntidadMonitor,
  EventoMonitorFiltros,
  EventoMonitorItem,
  TipoEventoMonitor,
} from '../../models/evento-monitor.model';
import { EventoService } from '../../services/evento.service';

interface DetalleCampoVisual {
  etiqueta: string;
  valor?: string;
  anterior?: string;
  nuevo?: string;
}

interface GrupoTipoEvento {
  titulo: string;
  tipos: readonly TipoEventoMonitor[];
}

@Component({
  selector: 'app-monitor-eventos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './monitor-eventos.component.html',
  styleUrl: './monitor-eventos.component.css',
})
export class MonitorEventosComponent implements OnInit {
  private readonly tiposEventoCriticos: readonly TipoEventoMonitor[] = [
    'MODIFICACION_ESTANCIA',
    'ELIMINACION_ESTANCIA',
  ];
  private readonly entidadesCriticas: readonly EntidadMonitor[] = ['PAGO', 'TARIFA_BASE', 'AJUSTE_TEMPORADA'];
  private readonly entidadesNavegables = new Set<EntidadMonitor>(['RESERVA', 'ESTANCIA', 'PAGO']);

  private readonly etiquetasDetalle: Record<string, string> = {
    id: 'ID',
    idEntidad: 'ID entidad',
    codigoReserva: 'Codigo reserva',
    codigoEstancia: 'Codigo estancia',
    codigoFolio: 'Codigo folio',
    numeroDocumento: 'Numero documento',
    numeroPersonas: 'Numero de personas',
    entradaReal: 'Entrada real',
    salidaReal: 'Salida real',
    entradaEstimada: 'Entrada estimada',
    salidaEstimada: 'Salida estimada',
    fechaCreacion: 'Fecha de creacion',
    fechaModificacion: 'Fecha de modificacion',
    fecha: 'Fecha',
    estado: 'Estado',
    estadoOperativo: 'Estado operativo',
    tipoEvento: 'Tipo de evento',
    tipoPago: 'Tipo de pago',
    medioPago: 'Medio de pago',
    monto: 'Monto',
    montoEstimado: 'Cobro sugerido',
    precioTotal: 'Precio total',
    notas: 'Notas',
    cliente: 'Cliente',
    ocupantes: 'Ocupantes',
    habitaciones: 'Habitaciones',
    unidad: 'Unidad',
    reserva: 'Reserva',
    estancia: 'Estancia',
    anterior: 'Anterior',
    nuevo: 'Nuevo',
  };

  readonly gruposTipoEvento: readonly GrupoTipoEvento[] = [
    {
      titulo: 'Acciones de estancia',
      tipos: ['CREACION_ESTANCIA', 'ACTIVACION_ESTANCIA', 'MODIFICACION_ESTANCIA', 'ELIMINACION_ESTANCIA', 'FINALIZACION_ESTANCIA'],
    },
    {
      titulo: 'Acciones de reserva',
      tipos: ['CREACION_RESERVA', 'MODIFICACION_RESERVA', 'ELIMINACION_RESERVA'],
    },
    {
      titulo: 'Acciones de pago',
      tipos: ['CREACION_PAGO', 'CREACION_RECARGO', 'MODIFICACION_PAGO', 'ELIMINACION_PAGO'],
    },
    {
      titulo: 'Acciones de configuracion',
      tipos: ['MODIFICACION_TARIFA_BASE', 'MODIFICACION_TEMPORADA_ACTIVA'],
    },
  ];

  readonly entidadesDisponibles: EntidadMonitor[] = ['ESTANCIA', 'RESERVA', 'PAGO', 'TARIFA_BASE', 'AJUSTE_TEMPORADA'];
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

  activarEventosCriticos(): void {
    this.filtros.tiposEvento = [...this.tiposEventoCriticos];
    this.filtros.entidades = [...this.entidadesCriticas];
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
    this.filtros.tiposEvento = this.toggleEnLista(this.filtros.tiposEvento, tipo);
  }

  toggleEntidad(entidad: EntidadMonitor): void {
    this.filtros.entidades = this.toggleEnLista(this.filtros.entidades, entidad);
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

  puedeVerEnTabla(evento: EventoMonitorItem | null): boolean {
    if (!evento) {
      return false;
    }
    return (
      this.entidadesNavegables.has(evento.entidad) &&
      Number.isInteger(evento.idEntidad) &&
      evento.idEntidad > 0
    );
  }

  irATablaEntidad(evento: EventoMonitorItem | null): void {
    if (!this.puedeVerEnTabla(evento) || !evento) {
      return;
    }

    if (evento.entidad === 'RESERVA') {
      this.router.navigate(['/reservas'], { queryParams: { idReserva: evento.idEntidad } });
      return;
    }
    if (evento.entidad === 'ESTANCIA') {
      this.router.navigate(['/estancias'], { queryParams: { idEstancia: evento.idEntidad } });
      return;
    }
    this.router.navigate(['/pagos'], { queryParams: { idPago: evento.idEntidad } });
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

    this.eventoService
      .obtenerEventos(filtrosConsulta, pagina, this.tamanoPagina)
      .pipe(finalize(() => (this.cargando = false)))
      .subscribe({
        next: (response) => {
          this.eventos = response.content;
          this.paginaActual = response.number;
          this.totalPaginas = response.totalPages;
          this.totalElementos = response.totalElements;
        },
        error: (errorResponse: unknown) => {
          this.eventos = [];
          this.error = extractBackendErrorMessage(errorResponse, 'No fue posible cargar los eventos.');
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
      fechaDesde: this.formatearFechaFiltro(this.filtros.fechaDesde, 'desde'),
      fechaHasta: this.formatearFechaFiltro(this.filtros.fechaHasta, 'hasta'),
    };
  }

  private formatearFechaFiltro(fecha: string | undefined, limite: 'desde' | 'hasta'): string {
    const fechaTrimmed = fecha?.trim() ?? '';
    if (!fechaTrimmed) {
      return '';
    }

    return limite === 'desde' ? `${fechaTrimmed}T00:00:00` : `${fechaTrimmed}T23:59:59`;
  }

  private toggleEnLista<T>(lista: readonly T[], valor: T): T[] {
    return lista.includes(valor) ? lista.filter((item) => item !== valor) : [...lista, valor];
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
          etiqueta: this.formatearEtiquetaDetalle(etiqueta),
          anterior: cambio.anterior,
          nuevo: cambio.nuevo,
        };
      }

      return {
        etiqueta: this.formatearEtiquetaDetalle(etiqueta),
        valor: valorRaw || '-',
      };
    });

    return campos.length ? campos : [{ etiqueta: 'Detalle', valor: detalleTrimmed }];
  }

  private mapearObjetoACampos(objeto: Record<string, unknown>): DetalleCampoVisual[] {
    const entriesOrdenadas = [...Object.entries(objeto)].sort(([claveA], [claveB]) => {
      return this.obtenerPrioridadCampoDetalle(claveA) - this.obtenerPrioridadCampoDetalle(claveB);
    });

    return entriesOrdenadas.map(([clave, valor]) => {
      const cambio = this.parsearCambioDesdeValorRaw(valor);

      if (cambio) {
        return {
          etiqueta: this.formatearEtiquetaDetalle(clave),
          anterior: cambio.anterior,
          nuevo: cambio.nuevo,
        };
      }

      return {
        etiqueta: this.formatearEtiquetaDetalle(clave),
        valor: this.formatearValorDetalle(valor),
      };
    });
  }

  private obtenerPrioridadCampoDetalle(clave: string): number {
    if (clave === 'montoEstimado') {
      return 0;
    }
    if (clave === 'monto') {
      return 1;
    }
    return 10;
  }

  private formatearEtiquetaDetalle(clave: string): string {
    const claveTrimmed = clave.trim();
    if (!claveTrimmed) {
      return 'Detalle';
    }

    const etiquetaDirecta = this.etiquetasDetalle[claveTrimmed];
    if (etiquetaDirecta) {
      return etiquetaDirecta;
    }

    const claveNormalizada = claveTrimmed
      .replace(/\./g, ' ')
      .replace(/_/g, ' ')
      .replace(/-/g, ' ')
      .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      .replace(/\s+/g, ' ')
      .trim();

    const etiquetaNormalizada = this.etiquetasDetalle[claveNormalizada];
    if (etiquetaNormalizada) {
      return etiquetaNormalizada;
    }

    return claveNormalizada
      .toLowerCase()
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
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
