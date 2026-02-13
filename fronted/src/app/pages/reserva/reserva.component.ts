import { CommonModule } from '@angular/common';
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { FullCalendarModule } from '@fullcalendar/angular';
import dayGridPlugin from '@fullcalendar/daygrid';
import {
  CalendarOptions,
  DatesSetArg,
  DayCellContentArg,
  DayCellMountArg,
  EventInput,
} from '@fullcalendar/core';

import { ReservaService } from '../../services/reserva.service';
import { ReservaCalendarioDTO } from '../../models/reserva-calendario.model';
import { EstanciaCalendarioDTO, ReservaCalendarioResumenDTO } from '../../models/detalle-calendario.model';
import { EstadoEstancia, EstadoReserva, TipoUnidad } from '../../models/enums';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

type TipoRegistroCalendario = 'reserva' | 'estancia';

interface RegistroCalendarioDia {
  id: number;
  tipo: TipoRegistroCalendario;
  inicio: string;
  fin: string;
  codigoUnidad: string | null;
  numeroPersonas: number | null;
  estado: string;
}

@Component({
  selector: 'app-reserva',
  standalone: true,
  imports: [CommonModule, FormsModule, FullCalendarModule, RouterModule],
  templateUrl: './reserva.component.html',
  styleUrl: './reserva.component.css',
  encapsulation: ViewEncapsulation.None,
})
export class ReservaComponent implements OnInit {
  reservas: ReservaCalendarioDTO[] = [];
  estancias: EstanciaCalendarioDTO[] = [];
  reservaSeleccionada: ReservaCalendarioDTO | null = null;
  estanciaSeleccionada: EstanciaCalendarioDTO | null = null;
  cargando = false;
  error = '';
  modalBusquedaAbierta = false;
  busquedaDocumento = '';
  busquedaReservas: ReservaCalendarioDTO[] = [];
  busquedaCargando = false;
  busquedaError = '';
  busquedaHecha = false;

  private readonly maxUnidadesPorDia = 13;
  private registrosPorDia = new Map<string, Map<string, RegistroCalendarioDia>>();
  private filtroCodigoTimeout: ReturnType<typeof setTimeout> | null = null;

  filtroTipoUnidad: TipoUnidad | '' = '';
  filtroCodigo = '';
  filtroEstadosReserva: EstadoReserva[] = [];
  filtroEstadosEstancia: EstadoEstancia[] = [];

  mesActivo = this.formatearMes(new Date());
  eventos: EventInput[] = [];

  tiposUnidad = [
    { label: 'Todas', value: '' as const },
    { label: 'Unidad', value: 'APARTAMENTO' as TipoUnidad },
    { label: 'Apartaestudio', value: 'APARTAESTUDIO' as TipoUnidad },
    { label: 'Habitacion', value: 'HABITACION' as TipoUnidad },
  ];

  readonly estadosReserva: EstadoReserva[] = ['CONFIRMADA', 'CANCELADA', 'COMPLETADA', 'EXPIRADA'];
  readonly estadosEstancia: EstadoEstancia[] = [
    'ACTIVA',
    'RESERVADA',
    'FINALIZADA',
    'EXCEDIDA',
    'CANCELADA',
  ];

  calendarioOpciones: CalendarOptions = {
    plugins: [dayGridPlugin],
    initialView: 'dayGridMonth',
    fixedWeekCount: false,
    contentHeight: 'auto',
    eventDisplay: 'none',
    locale: 'es',
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: '',
    },
    eventClassNames: (arg) => {
      const estado = (arg.event.extendedProps as { estado?: EstadoReserva }).estado;
      return estado ? [`reserva-evento--${estado.toLowerCase()}`] : [];
    },
    dayCellContent: (arg: DayCellContentArg) => this.renderizarContenidoDia(arg),
    dayCellDidMount: (arg: DayCellMountArg) => this.onDayCellDidMount(arg),
    datesSet: (arg: DatesSetArg) => this.onDatesSet(arg),
  };

  constructor(
    private readonly reservaService: ReservaService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.cargarReservasMes(this.mesActivo);
  }

  onDatesSet(event: DatesSetArg): void {
    const fecha = event.view.currentStart;
    const nuevoMes = this.formatearMes(fecha);
    if (nuevoMes !== this.mesActivo) {
      this.mesActivo = nuevoMes;
      this.cargarReservasMes(this.mesActivo);
    }
  }

  aplicarFiltros(): void {
    this.cargarReservasMes(this.mesActivo);
  }

  onFiltroCodigoChange(): void {
    if (this.filtroCodigoTimeout) {
      clearTimeout(this.filtroCodigoTimeout);
    }

    this.filtroCodigoTimeout = setTimeout(() => {
      this.aplicarFiltros();
      this.filtroCodigoTimeout = null;
    }, 350);
  }

  limpiarFiltros(): void {
    this.filtroTipoUnidad = '';
    this.filtroCodigo = '';
    this.filtroEstadosReserva = [];
    this.filtroEstadosEstancia = [];
    this.cargarReservasMes(this.mesActivo);
  }

  toggleEstadoReserva(estado: EstadoReserva, checked: boolean): void {
    this.filtroEstadosReserva = checked
      ? Array.from(new Set([...this.filtroEstadosReserva, estado]))
      : this.filtroEstadosReserva.filter((item) => item !== estado);
    this.aplicarFiltros();
  }

  toggleEstadoEstancia(estado: EstadoEstancia, checked: boolean): void {
    this.filtroEstadosEstancia = checked
      ? Array.from(new Set([...this.filtroEstadosEstancia, estado]))
      : this.filtroEstadosEstancia.filter((item) => item !== estado);
    this.aplicarFiltros();
  }

  estaEstadoReservaSeleccionado(estado: EstadoReserva): boolean {
    return this.filtroEstadosReserva.includes(estado);
  }

  estaEstadoEstanciaSeleccionado(estado: EstadoEstancia): boolean {
    return this.filtroEstadosEstancia.includes(estado);
  }

  private cargarReservasMes(mes: string): void {
    this.cargando = true;
    this.error = '';

    const tipoUnidad = this.filtroTipoUnidad || undefined;
    const codigoUnidad = this.filtroCodigo.trim() || undefined;
    const estadosReserva = this.filtroEstadosReserva.length ? this.filtroEstadosReserva : undefined;
    const estadosEstancia = this.filtroEstadosEstancia.length ? this.filtroEstadosEstancia : undefined;

    this.reservaService
      .obtenerCalendario(mes, tipoUnidad, codigoUnidad, estadosReserva, estadosEstancia)
      .subscribe({
        next: (detalle) => {
          const reservas = this.mapearReservasCalendario(detalle.reservas ?? []);
          this.reservas = reservas;
          this.estancias = detalle.estancias ?? [];
          this.reservaSeleccionada = null;
          this.estanciaSeleccionada = null;
          this.registrosPorDia = this.construirMapaDias(reservas, this.estancias);
          this.eventos = this.construirEventos(reservas, this.estancias);
          this.calendarioOpciones = { ...this.calendarioOpciones };
          this.cargando = false;
        },
        error: (errorResponse: unknown) => {
          this.error = extractBackendErrorMessage(
            errorResponse,
            'No fue posible cargar las reservas del calendario.'
          );
          this.reservas = [];
          this.estancias = [];
          this.reservaSeleccionada = null;
          this.estanciaSeleccionada = null;
          this.registrosPorDia.clear();
          this.eventos = [];
          this.calendarioOpciones = { ...this.calendarioOpciones };
          this.cargando = false;
        },
      });
  }

  private mapearReservasCalendario(reservas: ReservaCalendarioResumenDTO[]): ReservaCalendarioDTO[] {
    return reservas.map((reserva) => ({
      id: reserva.id,
      idCliente: reserva.idCliente,
      codigoReserva: reserva.codigoReserva,
      inicio: reserva.inicio,
      fin: reserva.fin,
      estado: reserva.estadoReserva,
      codigoUnidad: reserva.codigoUnidad,
      tipoUnidad: reserva.tipoUnidad,
      numeroPersonas: reserva.numeroPersonas,
      nombreCliente: reserva.nombreCliente,
      totalAnticipo: reserva.totalAnticipo,
      idPagoReserva: null,
    }));
  }

  private construirEventos(
    reservas: ReservaCalendarioDTO[],
    estancias: EstanciaCalendarioDTO[],
  ): EventInput[] {
    const eventosReserva = reservas.reduce<EventInput[]>((eventos, reserva) => {
      const inicio = this.parsearFechaLocal(reserva.inicio);
      const fin = this.parsearFechaLocal(reserva.fin);
      if (!inicio || !fin) {
        return eventos;
      }

      const finInclusivo = new Date(fin.getFullYear(), fin.getMonth(), fin.getDate() + 1);

      eventos.push({
        id: `${reserva.id}`,
        title: `${reserva.codigoUnidad ?? 'Unidad'} (${reserva.numeroPersonas ?? 0})`,
        start: inicio,
        end: finInclusivo,
        allDay: true,
        extendedProps: {
          estado: reserva.estado,
        },
      });

      return eventos;
    }, []);

    const eventosEstancia = estancias.reduce<EventInput[]>((eventos, estancia) => {
      const inicio = this.parsearFechaLocal(estancia.inicio);
      const fin = this.parsearFechaLocal(estancia.fin);
      if (!inicio || !fin) {
        return eventos;
      }

      const finInclusivo = new Date(fin.getFullYear(), fin.getMonth(), fin.getDate() + 1);

      eventos.push({
        id: `estancia-${estancia.id}`,
        title: `${estancia.codigoUnidad ?? 'Unidad'} (${estancia.numeroPersonas ?? 0})`,
        start: inicio,
        end: finInclusivo,
        allDay: true,
      });

      return eventos;
    }, []);

    return [...eventosReserva, ...eventosEstancia];
  }

  private construirMapaDias(
    reservas: ReservaCalendarioDTO[],
    estancias: EstanciaCalendarioDTO[],
  ): Map<string, Map<string, RegistroCalendarioDia>> {
    const mapa = new Map<string, Map<string, RegistroCalendarioDia>>();

    reservas.forEach((reserva) => {
      const inicio = this.parsearFechaLocal(reserva.inicio);
      const fin = this.parsearFechaLocal(reserva.fin);
      if (!inicio || !fin) {
        return;
      }

      const fechaActual = new Date(inicio.getFullYear(), inicio.getMonth(), inicio.getDate());
      const fechaFinal = new Date(fin.getFullYear(), fin.getMonth(), fin.getDate());
      const claveRegistro = `reserva-${reserva.codigoUnidad ?? `ID-${reserva.id}`}-${reserva.id}`;

      while (fechaActual <= fechaFinal) {
        const clave = this.formatearDia(fechaActual);
        let mapaDia = mapa.get(clave);
        if (!mapaDia) {
          mapaDia = new Map<string, RegistroCalendarioDia>();
          mapa.set(clave, mapaDia);
        }

        if (!mapaDia.has(claveRegistro)) {
          mapaDia.set(claveRegistro, {
            id: reserva.id,
            tipo: 'reserva',
            inicio: reserva.inicio,
            fin: reserva.fin,
            codigoUnidad: reserva.codigoUnidad,
            numeroPersonas: reserva.numeroPersonas,
            estado: reserva.estado,
          });
        }

        fechaActual.setDate(fechaActual.getDate() + 1);
      }
    });

    estancias.forEach((estancia) => {
      const inicio = this.parsearFechaLocal(estancia.inicio);
      const fin = this.parsearFechaLocal(estancia.fin);
      if (!inicio || !fin) {
        return;
      }

      const fechaActual = new Date(inicio.getFullYear(), inicio.getMonth(), inicio.getDate());
      const fechaFinal = new Date(fin.getFullYear(), fin.getMonth(), fin.getDate());
      const claveRegistro = `estancia-${estancia.codigoUnidad ?? `ID-${estancia.id}`}-${estancia.id}`;

      while (fechaActual <= fechaFinal) {
        const clave = this.formatearDia(fechaActual);
        let mapaDia = mapa.get(clave);
        if (!mapaDia) {
          mapaDia = new Map<string, RegistroCalendarioDia>();
          mapa.set(clave, mapaDia);
        }

        if (!mapaDia.has(claveRegistro)) {
          mapaDia.set(claveRegistro, {
            id: estancia.id,
            tipo: 'estancia',
            inicio: estancia.inicio,
            fin: estancia.fin,
            codigoUnidad: estancia.codigoUnidad,
            numeroPersonas: estancia.numeroPersonas,
            estado: estancia.estadoEstancia,
          });
        }

        fechaActual.setDate(fechaActual.getDate() + 1);
      }
    });

    return mapa;
  }

  private renderizarContenidoDia(arg: DayCellContentArg): { html: string } {
    const clave = this.formatearDia(arg.date);
    const mapaDia = this.registrosPorDia.get(clave);
    const registrosDia = mapaDia ? Array.from(mapaDia.entries()) : [];
    registrosDia.sort((a, b) => a[0].localeCompare(b[0]));

    const ocupadas = registrosDia.slice(0, this.maxUnidadesPorDia);

    const circulosOcupados = ocupadas
      .map(([, registro]) => {
        const estadoClase = registro.estado.toLowerCase();
        const etiqueta = registro.codigoUnidad ?? `Unidad ${registro.id}`;
        const tipoClase = `reserva-circulo--${registro.tipo}`;
        const tipoLabel = registro.tipo === 'reserva' ? 'Reserva' : 'Estancia';
        const etiquetaEscapada = this.escaparHtml(etiqueta);
        const titulo = this.escaparHtml(
          `${tipoLabel} · ${etiqueta} · ${this.formatearEtiqueta(registro.estado)}`,
        );
        const dataReservaId = registro.tipo === 'reserva' ? ` data-reserva-id="${registro.id}"` : '';
        const dataEstanciaId =
          registro.tipo === 'estancia' ? ` data-estancia-id="${registro.id}"` : '';
        return `<span class="reserva-circulo ${tipoClase} reserva-circulo--${estadoClase}"${dataReservaId}${dataEstanciaId} title="${titulo}" aria-label="${titulo}">${etiquetaEscapada}</span>`;
      })
      .join('');

    if (!circulosOcupados) {
      return {
        html: `<div class="reserva-dia">
          <span class="reserva-dia-numero">${this.escaparHtml(arg.dayNumberText)}</span>
        </div>`,
      };
    }

    return {
      html: `<div class="reserva-dia">
        <span class="reserva-dia-numero">${this.escaparHtml(arg.dayNumberText)}</span>
        <div class="reserva-circulos">${circulosOcupados}</div>
      </div>`,
    };
  }

  private formatearMes(fecha: Date): string {
    const year = fecha.getFullYear();
    const month = `${fecha.getMonth() + 1}`.padStart(2, '0');
    return `${year}-${month}`;
  }

  private formatearDia(fecha: Date): string {
    const year = fecha.getFullYear();
    const month = `${fecha.getMonth() + 1}`.padStart(2, '0');
    const day = `${fecha.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private escaparHtml(valor: string): string {
    return valor
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  private parsearFechaLocal(valor: string | null | undefined): Date | null {
    if (!valor) {
      return null;
    }
    const [fecha] = valor.split('T');
    if (!fecha) {
      return null;
    }
    const [year, month, day] = fecha.split('-').map((parte) => Number.parseInt(parte, 10));
    if (!year || !month || !day) {
      return null;
    }
    return new Date(year, month - 1, day);
  }

  private onDayCellDidMount(arg: DayCellMountArg): void {
    arg.el.addEventListener('click', this.onDayCellClick);
  }

  private onDayCellClick = (event: MouseEvent): void => {
    const objetivo = event.target as HTMLElement | null;
    if (!objetivo) {
      return;
    }

    const circulo = objetivo.closest('.reserva-circulo') as HTMLElement | null;
    if (!circulo) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();

    const idReservaTexto = circulo.dataset['reservaId'];
    const idEstanciaTexto = circulo.dataset['estanciaId'];

    if (idReservaTexto) {
      const idReserva = Number.parseInt(idReservaTexto, 10);
      if (!Number.isFinite(idReserva)) {
        return;
      }
      const reserva = this.reservas.find((item) => item.id === idReserva);
      if (reserva) {
        this.abrirDetalle(reserva);
      }
      return;
    }

    if (idEstanciaTexto) {
      const idEstancia = Number.parseInt(idEstanciaTexto, 10);
      if (!Number.isFinite(idEstancia)) {
        return;
      }
      const estancia = this.estancias.find((item) => item.id === idEstancia);
      if (estancia) {
        this.abrirDetalleEstancia(estancia);
      }
    }
  };

  abrirDetalle(reserva: ReservaCalendarioDTO): void {
    this.reservaSeleccionada = reserva;
    this.estanciaSeleccionada = null;
  }

  abrirDetalleEstancia(estancia: EstanciaCalendarioDTO): void {
    this.estanciaSeleccionada = estancia;
    this.reservaSeleccionada = null;
  }

  cerrarDetalle(): void {
    this.reservaSeleccionada = null;
    this.estanciaSeleccionada = null;
  }

  mantenerModal(event: MouseEvent): void {
    event.stopPropagation();
  }

  abrirBusqueda(): void {
    this.modalBusquedaAbierta = true;
    this.busquedaError = '';
  }

  cerrarBusqueda(): void {
    this.modalBusquedaAbierta = false;
    this.busquedaDocumento = '';
    this.busquedaReservas = [];
    this.busquedaError = '';
    this.busquedaHecha = false;
    this.busquedaCargando = false;
  }

  buscarReservasPorDocumento(): void {
    const documento = this.busquedaDocumento.trim();
    if (!documento) {
      this.busquedaError = 'Ingresa el numero de documento para buscar.';
      this.busquedaReservas = [];
      this.busquedaHecha = true;
      return;
    }

    this.busquedaCargando = true;
    this.busquedaError = '';
    this.reservaService.buscarPorDocumento(documento).subscribe({
      next: (reservas) => {
        this.busquedaReservas = reservas;
        this.busquedaCargando = false;
        this.busquedaHecha = true;
      },
      error: (errorResponse: unknown) => {
        this.busquedaError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible buscar reservas con ese documento.'
        );
        this.busquedaReservas = [];
        this.busquedaCargando = false;
        this.busquedaHecha = true;
      },
    });
  }

  verMasReserva(reserva: ReservaCalendarioDTO): void {
    this.cerrarBusqueda();
    this.abrirDetalle(reserva);
  }

  darIngreso(reserva: ReservaCalendarioDTO): void {
    this.cerrarDetalle();
    this.cerrarBusqueda();
    this.router.navigate(['/estancias/nueva'], {
      state: {
        idReserva: reserva.id,
        idCliente: reserva.idCliente && reserva.idCliente > 0 ? reserva.idCliente : undefined,
        idPagoReserva: reserva.idPagoReserva ?? undefined,
        nombreCliente: reserva.nombreCliente ?? '',
        codigo: reserva.codigoUnidad ?? '',
        tipo: reserva.tipoUnidad ?? '',
        entrada: reserva.inicio ?? '',
        salida: reserva.fin ?? '',
      },
    });
  }

  formatearFecha(valor: string | null | undefined): string {
    const fecha = this.parsearFechaLocal(valor);
    if (!fecha) {
      return '-';
    }
    return fecha.toLocaleDateString('es-CO', {
      year: 'numeric',
      month: 'long',
      day: '2-digit',
    });
  }

  formatearEtiqueta(valor: string | null | undefined): string {
    if (!valor) {
      return '-';
    }
    const texto = valor.replace(/_/g, ' ').toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }
}
