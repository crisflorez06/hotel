import { CommonModule } from '@angular/common';
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FullCalendarModule } from '@fullcalendar/angular';
import dayGridPlugin from '@fullcalendar/daygrid';
import { CalendarOptions, DatesSetArg, DayCellContentArg, EventInput } from '@fullcalendar/core';

import { ReservaService } from '../../services/reserva.service';
import { ReservaCalendarioDTO } from '../../models/reserva-calendario.model';
import { EstadoReserva, TipoUnidad } from '../../models/enums';

@Component({
  selector: 'app-reserva',
  standalone: true,
  imports: [CommonModule, FormsModule, FullCalendarModule],
  templateUrl: './reserva.component.html',
  styleUrl: './reserva.component.css',
  encapsulation: ViewEncapsulation.None,
})
export class ReservaComponent implements OnInit {
  reservas: ReservaCalendarioDTO[] = [];
  cargando = false;
  error = '';
  private readonly maxUnidadesPorDia = 13;
  private reservasPorDia = new Map<string, Map<string, EstadoReserva>>();

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
    datesSet: (arg: DatesSetArg) => this.onDatesSet(arg),
  };

  constructor(private readonly reservaService: ReservaService) {}

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
  ): Map<string, Map<string, EstadoReserva>> {
    const mapa = new Map<string, Map<string, EstadoReserva>>();

    reservas.forEach((reserva) => {
      const inicio = this.parsearFechaLocal(reserva.inicio);
      const fin = this.parsearFechaLocal(reserva.fin);
      if (!inicio || !fin || !reserva.codigoUnidad) {
        return;
      }

      const fechaActual = new Date(inicio.getFullYear(), inicio.getMonth(), inicio.getDate());
      const fechaFinal = new Date(fin.getFullYear(), fin.getMonth(), fin.getDate());

      while (fechaActual <= fechaFinal) {
        const clave = this.formatearDia(fechaActual);
        let mapaDia = mapa.get(clave);
        if (!mapaDia) {
          mapaDia = new Map<string, EstadoReserva>();
          mapa.set(clave, mapaDia);
        }

        if (!mapaDia.has(reserva.codigoUnidad)) {
          mapaDia.set(reserva.codigoUnidad, reserva.estado);
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
      .map(([codigo, estado]) => {
        const slot = this.obtenerSlotCodigo(codigo);
        const estadoClase = estado.toLowerCase();
        return `<span class="reserva-circulo reserva-circulo--slot-${slot} reserva-circulo--${estadoClase}">${this.escaparHtml(
          codigo,
        )}</span>`;
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
}
