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
import { EstadoReserva, TipoUnidad } from '../../models/enums';

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
  reservaSeleccionada: ReservaCalendarioDTO | null = null;
  cargando = false;
  error = '';
  modalBusquedaAbierta = false;
  busquedaDocumento = '';
  busquedaReservas: ReservaCalendarioDTO[] = [];
  busquedaCargando = false;
  busquedaError = '';
  busquedaHecha = false;
  private readonly maxUnidadesPorDia = 13;
  private reservasPorDia = new Map<string, Map<string, ReservaCalendarioDTO>>();

  filtroTipoUnidad: TipoUnidad | '' = '';
  filtroCodigo = '';

  mesActivo = this.formatearMes(new Date());
  eventos: EventInput[] = [];

  tiposUnidad = [
    { label: 'Todas', value: '' as const },
    { label: 'Unidad', value: 'APARTAMENTO' as TipoUnidad },
    { label: 'Apartaestudio', value: 'APARTAESTUDIO' as TipoUnidad },
    { label: 'Habitacion', value: 'HABITACION' as TipoUnidad },
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
    private readonly router: Router
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

  limpiarFiltros(): void {
    this.filtroTipoUnidad = '';
    this.filtroCodigo = '';
    this.cargarReservasMes(this.mesActivo);
  }

  private cargarReservasMes(mes: string): void {
    this.cargando = true;
    this.error = '';

    const tipoUnidad = this.filtroTipoUnidad || undefined;
    const codigoUnidad = this.filtroCodigo.trim() || undefined;

    this.reservaService.obtenerReservasCalendario(mes, tipoUnidad, codigoUnidad).subscribe({
      next: (reservas) => {
        this.reservas = reservas;
        this.reservasPorDia = this.construirMapaDias(reservas);
        this.eventos = this.construirEventos(reservas);
        this.calendarioOpciones = { ...this.calendarioOpciones };
        this.cargando = false;
      },
      error: () => {
        this.error = 'No fue posible cargar las reservas del calendario.';
        this.reservas = [];
        this.reservasPorDia.clear();
        this.eventos = [];
        this.calendarioOpciones = { ...this.calendarioOpciones };
        this.cargando = false;
      },
    });
  }

  private construirEventos(reservas: ReservaCalendarioDTO[]): EventInput[] {
    return reservas.reduce<EventInput[]>((eventos, reserva) => {
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
  }

  private construirMapaDias(
    reservas: ReservaCalendarioDTO[],
  ): Map<string, Map<string, ReservaCalendarioDTO>> {
    const mapa = new Map<string, Map<string, ReservaCalendarioDTO>>();

    reservas.forEach((reserva) => {
      const inicio = this.parsearFechaLocal(reserva.inicio);
      const fin = this.parsearFechaLocal(reserva.fin);
      if (!inicio || !fin) {
        return;
      }

      const fechaActual = new Date(inicio.getFullYear(), inicio.getMonth(), inicio.getDate());
      const fechaFinal = new Date(fin.getFullYear(), fin.getMonth(), fin.getDate());
      const claveUnidad = reserva.codigoUnidad ?? `ID-${reserva.id}`;

      while (fechaActual <= fechaFinal) {
        const clave = this.formatearDia(fechaActual);
        let mapaDia = mapa.get(clave);
        if (!mapaDia) {
          mapaDia = new Map<string, ReservaCalendarioDTO>();
          mapa.set(clave, mapaDia);
        }

        if (!mapaDia.has(claveUnidad)) {
          mapaDia.set(claveUnidad, reserva);
        }

        fechaActual.setDate(fechaActual.getDate() + 1);
      }
    });

    return mapa;
  }

  private renderizarContenidoDia(arg: DayCellContentArg): { html: string } {
    const clave = this.formatearDia(arg.date);
    const mapaDia = this.reservasPorDia.get(clave);
    const reservasDia = mapaDia ? Array.from(mapaDia.entries()) : [];
    reservasDia.sort((a, b) => a[0].localeCompare(b[0]));

    const ocupadas = reservasDia.slice(0, this.maxUnidadesPorDia);

    const circulosOcupados = ocupadas
      .map(([codigo, reserva]) => {
        const slot = this.obtenerSlotCodigo(codigo);
        const estadoClase = reserva.estado.toLowerCase();
        const etiqueta = reserva.codigoUnidad ?? `Unidad ${reserva.id}`;
        const etiquetaEscapada = this.escaparHtml(etiqueta);
        const titulo = this.escaparHtml(
          `${etiqueta} · ${this.formatearEtiqueta(reserva.estado)}`,
        );
        return `<span class="reserva-circulo reserva-circulo--slot-${slot} reserva-circulo--${estadoClase}" data-reserva-id="${reserva.id}" title="${titulo}" aria-label="${titulo}">${etiquetaEscapada}</span>`;
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

  private obtenerSlotCodigo(codigo: string): number {
    let hash = 0;
    for (let i = 0; i < codigo.length; i += 1) {
      hash = (hash * 31 + codigo.charCodeAt(i)) % this.maxUnidadesPorDia;
    }
    return hash;
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

    const idTexto = circulo.dataset['reservaId'];
    const id = idTexto ? Number.parseInt(idTexto, 10) : Number.NaN;
    if (!Number.isFinite(id)) {
      return;
    }

    const reserva = this.reservas.find((item) => item.id === id);
    if (reserva) {
      this.abrirDetalle(reserva);
    }
  };

  abrirDetalle(reserva: ReservaCalendarioDTO): void {
    this.reservaSeleccionada = reserva;
  }

  cerrarDetalle(): void {
    this.reservaSeleccionada = null;
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
      error: () => {
        this.busquedaError = 'No fue posible buscar reservas con ese documento.';
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
    console.log('reserva', reserva);
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
