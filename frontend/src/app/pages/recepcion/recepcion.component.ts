import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { map } from 'rxjs';

import { HabitacionDTO } from '../../models/habitacion.model';
import { EstanciaDTO } from '../../models/estancia-detalle.model';
import { OcupanteDTO } from '../../models/ocupante.model';
import { UnidadDTO } from '../../models/unidad.model';
import { EstanciaService } from '../../services/estancia.service';
import { HabitacionService } from '../../services/habitacion.service';
import { ReservaService } from '../../services/reserva.service';
import { UnidadService } from '../../services/unidad.service';
import { EstadoOperativo, Piso, TipoUnidad } from '../../models/enums';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import { getCurrentDateInput } from '../../core/utils/date-time.util';
import { parseJsonSafe } from '../../core/utils/json.util';
import { ReservaTablaFiltros, ReservaTablaItem } from '../../models/reserva-tabla.model';

interface InformacionEstanciaUnidad {
  idReserva?: number;
  idEstancia?: number;
  idCliente?: number;
  codigo?: string;
  nombreCliente?: string;
  fechaEntrada?: string;
  fechaSalida?: string;
  modoOcupacion?: string;
  numeroPersonas?: number;
  estado?: string;
  totalPagadoAnticipo?: number;
}

interface InformacionAdicionalUnidad {
  ESTANCIA?: InformacionEstanciaUnidad;
  RESERVA?: InformacionEstanciaUnidad;
}

interface MetaInfoVisual {
  tipo: 'RANGO' | 'PROXIMA';
  fechaInicio: string;
  fechaFin?: string;
  numeroPersonas?: number;
  fuente?: 'ESTANCIA' | 'RESERVA';
}

interface ModalDetalleItem {
  etiqueta: string;
  valor: string;
}

@Component({
  selector: 'app-recepcion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './recepcion.component.html',
  styleUrl: './recepcion.component.css',
})
export class RecepcionComponent implements OnInit {
  unidades: UnidadDTO[] = [];
  cargando = true;
  error = '';
  mostrarModalAlerta = false;
  modalAlertaTitulo = '';
  modalAlertaContexto = '';
  modalAlertaMensaje = '';
  modalAlertaDetalle: ModalDetalleItem[] = [];
  modalAlertaVariante: 'alerta' | 'reserva' = 'alerta';
  private codigoTimer?: ReturnType<typeof setTimeout>;
  private requestCounter = 0;

  filtroTipo: TipoUnidad | '' = '';
  filtroEstados: EstadoOperativo[] = [];
  filtroPisos: Piso[] = [];
  filtroCodigo = '';

  tiposUnidad = [
    { label: 'APARTAMENTO', value: 'APARTAMENTO' as TipoUnidad },
    { label: 'HABITACION', value: 'HABITACION' as TipoUnidad },
    { label: 'APARTAESTUDIO', value: 'APARTAESTUDIO' as TipoUnidad },
  ];
  estadosOperativos: EstadoOperativo[] = ['DISPONIBLE', 'PARCIALMENTE', 'OCUPADO', 'RESERVADO'];
  pisos: Piso[] = ['PISO_1', 'PISO_2', 'PISO_3'];

  constructor(
    private readonly unidadService: UnidadService,
    private readonly habitacionService: HabitacionService,
    private readonly estanciaService: EstanciaService,
    private readonly reservaService: ReservaService,
    private readonly router: Router
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
    this.filtroTipo = this.filtroTipo === tipo ? '' : tipo;
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

  get totalFiltrosActivos(): number {
    let total = 0;
    if (this.filtroTipo) {
      total += 1;
    }
    if (this.filtroEstados.length) {
      total += 1;
    }
    if (this.filtroPisos.length) {
      total += 1;
    }
    if (this.filtroCodigo.trim()) {
      total += 1;
    }
    return total;
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

  claseChipEstado(estado: EstadoOperativo): string {
    switch (estado) {
      case 'DISPONIBLE':
        return 'segmento-estado--disponible';
      case 'PARCIALMENTE':
        return 'segmento-estado--parcial';
      case 'OCUPADO':
        return 'segmento-estado--ocupado';
      case 'RESERVADO':
        return 'segmento-estado--reservado';
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

  unidadDesdeHabitacion(habitacion: HabitacionDTO): UnidadDTO {
    return {
      id: habitacion.id,
      codigo: habitacion.codigo,
      tipo: 'HABITACION',
      piso: habitacion.piso,
      estado: habitacion.estado,
      habitaciones: [habitacion],
      informacionAdicional: habitacion.informacionAdicional,
    };
  }

  obtenerMetaUnidad(unidad: UnidadDTO): MetaInfoVisual[] {
    return this.construirMetaVisualPorEstado(unidad.estado, unidad.informacionAdicional);
  }

  obtenerMetaHabitacion(habitacion: HabitacionDTO): MetaInfoVisual[] {
    return this.construirMetaVisualPorEstado(habitacion.estado, habitacion.informacionAdicional);
  }

  obtenerMetaRangoHabitacion(habitacion: HabitacionDTO): MetaInfoVisual | null {
    return this.obtenerMetaHabitacion(habitacion).find((meta) => meta.tipo === 'RANGO') ?? null;
  }

  tieneMetaHabitacion(habitacion: HabitacionDTO): boolean {
    return this.obtenerMetaRangoHabitacion(habitacion) !== null;
  }

  obtenerFechaProximaReservaUnidad(unidad: UnidadDTO): string | null {
    return this.obtenerMetaUnidad(unidad).find((meta) => meta.tipo === 'PROXIMA')?.fechaInicio ?? null;
  }

  obtenerFechaProximaReservaHabitacion(habitacion: HabitacionDTO): string | null {
    return this.obtenerMetaHabitacion(habitacion).find((meta) => meta.tipo === 'PROXIMA')?.fechaInicio ?? null;
  }

  puedeDarSalida(unidad: UnidadDTO): boolean {
    if (unidad.estado !== 'OCUPADO') {
      return false;
    }

    const info = this.parsearInformacionAdicional(unidad.informacionAdicional);
    return this.extraerInfoValida(info?.ESTANCIA) !== null;
  }

  puedeDarEntrada(unidad: UnidadDTO): boolean {
    if (unidad.estado !== 'RESERVADO') {
      return false;
    }

    const info = this.parsearInformacionAdicional(unidad.informacionAdicional);
    return this.extraerInfoValida(info?.RESERVA) !== null;
  }

  obtenerAlertaUnidad(unidad: UnidadDTO): string | null {
    return this.obtenerMensajeAlerta(unidad.informacionAdicional);
  }

  obtenerAlertaHabitacion(habitacion: HabitacionDTO): string | null {
    return this.obtenerMensajeAlerta(habitacion.informacionAdicional);
  }

  abrirModalAlerta(
    mensaje: string,
    codigo: string,
    event: Event,
    esHabitacion = false
  ): void {
    event.stopPropagation();
    event.preventDefault();
    this.modalAlertaVariante = 'alerta';
    this.modalAlertaTitulo = esHabitacion ? 'Alerta en habitacion' : 'Alerta en unidad';
    this.modalAlertaContexto = codigo;
    this.modalAlertaMensaje = mensaje || 'Sin alertas registradas.';
    this.modalAlertaDetalle = [];
    this.mostrarModalAlerta = true;
  }

  cerrarModalAlerta(): void {
    this.mostrarModalAlerta = false;
    this.modalAlertaTitulo = '';
    this.modalAlertaContexto = '';
    this.modalAlertaMensaje = '';
    this.modalAlertaDetalle = [];
    this.modalAlertaVariante = 'alerta';
  }

  abrirModalProximaReserva(
    informacionAdicional: string | null | undefined,
    codigo: string,
    event: Event,
    esHabitacion = false
  ): void {
    event.stopPropagation();
    event.preventDefault();
    this.modalAlertaVariante = 'reserva';
    const info = this.parsearInformacionAdicional(informacionAdicional);
    const reserva = this.extraerInfoValida(info?.RESERVA);
    this.modalAlertaTitulo = esHabitacion ? 'Proxima reserva de habitacion' : 'Proxima reserva';
    this.modalAlertaContexto = codigo;
    this.modalAlertaMensaje = reserva
      ? 'Detalle completo de la proxima reserva registrada.'
      : 'No hay informacion de reserva disponible.';
    this.modalAlertaDetalle = reserva
      ? [
          { etiqueta: 'Entrada', valor: this.formatearFechaInfo(reserva.fechaEntrada) },
          { etiqueta: 'Salida', valor: this.formatearFechaInfo(reserva.fechaSalida) },
          { etiqueta: 'Personas', valor: `${reserva.numeroPersonas ?? '-'}` },
        ]
      : [];
    this.mostrarModalAlerta = true;
  }

  formatearFechaInfo(valor: string | undefined): string {
    if (!valor) {
      return '-';
    }

    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) {
      return valor;
    }

    return new Intl.DateTimeFormat('es-CO', {
      dateStyle: 'medium',
    }).format(fecha);
  }

  formatearFechaCortaInfo(valor: string | undefined): string {
    if (!valor) {
      return '-';
    }

    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) {
      return valor;
    }

    const dia = `${fecha.getDate()}`.padStart(2, '0');
    const mes = `${fecha.getMonth() + 1}`.padStart(2, '0');
    return `${dia}/${mes}`;
  }

  tooltipFecha(meta: MetaInfoVisual): string {
    if (meta.fuente === 'RESERVA') {
      return 'Fechas de reserva';
    }
    return 'Fechas de estancia';
  }

  tieneHabitacionesReservadas(unidad: UnidadDTO): boolean {
    return this.esApartamento(unidad)
      && unidad.habitaciones.some((habitacion) => habitacion.estado === 'RESERVADO');
  }

  buscarUnidades(): void {
    const requestId = ++this.requestCounter;
    this.cargando = true;
    this.error = '';

    const estados = this.filtroEstados.length ? this.filtroEstados : undefined;
    const pisos = this.filtroPisos.length ? this.filtroPisos : undefined;
    const codigo = this.filtroCodigo.trim() || undefined;

    if (this.filtroTipo === 'HABITACION') {
      this.habitacionService.buscarHabitaciones({ estados, pisos, codigo }).subscribe({
        next: (habitaciones) => {
          if (requestId !== this.requestCounter) {
            return;
          }
          this.unidades = this.mapearHabitaciones(habitaciones);
          this.cargando = false;
        },
        error: (errorResponse: unknown) => {
          if (requestId !== this.requestCounter) {
            return;
          }
          this.error = extractBackendErrorMessage(
            errorResponse,
            'No fue posible cargar las habitaciones.'
          );
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
          if (requestId !== this.requestCounter) {
            return;
          }
          this.unidades = unidades;
          this.cargando = false;
        },
        error: (errorResponse: unknown) => {
          if (requestId !== this.requestCounter) {
            return;
          }
          this.error = extractBackendErrorMessage(
            errorResponse,
            'No fue posible cargar las unidades.'
          );
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

  abrirDetalle(unidad: UnidadDTO): void {
    this.router.navigate(['/recepcion/panel'], {
      queryParams: { codigo: unidad.codigo, tipo: unidad.tipo },
    });
  }

  abrirDetalleHabitacion(habitacion: HabitacionDTO, event: Event): void {
    event.stopPropagation();
    const unidadHabitacion = this.unidadDesdeHabitacion(habitacion);
    this.abrirDetalle(unidadHabitacion);
  }

  crearEstancia(unidad: UnidadDTO, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    this.router.navigate(['/estancias/nueva'], {
      queryParams: { codigo: unidad.codigo, tipo: unidad.tipo },
      state: { returnTo: this.router.url },
    });
  }

  crearEstanciaOSalida(unidad: UnidadDTO, event: Event): void {
    if (unidad.estado === 'OCUPADO') {
      this.darSalida(unidad, event);
      return;
    }

    this.crearEstancia(unidad, event);
  }

  crearReserva(unidad: UnidadDTO, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    this.router.navigate(['/reservas/nueva'], {
      queryParams: { codigo: unidad.codigo, tipo: unidad.tipo },
      state: { returnTo: this.router.url },
    });
  }

  crearReservaOIngreso(unidad: UnidadDTO, event: Event): void {
    this.crearReserva(unidad, event);
  }

  darIngreso(unidad: UnidadDTO, event: Event): void {
    this.darIngresoReserva(unidad, event);
  }

  finalizarEstancia(unidad: UnidadDTO, event: Event): void {
    this.darSalida(unidad, event);
  }

  private darIngresoReserva(unidad: UnidadDTO, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    const reservaInfo = this.parsearInformacionAdicional(unidad.informacionAdicional)?.RESERVA ?? null;

    this.buscarReservaConfirmada(unidad.codigo, 1).subscribe({
      next: (reservas) => {
        const reserva = reservas[0];
        if (!reserva) {
          this.navegarADarIngresoSinReserva(unidad, reservaInfo);
          return;
        }

        this.navegarADarIngreso(reserva, unidad, reservaInfo);
      },
      error: () => {
        this.navegarADarIngresoSinReserva(unidad, reservaInfo);
      },
    });
  }

  private buscarReservaConfirmada(codigoUnidad: string, size: number) {
    const filtros: ReservaTablaFiltros = {
      estados: ['CONFIRMADA'],
      canales: [],
      modoOcupacion: '',
      tipoUnidad: '',
      codigoReserva: '',
      codigoUnidad,
      nombreCliente: '',
      numeroDocumentoCliente: '',
      rangoGeneralDesde: '',
      rangoGeneralHasta: '',
      fechaCreacionDesde: '',
      fechaCreacionHasta: '',
      entradaDesde: this.obtenerFechaActual(),
      entradaHasta: '',
      salidaDesde: '',
      salidaHasta: '',
    };

    return this.reservaService
      .obtenerTabla(filtros, 0, size, ['entradaEstimada,asc'])
      .pipe(map((respuesta) => respuesta.content));
  }

  private navegarADarIngreso(
    reserva: ReservaTablaItem,
    unidad: UnidadDTO,
    reservaInfo: InformacionEstanciaUnidad | null
  ): void {
    this.router.navigate(['/estancias/nueva'], {
      state: {
        returnTo: this.router.url,
        flujo: 'INGRESO',
        idReserva: reserva.id || reservaInfo?.idReserva || undefined,
        idEstancia: reservaInfo?.idEstancia ?? undefined,
        codigoReserva: reserva.codigoReserva || reservaInfo?.codigo || '',
        idCliente:
          (reserva.idCliente && reserva.idCliente > 0 ? reserva.idCliente : undefined) ??
          (reservaInfo?.idCliente && reservaInfo.idCliente > 0 ? reservaInfo.idCliente : undefined),
        nombreCliente: reserva.nombreCliente || reservaInfo?.nombreCliente || '',
        codigo: reserva.codigoUnidad || unidad.codigo,
        tipo: reserva.tipoUnidad || unidad.tipo,
        entrada: reserva.entradaEstimada || reservaInfo?.fechaEntrada || '',
        salida: reserva.salidaEstimada || reservaInfo?.fechaSalida || '',
        numeroPersonasReserva: reserva.numeroPersonas || reservaInfo?.numeroPersonas || undefined,
        totalAnticipo: reserva.totalPagoReserva ?? reservaInfo?.totalPagadoAnticipo ?? undefined,
      },
    });
  }

  private navegarADarIngresoSinReserva(
    unidad: UnidadDTO,
    reservaInfo: InformacionEstanciaUnidad | null
  ): void {
    this.router.navigate(['/estancias/nueva'], {
      queryParams: { codigo: unidad.codigo, tipo: unidad.tipo },
      state: {
        returnTo: this.router.url,
        flujo: 'INGRESO',
        idReserva: reservaInfo?.idReserva ?? undefined,
        idEstancia: reservaInfo?.idEstancia ?? undefined,
        idCliente:
          reservaInfo?.idCliente && reservaInfo.idCliente > 0 ? reservaInfo.idCliente : undefined,
        codigoReserva: reservaInfo?.codigo || '',
        nombreCliente: reservaInfo?.nombreCliente || '',
        entrada: reservaInfo?.fechaEntrada || '',
        salida: reservaInfo?.fechaSalida || '',
        numeroPersonasReserva: reservaInfo?.numeroPersonas || undefined,
        totalAnticipo: reservaInfo?.totalPagadoAnticipo ?? undefined,
      },
    });
  }

  private darSalida(unidad: UnidadDTO, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    this.error = '';
    const idEstancia = this.obtenerIdEstanciaDesdeInformacionAdicional(unidad.informacionAdicional);

    if (!idEstancia) {
      this.error = 'No se encontro idEstancia en la informacion adicional para registrar salida.';
      return;
    }

    this.estanciaService.obtenerEstanciaPorId(idEstancia).subscribe({
      next: (estancia) => {
        const ocupantes = this.obtenerOcupantesEstancia(estancia);
        const cliente = ocupantes.find((ocupante) => ocupante.tipoOcupante === 'CLIENTE');
        const nombreCliente = [cliente?.nombres, cliente?.apellidos]
          .filter((valor) => Boolean(valor?.trim()))
          .join(' ');

        this.router.navigate(['/estancias/salida'], {
          state: {
            returnTo: this.router.url,
            estanciaId: estancia.id,
            codigo: unidad.codigo,
            tipo: unidad.tipo,
            entrada: estancia.entradaReal,
            salida: estancia.salidaEstimada,
            numeroPersonas: ocupantes.length,
            nombreCliente,
          },
        });
      },
      error: (errorResponse: unknown) => {
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible preparar la finalizacion de la estancia.'
        );
      },
    });
  }

  private obtenerIdEstanciaDesdeInformacionAdicional(
    informacionAdicional: string | null | undefined
  ): number | null {
    const info = this.parsearInformacionAdicional(informacionAdicional);
    const idEstancia = info?.ESTANCIA?.idEstancia;

    if (typeof idEstancia !== 'number' || Number.isNaN(idEstancia) || idEstancia <= 0) {
      return null;
    }

    return idEstancia;
  }

  private obtenerFechaActual(): string {
    return getCurrentDateInput();
  }

  private parsearInformacionAdicional(
    informacionAdicional: string | null | undefined
  ): InformacionAdicionalUnidad | null {
    return parseJsonSafe<InformacionAdicionalUnidad>(informacionAdicional);
  }

  private extraerInfoValida(
    info: InformacionEstanciaUnidad | undefined
  ): InformacionEstanciaUnidad | null {
    if (!info?.fechaEntrada) {
      return null;
    }

    return info;
  }

  private obtenerOcupantesEstancia(estancia: EstanciaDTO): OcupanteDTO[] {
    const cliente = estancia.cliente ? [estancia.cliente] : [];
    return [...cliente, ...(estancia.acompanantes ?? [])];
  }

  private obtenerMensajeAlerta(informacionAdicional: string | null | undefined): string | null {
    const info = this.parsearInformacionAdicional(informacionAdicional);
    const alertas: string[] = [];

    if (info?.ESTANCIA?.estado === 'EXCEDIDA') {
      alertas.push('La estancia se encuentra excedida y requiere finalizacion.');
    }

    if (info?.RESERVA?.estado === 'EXPIRADA') {
      alertas.push('La reserva se encuentra expirada y requiere revision.');
    }

    return alertas.length ? alertas.join(' ') : null;
  }

  private construirMetaVisualPorEstado(
    estado: EstadoOperativo,
    informacionAdicional: string | null | undefined
  ): MetaInfoVisual[] {
    const info = this.parsearInformacionAdicional(informacionAdicional);
    const estancia = this.extraerInfoValida(info?.ESTANCIA);
    const reserva = this.extraerInfoValida(info?.RESERVA);
    const metas: MetaInfoVisual[] = [];

    if (estado === 'OCUPADO') {
      if (estancia?.fechaEntrada && estancia.fechaSalida) {
        metas.push({
          tipo: 'RANGO',
          fechaInicio: estancia.fechaEntrada,
          fechaFin: estancia.fechaSalida,
          numeroPersonas: estancia.numeroPersonas,
          fuente: 'ESTANCIA',
        });
      }

      if (reserva?.fechaEntrada) {
        metas.push({
          tipo: 'PROXIMA',
          fechaInicio: reserva.fechaEntrada,
          fuente: 'RESERVA',
        });
      }

      return metas;
    }

    if (estado === 'PARCIALMENTE' || estado === 'DISPONIBLE') {
      if (reserva?.fechaEntrada) {
        metas.push({
          tipo: 'PROXIMA',
          fechaInicio: reserva.fechaEntrada,
          fuente: 'RESERVA',
        });
      }

      return metas;
    }

    if (estado === 'RESERVADO') {
      if (reserva?.fechaEntrada && reserva.fechaSalida) {
        metas.push({
          tipo: 'RANGO',
          fechaInicio: reserva.fechaEntrada,
          fechaFin: reserva.fechaSalida,
          numeroPersonas: reserva.numeroPersonas,
          fuente: 'RESERVA',
        });
      }
    }

    return metas;
  }

  private mapearHabitaciones(habitaciones: HabitacionDTO[]): UnidadDTO[] {
    return habitaciones.map((habitacion) => ({
      id: habitacion.id,
      codigo: habitacion.codigo,
      tipo: 'HABITACION',
      piso: habitacion.piso,
      estado: habitacion.estado,
      habitaciones: [habitacion],
      informacionAdicional: habitacion.informacionAdicional,
    }));
  }

}
