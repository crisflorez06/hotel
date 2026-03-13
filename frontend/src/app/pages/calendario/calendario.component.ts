import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Observable, of, Subject, catchError, finalize, switchMap, takeUntil, tap } from 'rxjs';

import { DetalleCalendarioUnidadDTO } from '../../models/detalle-calendario.model';
import { EstanciaDTO } from '../../models/estancia-detalle.model';
import { EstadoEstancia, EstadoReserva, TipoUnidad } from '../../models/enums';
import { OcupanteDTO } from '../../models/ocupante.model';
import { ReservaDTO } from '../../models/reserva.model';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import { EstanciaService } from '../../services/estancia.service';
import { ReservaService } from '../../services/reserva.service';
import { CalendarioInfoCardComponent } from './calendario-info-card/calendario-info-card.component';
import {
  construirQueryClientesPorEstancia,
  construirQueryClientesPorOcupante,
  construirQueryEstanciasPorCodigo,
  construirQueryReservasPorCodigo,
} from './calendario-navigation.util';

type TipoRegistro = 'RESERVA' | 'ESTANCIA';

interface CalendarioRegistroView {
  id: number;
  tipo: TipoRegistro;
  etiqueta: string;
  estado: string;
  modoOcupacion?: string | null;
  inicio: string;
  fin: string;
  codigoUnidad: string;
  codigoHabitacion?: string | null;
  tipoUnidad: TipoUnidad | null;
  numeroPersonas: number | null;
  nombreCliente: string | null;
  idCliente: number | null;
  mostrarResumenIndividualUnidad?: boolean;
  habitacionesAfectadas?: string[];
  reserva?: ReservaDTO;
  estancia?: EstanciaDTO;
}

interface CalendarioFilaView {
  id: string;
  nombre: string;
  subtitulo: string;
  tipoUnidad: TipoUnidad | null;
  nivel: 'unidad' | 'habitacion';
  esHabitacionDeApartamento: boolean;
  registros: CalendarioRegistroView[];
  informacionAdicional: string | null;
}

interface CalendarioSegmentoView {
  registro: CalendarioRegistroView;
  inicioIndice: number;
  finIndice: number;
  nivel: number;
}

interface CalendarioSeleccion {
  filaId: string;
  dia: string;
}

@Component({
  selector: 'app-calendario',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, CalendarioInfoCardComponent],
  templateUrl: './calendario.component.html',
  styleUrl: './calendario.component.css',
})
export class CalendarioComponent implements OnInit, OnDestroy {
  private readonly diasVisibles = 15;
  private readonly margenPrefetchDias = 21;
  private readonly estadosReservaPorDefecto: EstadoReserva[] = ['CONFIRMADA', 'EXPIRADA'];
  private readonly estadosEstanciaPorDefecto: EstadoEstancia[] = ['ACTIVA', 'EXCEDIDA'];

  cargando = false;
  error = '';

  filas: CalendarioFilaView[] = [];
  private registrosPorFilaDia = new Map<string, Map<string, CalendarioRegistroView[]>>();
  private segmentosPorFila = new Map<string, CalendarioSegmentoView[]>();
  private detalleCache: DetalleCalendarioUnidadDTO[] = [];
  private cacheDesde: Date | null = null;
  private cacheHasta: Date | null = null;
  private cacheClaveFiltros = '';
  seleccion: CalendarioSeleccion | null = null;
  tooltipSeleccionVisible: CalendarioSeleccion | null = null;
  private tooltipSeleccionTimeout: ReturnType<typeof setTimeout> | null = null;

  semanaInicio = this.inicioDelDia(new Date());

  filtroTipoUnidad: TipoUnidad | '' = '';
  filtroCodigo = '';
  filtroEstadosReserva: EstadoReserva[] = [...this.estadosReservaPorDefecto];
  filtroEstadosEstancia: EstadoEstancia[] = [...this.estadosEstanciaPorDefecto];

  reservaSeleccionada: CalendarioRegistroView | null = null;
  estanciaSeleccionada: CalendarioRegistroView | null = null;
  private readonly recargaCalendario$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  private filtroCodigoTimeout: ReturnType<typeof setTimeout> | null = null;
  eliminandoEstancia = false;
  mostrarModalEliminarEstancia = false;
  estanciaPendienteEliminar: CalendarioRegistroView | null = null;
  eliminandoReserva = false;
  mostrarModalEliminarReserva = false;
  reservaPendienteEliminar: CalendarioRegistroView | null = null;

  tiposUnidad = [
    { label: 'Todas', value: '' as const },
    { label: 'Apartamento', value: 'APARTAMENTO' as TipoUnidad },
    { label: 'Apartaestudio', value: 'APARTAESTUDIO' as TipoUnidad },
    { label: 'Habitacion', value: 'HABITACION' as TipoUnidad },
  ];

  readonly estadosReserva: EstadoReserva[] = ['CONFIRMADA', 'CANCELADA', 'COMPLETADA', 'EXPIRADA'];
  readonly estadosEstancia: EstadoEstancia[] = [
    'ACTIVA',
    'FINALIZADA',
    'EXCEDIDA',
    'CANCELADA',
  ];

  constructor(
    private readonly reservaService: ReservaService,
    private readonly estanciaService: EstanciaService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.recargaCalendario$
      .pipe(
        switchMap(() => this.ejecutarCargaCalendarioSemana()),
        takeUntil(this.destroy$),
      )
      .subscribe();

    this.cargarCalendarioSemana();
  }

  ngOnDestroy(): void {
    this.ocultarTooltipSeleccion();
    if (this.filtroCodigoTimeout) {
      clearTimeout(this.filtroCodigoTimeout);
      this.filtroCodigoTimeout = null;
    }

    this.destroy$.next();
    this.destroy$.complete();
    this.recargaCalendario$.complete();
  }

  get diasSemana(): Date[] {
    return Array.from({ length: this.diasVisibles }, (_, index) => {
      const dia = new Date(this.semanaInicio);
      dia.setDate(dia.getDate() + index);
      return dia;
    });
  }

  get rangoSemanaTexto(): string {
    const inicio = this.formatearSoloFecha(this.semanaInicio);
    const fin = this.formatearSoloFecha(this.diasSemana[this.diasSemana.length - 1]);
    return `${inicio} - ${fin}`;
  }

  irANuevaReserva(): void {
    const prefill = this.obtenerPrefillSeleccion();
    this.router.navigate(['/reservas/nueva'], {
      state: {
        returnTo: this.router.url,
        codigo: prefill?.codigo,
        tipo: prefill?.tipo,
        entrada: prefill?.entrada,
      },
    });
  }

  irANuevaEstancia(): void {
    const prefill = this.obtenerPrefillSeleccion();
    this.router.navigate(['/estancias/nueva'], {
      state: {
        returnTo: this.router.url,
        codigo: prefill?.codigo,
        tipo: prefill?.tipo,
        entrada: prefill?.entrada,
      },
    });
  }

  seleccionarCuadro(fila: CalendarioFilaView, dia: Date): void {
    if (this.esCuadroBloqueadoParaSeleccion(fila, dia)) {
      return;
    }

    const diaFormateado = this.formatearDia(dia);
    if (this.seleccion?.filaId === fila.id && this.seleccion?.dia === diaFormateado) {
      this.seleccion = null;
      this.ocultarTooltipSeleccion();
      return;
    }

    this.seleccion = {
      filaId: fila.id,
      dia: diaFormateado,
    };
    this.mostrarTooltipSeleccionTemporal(fila.id, diaFormateado);
  }

  estaCuadroSeleccionado(fila: CalendarioFilaView, dia: Date): boolean {
    if (!this.seleccion) {
      return false;
    }

    return this.seleccion.filaId === fila.id && this.seleccion.dia === this.formatearDia(dia);
  }

  mostrarTooltipSeleccion(fila: CalendarioFilaView, dia: Date): boolean {
    if (!this.tooltipSeleccionVisible) {
      return false;
    }

    return (
      this.tooltipSeleccionVisible.filaId === fila.id &&
      this.tooltipSeleccionVisible.dia === this.formatearDia(dia)
    );
  }

  esCuadroBloqueadoParaSeleccion(fila: CalendarioFilaView, dia: Date): boolean {
    const diaFormateado = this.formatearDia(dia);
    const registrosDia = this.registrosPorFilaDia.get(fila.id)?.get(diaFormateado) ?? [];

    return registrosDia.some((registro) => {
      const estado = registro.estado?.toUpperCase?.() ?? '';
      if (registro.tipo === 'ESTANCIA') {
        return estado === 'ACTIVA' || estado === 'EXCEDIDA';
      }
      if (registro.tipo === 'RESERVA') {
        return estado === 'CONFIRMADA' || estado === 'EXPIRADA';
      }
      return false;
    });
  }

  irSemanaAnterior(): void {
    const nuevaFecha = new Date(this.semanaInicio);
    nuevaFecha.setDate(nuevaFecha.getDate() - 1);
    this.semanaInicio = nuevaFecha;
    this.cargarCalendarioSemana();
  }

  irSemanaSiguiente(): void {
    const nuevaFecha = new Date(this.semanaInicio);
    nuevaFecha.setDate(nuevaFecha.getDate() + 1);
    this.semanaInicio = nuevaFecha;
    this.cargarCalendarioSemana();
  }

  irSemanaActual(): void {
    this.semanaInicio = this.inicioDelDia(new Date());
    this.cargarCalendarioSemana();
  }

  aplicarFiltros(): void {
    this.cargarCalendarioSemana();
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
    this.filtroEstadosReserva = [...this.estadosReservaPorDefecto];
    this.filtroEstadosEstancia = [...this.estadosEstanciaPorDefecto];
    this.cargarCalendarioSemana();
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

  get totalFiltrosActivos(): number {
    let total = 0;
    if (this.filtroTipoUnidad) {
      total += 1;
    }
    if (this.filtroCodigo.trim()) {
      total += 1;
    }
    if (this.filtroEstadosReserva.length) {
      total += 1;
    }
    if (this.filtroEstadosEstancia.length) {
      total += 1;
    }
    return total;
  }

  get usaEstadosPorDefecto(): boolean {
    const reservaActual = [...this.filtroEstadosReserva].sort().join(',');
    const reservaDefault = [...this.estadosReservaPorDefecto].sort().join(',');
    const estanciaActual = [...this.filtroEstadosEstancia].sort().join(',');
    const estanciaDefault = [...this.estadosEstanciaPorDefecto].sort().join(',');
    return reservaActual === reservaDefault && estanciaActual === estanciaDefault;
  }

  get mostrarMensajeSinDatos(): boolean {
    return !this.cargando;
  }

  get puedeAgregarEstanciaDesdeSeleccion(): boolean {
    if (!this.seleccion) {
      return false;
    }

    const fechaSeleccion = this.parsearFechaLocal(this.seleccion.dia);
    if (!fechaSeleccion) {
      return false;
    }

    return fechaSeleccion.getTime() <= this.inicioDelDia(new Date()).getTime();
  }

  get puedeCrearReservaDesdeSeleccion(): boolean {
    if (!this.seleccion) {
      return false;
    }

    const fechaSeleccion = this.parsearFechaLocal(this.seleccion.dia);
    if (!fechaSeleccion) {
      return false;
    }

    return fechaSeleccion.getTime() >= this.inicioDelDia(new Date()).getTime();
  }

  obtenerSegmentosFila(filaId: string): CalendarioSegmentoView[] {
    return this.segmentosPorFila.get(filaId) ?? [];
  }

  alturaFilaPx(filaId: string): number {
    const segmentos = this.obtenerSegmentosFila(filaId);
    if (!segmentos.length) {
      return 56;
    }

    const maxNivel = Math.max(...segmentos.map((segmento) => segmento.nivel));
    return Math.max(56, (maxNivel + 1) * 24 + 10);
  }

  abrirDetalleRegistro(registro: CalendarioRegistroView): void {
    if (registro.tipo === 'RESERVA') {
      this.reservaSeleccionada = registro;
      this.estanciaSeleccionada = null;
      return;
    }

    this.estanciaSeleccionada = registro;
    this.reservaSeleccionada = null;
  }

  cerrarDetalle(): void {
    this.reservaSeleccionada = null;
    this.estanciaSeleccionada = null;
  }

  mantenerModal(event: MouseEvent): void {
    event.stopPropagation();
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

  formatearSoloFecha(valor: Date): string {
    return valor.toLocaleDateString('es-CO', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  }

  formatearDiaCabecera(valor: Date): string {
    return valor.toLocaleDateString('es-CO', {
      weekday: 'short',
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

  formatearCanalReserva(valor: string | null | undefined): string {
    if (!valor) {
      return '-';
    }

    const canalSinPrefijo = valor.startsWith('PLATAFORMA_')
      ? valor.replace('PLATAFORMA_', '')
      : valor;

    return this.formatearEtiqueta(canalSinPrefijo);
  }

  obtenerTotalPagadoEstancia(item: CalendarioRegistroView): number {
    return (item.estancia?.pagos ?? [])
      .filter((pago) => pago.estado === 'COMPLETADO')
      .reduce((total, pago) => total + Number(pago.monto ?? 0), 0);
  }

  irATablaReservasPorCodigo(codigoReserva: string | null | undefined): void {
    const queryParams = construirQueryReservasPorCodigo(codigoReserva);
    if (!queryParams) {
      return;
    }

    this.cerrarDetalle();
    this.router.navigate(['/reservas'], {
      queryParams,
    });
  }

  irATablaEstanciasPorCodigo(codigoEstancia: string | null | undefined): void {
    const queryParams = construirQueryEstanciasPorCodigo(codigoEstancia);
    if (!queryParams) {
      return;
    }

    this.cerrarDetalle();
    this.router.navigate(['/estancias'], {
      queryParams,
    });
  }

  irATablaClientes(cliente: OcupanteDTO | null | undefined): void {
    const queryParams = construirQueryClientesPorOcupante(cliente);
    if (!queryParams) {
      return;
    }

    this.cerrarDetalle();
    this.router.navigate(['/ocupantes/tabla-clientes'], { queryParams });
  }

  esClienteNavegable(cliente: OcupanteDTO | null | undefined): boolean {
    return !!construirQueryClientesPorOcupante(cliente);
  }

  puedeIrAClientesPorEstancia(estancia: EstanciaDTO | null | undefined): boolean {
    return !!construirQueryClientesPorEstancia(estancia);
  }

  irAClientesPorPersonasEstancia(estancia: EstanciaDTO | null | undefined): void {
    const queryParams = construirQueryClientesPorEstancia(estancia);
    if (!queryParams) {
      return;
    }

    this.cerrarDetalle();
    this.router.navigate(['/ocupantes/tabla-clientes'], {
      queryParams,
    });
  }

  mostrarCodigoEstanciaReserva(item: CalendarioRegistroView): boolean {
    const codigoEstancia = item.reserva?.codigoEstancia?.trim();
    return item.estado !== 'CONFIRMADA' && Boolean(codigoEstancia);
  }

  private cargarCalendarioSemana(): void {
    this.recargaCalendario$.next();
  }

  private ejecutarCargaCalendarioSemana(): Observable<DetalleCalendarioUnidadDTO[] | null> {
    this.error = '';

    const visibleDesde = this.inicioDelDia(this.semanaInicio);
    const visibleHasta = this.inicioDelDia(this.ultimoDiaVisible());

    const tipoUnidad = this.filtroTipoUnidad || undefined;
    const codigoUnidad = this.filtroCodigo.trim() || undefined;
    const estadosReserva = this.filtroEstadosReserva;
    const estadosEstancia = this.filtroEstadosEstancia;

    const claveFiltros = this.construirClaveFiltros(
      tipoUnidad,
      codigoUnidad,
      estadosReserva,
      estadosEstancia,
    );

    if (this.cubreRangoEnCache(visibleDesde, visibleHasta) && this.cacheClaveFiltros === claveFiltros) {
      this.actualizarVistaDesdeCache();
      return of(null);
    }

    const desdeConsulta = new Date(visibleDesde);
    desdeConsulta.setDate(desdeConsulta.getDate() - this.margenPrefetchDias);
    const hastaConsulta = new Date(visibleHasta);
    hastaConsulta.setDate(hastaConsulta.getDate() + this.margenPrefetchDias);

    const desde = this.formatearFechaIsoLocal(desdeConsulta, false);
    const hasta = this.formatearFechaIsoLocal(hastaConsulta, true);

    const mostrarCargaCompleta = this.detalleCache.length === 0;
    if (mostrarCargaCompleta) {
      this.cargando = true;
    }

    return this.reservaService
      .obtenerCalendarioDetalle(desde, hasta, tipoUnidad, codigoUnidad, estadosReserva, estadosEstancia)
      .pipe(
        tap((detalle) => {
          this.detalleCache = detalle ?? [];
          this.cacheDesde = this.inicioDelDia(desdeConsulta);
          this.cacheHasta = this.inicioDelDia(hastaConsulta);
          this.cacheClaveFiltros = claveFiltros;
          this.actualizarVistaDesdeCache();
          this.reservaSeleccionada = null;
          this.estanciaSeleccionada = null;
        }),
        catchError((errorResponse: unknown) => {
          this.error = extractBackendErrorMessage(
            errorResponse,
            'No fue posible cargar el calendario operativo.',
          );
          if (this.detalleCache.length === 0) {
            this.filas = [];
            this.registrosPorFilaDia.clear();
          }
          return of(null);
        }),
        finalize(() => {
          this.cargando = false;
        }),
      );
  }

  private actualizarVistaDesdeCache(): void {
    this.filas = this.construirFilas(this.detalleCache);
    this.registrosPorFilaDia = this.construirMapaRegistrosPorDia(this.filas);
    this.segmentosPorFila = this.construirSegmentosPorFila(this.filas);
    this.ajustarSeleccionSegunVista();
  }

  private construirSegmentosPorFila(
    filas: CalendarioFilaView[],
  ): Map<string, CalendarioSegmentoView[]> {
    const resultado = new Map<string, CalendarioSegmentoView[]>();
    const inicioVisible = this.inicioDelDia(this.semanaInicio);
    const finVisible = this.inicioDelDia(this.ultimoDiaVisible());

    filas.forEach((fila) => {
      const segmentosBase: CalendarioSegmentoView[] = [];

      fila.registros.forEach((registro) => {
        const inicio = this.parsearFechaLocal(registro.inicio);
        const fin = this.parsearFechaLocal(registro.fin);
        if (!inicio || !fin) {
          return;
        }

        const inicioRecorte = inicio < inicioVisible ? inicioVisible : inicio;
        const finRecorte = fin > finVisible ? finVisible : fin;
        if (finRecorte < inicioVisible || inicioRecorte > finVisible) {
          return;
        }

        const inicioIndice = this.diferenciaDias(inicioVisible, inicioRecorte);
        const finIndice = this.diferenciaDias(inicioVisible, finRecorte);

        if (inicioIndice < 0 || finIndice < 0 || inicioIndice > finIndice) {
          return;
        }

        segmentosBase.push({
          registro,
          inicioIndice,
          finIndice,
          nivel: 0,
        });
      });

      segmentosBase.sort((a, b) => {
        if (a.inicioIndice !== b.inicioIndice) {
          return a.inicioIndice - b.inicioIndice;
        }
        return b.finIndice - a.finIndice;
      });

      const finPorNivel: number[] = [];
      const segmentosNivelados = segmentosBase.map((segmento) => {
        let nivel = finPorNivel.findIndex((finNivel) => segmento.inicioIndice > finNivel);
        if (nivel === -1) {
          finPorNivel.push(segmento.finIndice);
          nivel = finPorNivel.length - 1;
        } else {
          finPorNivel[nivel] = segmento.finIndice;
        }

        return {
          ...segmento,
          nivel,
        };
      });

      resultado.set(fila.id, segmentosNivelados);
    });

    return resultado;
  }

  private cubreRangoEnCache(desde: Date, hasta: Date): boolean {
    if (!this.cacheDesde || !this.cacheHasta) {
      return false;
    }
    return this.cacheDesde.getTime() <= desde.getTime() && this.cacheHasta.getTime() >= hasta.getTime();
  }

  private construirFilas(detalle: DetalleCalendarioUnidadDTO[]): CalendarioFilaView[] {
    const filas: CalendarioFilaView[] = [];
    const mostrarSoloHabitaciones = this.filtroTipoUnidad === 'HABITACION';
    const unidadesOrdenadas = [...detalle].sort((a, b) => {
      const prioridad = (tipo: TipoUnidad | null | undefined): number => {
        if (tipo === 'APARTAMENTO') {
          return 0;
        }
        if (tipo === 'APARTAESTUDIO') {
          return 1;
        }
        return 2;
      };

      const porTipo = prioridad(a.unidad?.tipo) - prioridad(b.unidad?.tipo);
      if (porTipo !== 0) {
        return porTipo;
      }

      return (a.unidad?.codigo ?? '').localeCompare(b.unidad?.codigo ?? '', 'es', {
        sensitivity: 'base',
      });
    });

    unidadesOrdenadas.forEach((item) => {
      const codigoUnidad = item.unidad?.codigo ?? 'Unidad';
      const tipoUnidad = item.unidad?.tipo ?? null;

      const filasHabitaciones: CalendarioFilaView[] = [];
      const registrosIndividualesHabitaciones: CalendarioRegistroView[] = [];

      if (item.unidad?.tipo === 'APARTAMENTO' || mostrarSoloHabitaciones) {
        (item.habitaciones ?? []).forEach((habitacion) => {
          const codigoHabitacion = habitacion.habitacion?.codigo ?? 'Habitacion';
          const registrosHabitacion = [
            ...this.mapearReservas(habitacion.reservas ?? [], codigoHabitacion, 'HABITACION', 'habitacion'),
            ...this.mapearEstancias(
              habitacion.estancias ?? [],
              codigoHabitacion,
              'HABITACION',
              habitacion.habitacion?.informacionAdicional,
              'habitacion',
            ),
          ];

          registrosHabitacion
            .filter((registro) => registro.modoOcupacion === 'INDIVIDUAL')
            .forEach((registro) => registrosIndividualesHabitaciones.push(registro));

          filasHabitaciones.push({
            id: `habitacion-${habitacion.habitacion.id}`,
            nombre: codigoHabitacion,
            subtitulo: `Hab. de ${codigoUnidad}`,
            tipoUnidad: 'HABITACION',
            nivel: 'habitacion',
            esHabitacionDeApartamento: true,
            informacionAdicional: habitacion.habitacion?.informacionAdicional ?? null,
            registros: registrosHabitacion,
          });
        });
      }

      const registrosUnidad = [
        ...this.mapearReservas(item.reservas ?? [], codigoUnidad, tipoUnidad, 'unidad'),
        ...this.mapearEstancias(
          item.estancias ?? [],
          codigoUnidad,
          tipoUnidad,
          item.unidad?.informacionAdicional,
          'unidad',
        ),
      ];

      const registrosUnidadNormalizados = this.normalizarRegistrosUnidadApartamento(
        registrosUnidad,
        tipoUnidad,
        registrosIndividualesHabitaciones,
        codigoUnidad,
      );

      if (!mostrarSoloHabitaciones) {
        filas.push({
          id: `unidad-${item.unidad.id}`,
          nombre: codigoUnidad,
          subtitulo: this.formatearEtiqueta(tipoUnidad),
          tipoUnidad,
          nivel: 'unidad',
          esHabitacionDeApartamento: false,
          informacionAdicional: item.unidad?.informacionAdicional ?? null,
          registros: registrosUnidadNormalizados,
        });
      }

      filas.push(...filasHabitaciones);
    });

    return filas;
  }

  private mapearReservas(
    reservas: ReservaDTO[],
    codigoUnidad: string,
    tipoUnidad: TipoUnidad | null,
    destino: 'unidad' | 'habitacion',
  ): CalendarioRegistroView[] {
    return reservas
      .filter((reserva) => this.debePintarRegistroSegunModo(tipoUnidad, destino))
      .filter((reserva) => {
        const estado = (reserva.estadoReserva ?? 'CONFIRMADA') as EstadoReserva;
        return this.filtroEstadosReserva.includes(estado);
      })
      .filter((reserva) => !!reserva.id && !!reserva.entradaEstimada && !!reserva.salidaEstimada)
      .map((reserva) => ({
        id: reserva.id,
        tipo: 'RESERVA' as const,
        etiqueta: reserva.codigoReserva || `RES-${reserva.id}`,
        estado: reserva.estadoReserva ?? 'CONFIRMADA',
        modoOcupacion: reserva.modoOcupacion ?? null,
        inicio: reserva.entradaEstimada,
        fin: reserva.salidaEstimada,
        codigoUnidad,
        tipoUnidad,
        numeroPersonas: reserva.numeroPersonas ?? null,
        nombreCliente: this.obtenerNombreClienteReserva(reserva),
        idCliente: reserva.cliente?.id ?? null,
        codigoHabitacion: codigoUnidad,
        reserva,
      }));
  }

  private mapearEstancias(
    estancias: EstanciaDTO[],
    codigoUnidad: string,
    tipoUnidad: TipoUnidad | null,
    informacionAdicional: string | null | undefined,
    destino: 'unidad' | 'habitacion',
  ): CalendarioRegistroView[] {
    return estancias
      .filter((estancia) =>
        this.debePintarRegistroSegunModo(tipoUnidad, destino)
      )
      .filter(
        (estancia) =>
          !!estancia.id &&
          !!estancia.entradaReal &&
          !!(estancia.salidaEstimada || estancia.salidaReal),
      )
      .reduce<CalendarioRegistroView[]>((acumulado, estancia) => {
        const ocupantes = this.obtenerOcupantesEstancia(estancia);
        const estadoEstancia = this.resolverEstadoEstancia(estancia, informacionAdicional);
        if (!this.filtroEstadosEstancia.includes(estadoEstancia)) {
          return acumulado;
        }
        const fechaFin =
          estadoEstancia === 'FINALIZADA' && estancia.salidaReal
            ? estancia.salidaReal
            : estancia.salidaEstimada || estancia.salidaReal || estancia.entradaReal;
        const cliente = ocupantes.find((ocupante) => ocupante.tipoOcupante === 'CLIENTE');
        acumulado.push({
          id: estancia.id,
          tipo: 'ESTANCIA' as const,
          etiqueta: estancia.codigoFolio || `EST-${estancia.id}`,
          estado: estadoEstancia,
          modoOcupacion: estancia.modoOcupacion ?? null,
          inicio: estancia.entradaReal,
          fin: fechaFin,
          codigoUnidad,
          tipoUnidad,
          numeroPersonas: ocupantes.length,
          nombreCliente: cliente ? `${cliente.nombres} ${cliente.apellidos}`.trim() : null,
          idCliente: cliente?.id ?? null,
          codigoHabitacion: codigoUnidad,
          estancia,
        });
        return acumulado;
      }, []);
  }

  private normalizarRegistrosUnidadApartamento(
    registros: CalendarioRegistroView[],
    tipoUnidad: TipoUnidad | null,
    registrosHabitacionesIndividuales: CalendarioRegistroView[],
    codigoUnidadPadre: string,
  ): CalendarioRegistroView[] {
    if (tipoUnidad !== 'APARTAMENTO') {
      return registros;
    }

    const completos = registros.filter((registro) => registro.modoOcupacion !== 'INDIVIDUAL');
    const seleccionados = this.construirResumenesIndividualesUnidad(
      registrosHabitacionesIndividuales,
      codigoUnidadPadre,
    );

    return [...completos, ...seleccionados];
  }

  private construirResumenesIndividualesUnidad(
    registrosIndividualesHabitaciones: CalendarioRegistroView[],
    codigoUnidadPadre: string,
  ): CalendarioRegistroView[] {
    const resumenes: CalendarioRegistroView[] = [];

    (['RESERVA', 'ESTANCIA'] as const).forEach((tipoRegistro) => {
      const grupo = registrosIndividualesHabitaciones.filter(
        (registro) => registro.tipo === tipoRegistro && registro.modoOcupacion === 'INDIVIDUAL',
      );

      if (!grupo.length) {
        return;
      }

      const intervalos = this.construirIntervalosContiguos(grupo);

      intervalos.forEach((intervalo, indice) => {
        const base =
          grupo.find((registro) => this.registroIncluyeDia(registro, intervalo.inicio)) ??
          grupo[0];

        resumenes.push({
          ...base,
          id: base.id + indice,
          inicio: this.formatearFechaIsoLocal(intervalo.inicio, false),
          fin: this.formatearFechaIsoLocal(intervalo.fin, false),
          codigoUnidad: codigoUnidadPadre,
          codigoHabitacion: null,
          mostrarResumenIndividualUnidad: true,
          habitacionesAfectadas: intervalo.habitaciones,
          etiqueta: `${tipoRegistro === 'RESERVA' ? 'Reserva' : 'Estancia'} individual`,
        });
      });
    });

    return resumenes;
  }

  private construirIntervalosContiguos(
    registros: CalendarioRegistroView[],
  ): Array<{ inicio: Date; fin: Date; habitaciones: string[] }> {
    const porDia = new Map<string, Set<string>>();

    registros.forEach((registro) => {
      const inicio = this.parsearFechaLocal(registro.inicio);
      const fin = this.parsearFechaLocal(registro.fin);
      if (!inicio || !fin) {
        return;
      }

      const cursor = new Date(inicio);
      while (cursor <= fin) {
        const clave = this.formatearDia(cursor);
        const habitacionesDia = porDia.get(clave) ?? new Set<string>();
        if (registro.codigoUnidad?.trim()) {
          habitacionesDia.add(registro.codigoUnidad);
        }
        porDia.set(clave, habitacionesDia);
        cursor.setDate(cursor.getDate() + 1);
      }
    });

    const diasOrdenados = Array.from(porDia.keys()).sort((a, b) => a.localeCompare(b));
    if (!diasOrdenados.length) {
      return [];
    }

    const intervalos: Array<{ inicio: Date; fin: Date; habitaciones: Set<string> }> = [];
    let inicioActual = this.parsearFechaLocal(diasOrdenados[0]);
    let finActual = this.parsearFechaLocal(diasOrdenados[0]);
    let habitacionesActuales = new Set<string>(porDia.get(diasOrdenados[0]) ?? []);

    for (let i = 1; i < diasOrdenados.length; i += 1) {
      const dia = this.parsearFechaLocal(diasOrdenados[i]);
      if (!dia || !finActual || !inicioActual) {
        continue;
      }

      const esperado = new Date(finActual);
      esperado.setDate(esperado.getDate() + 1);
      const esContiguo = this.formatearDia(esperado) === diasOrdenados[i];

      if (esContiguo) {
        finActual = dia;
        (porDia.get(diasOrdenados[i]) ?? []).forEach((codigo) => habitacionesActuales.add(codigo));
        continue;
      }

      intervalos.push({
        inicio: inicioActual,
        fin: finActual,
        habitaciones: habitacionesActuales,
      });

      inicioActual = dia;
      finActual = dia;
      habitacionesActuales = new Set<string>(porDia.get(diasOrdenados[i]) ?? []);
    }

    if (inicioActual && finActual) {
      intervalos.push({
        inicio: inicioActual,
        fin: finActual,
        habitaciones: habitacionesActuales,
      });
    }

    return intervalos.map((intervalo) => ({
      inicio: intervalo.inicio,
      fin: intervalo.fin,
      habitaciones: Array.from(intervalo.habitaciones).sort((a, b) =>
        a.localeCompare(b, 'es', { sensitivity: 'base' }),
      ),
    }));
  }

  private construirMapaRegistrosPorDia(
    filas: CalendarioFilaView[],
  ): Map<string, Map<string, CalendarioRegistroView[]>> {
    const mapaFilas = new Map<string, Map<string, CalendarioRegistroView[]>>();

    filas.forEach((fila) => {
      const mapaDias = new Map<string, CalendarioRegistroView[]>();

      this.diasSemana.forEach((dia) => {
        const claveDia = this.formatearDia(dia);
        const registrosDia = fila.registros.filter((registro) => this.registroIncluyeDia(registro, dia));
        mapaDias.set(claveDia, registrosDia);
      });

      mapaFilas.set(fila.id, mapaDias);
    });

    return mapaFilas;
  }

  private registroIncluyeDia(registro: CalendarioRegistroView, dia: Date): boolean {
    const inicio = this.parsearFechaLocal(registro.inicio);
    const fin = this.parsearFechaLocal(registro.fin);
    if (!inicio || !fin) {
      return false;
    }

    const diaComparacion = new Date(dia.getFullYear(), dia.getMonth(), dia.getDate());
    return diaComparacion >= inicio && diaComparacion <= fin;
  }

  private resolverEstadoEstancia(
    estancia: EstanciaDTO,
    informacionAdicional: string | null | undefined,
  ): EstadoEstancia {
    if (estancia.estado) {
      return estancia.estado;
    }

    if (!informacionAdicional?.trim()) {
      return 'ACTIVA';
    }

    try {
      const informacion = JSON.parse(informacionAdicional) as {
        ESTANCIA?: { idEstancia?: number | string; estado?: EstadoEstancia };
      };

      const idInfo = Number(informacion?.ESTANCIA?.idEstancia);
      if (idInfo === estancia.id && informacion.ESTANCIA?.estado) {
        return informacion.ESTANCIA.estado;
      }
    } catch {
      return 'ACTIVA';
    }

    return 'ACTIVA';
  }

  private obtenerNombreClienteReserva(reserva: ReservaDTO): string | null {
    const nombres = reserva.cliente?.nombres?.trim() ?? '';
    const apellidos = reserva.cliente?.apellidos?.trim() ?? '';
    const nombre = `${nombres} ${apellidos}`.trim();
    return nombre || null;
  }

  private obtenerOcupantesEstancia(estancia: EstanciaDTO) {
    const cliente = estancia.cliente ? [estancia.cliente] : [];
    return [...cliente, ...(estancia.acompanantes ?? [])];
  }

  private debePintarRegistroSegunModo(
    tipoUnidad: TipoUnidad | null,
    destino: 'unidad' | 'habitacion',
  ): boolean {
    if (tipoUnidad === 'APARTAMENTO') {
      return destino === 'unidad' || destino === 'habitacion';
    }

    if (tipoUnidad === 'HABITACION') {
      return destino === 'habitacion';
    }

    if (tipoUnidad !== 'APARTAESTUDIO') {
      return destino === 'unidad';
    }

    return destino === 'unidad';
  }

  textoBadge(registro: CalendarioRegistroView, fila: CalendarioFilaView, duracionDias: number): string {
    const esAmarilloUnidadIndividual =
      fila.tipoUnidad === 'APARTAMENTO' &&
      fila.nivel === 'unidad' &&
      registro.modoOcupacion === 'INDIVIDUAL';

    const esAmarilloHabitacionCompleto =
      fila.esHabitacionDeApartamento &&
      fila.nivel === 'habitacion' &&
      registro.modoOcupacion === 'COMPLETO';

    if (esAmarilloUnidadIndividual || esAmarilloHabitacionCompleto) {
      const tipo = registro.tipo === 'RESERVA' ? 'Reserva' : 'Estancia';
      const tipoCorto = registro.tipo === 'RESERVA' ? 'R' : 'E';
      const modo = registro.modoOcupacion === 'INDIVIDUAL' ? 'individual' : 'completa';
      if (duracionDias <= 1) {
        return `${tipoCorto} ${modo}`;
      }
      return `${tipo} ${modo}`;
    }

    return registro.nombreCliente?.trim() || 'Sin nombre';
  }

  esBadgeAmarillo(registro: CalendarioRegistroView, fila: CalendarioFilaView): boolean {
    return (
      (fila.tipoUnidad === 'APARTAMENTO' &&
        fila.nivel === 'unidad' &&
        registro.modoOcupacion === 'INDIVIDUAL') ||
      (fila.esHabitacionDeApartamento &&
        fila.nivel === 'habitacion' &&
        registro.modoOcupacion === 'COMPLETO')
    );
  }

  manejarClickSegmento(registro: CalendarioRegistroView, fila: CalendarioFilaView): void {
    if (this.esBadgeAmarillo(registro, fila)) {
      return;
    }
    this.abrirDetalleRegistro(registro);
  }

  puedeGestionarEstanciaModal(item: CalendarioRegistroView): boolean {
    if (!item.estancia?.id) {
      return false;
    }

    return item.estado === 'ACTIVA' || item.estado === 'EXCEDIDA';
  }

  puedeEditarEliminarReservaModal(item: CalendarioRegistroView): boolean {
    if (!item.reserva?.id) {
      return false;
    }

    return item.estado === 'CONFIRMADA' || item.estado === 'EXPIRADA';
  }

  puedeDarEntradaReservaModal(item: CalendarioRegistroView): boolean {
    if (!item.reserva?.id) {
      return false;
    }

    return item.estado === 'CONFIRMADA' || item.estado === 'EXPIRADA';
  }

  editarReservaDesdeModal(item: CalendarioRegistroView): void {
    if (!this.puedeEditarEliminarReservaModal(item) || this.eliminandoReserva || !item.reserva?.id) {
      return;
    }

    this.cerrarDetalle();
    this.router.navigate(['/reservas/nueva'], {
      state: {
        editMode: true,
        returnTo: this.router.url,
        reserva: {
          id: item.reserva.id,
          codigo: item.codigoUnidad,
          tipoUnidad: item.tipoUnidad ?? undefined,
          idOcupante: item.reserva.cliente?.id ?? undefined,
          nombreCliente: item.nombreCliente ?? undefined,
          numeroPersonas: item.numeroPersonas ?? undefined,
          canalReserva: item.reserva.canalReserva,
          entradaEstimada: item.reserva.entradaEstimada,
          salidaEstimada: item.reserva.salidaEstimada,
        },
      },
    });
  }

  darEntradaReservaDesdeModal(item: CalendarioRegistroView): void {
    if (!this.puedeDarEntradaReservaModal(item) || this.eliminandoReserva || !item.reserva?.id) {
      return;
    }

    this.cerrarDetalle();
    this.router.navigate(['/estancias/nueva'], {
      state: {
        returnTo: this.router.url,
        flujo: 'INGRESO',
        idReserva: item.reserva.id,
        idEstancia: item.reserva.idEstancia ?? undefined,
        idCliente: item.reserva.cliente?.id ?? undefined,
        codigoReserva: item.reserva.codigoReserva ?? '',
        nombreCliente: item.nombreCliente ?? '',
        codigo: item.codigoUnidad,
        tipo: item.tipoUnidad,
        entrada: item.reserva.entradaEstimada,
        salida: item.reserva.salidaEstimada,
        numeroPersonasReserva: item.reserva.numeroPersonas,
      },
    });
  }

  eliminarReservaDesdeModal(item: CalendarioRegistroView): void {
    if (!this.puedeEditarEliminarReservaModal(item) || this.eliminandoReserva || !item.reserva?.id) {
      return;
    }

    this.reservaPendienteEliminar = item;
    this.mostrarModalEliminarReserva = true;
  }

  cerrarModalEliminarReserva(): void {
    if (this.eliminandoReserva) {
      return;
    }

    this.mostrarModalEliminarReserva = false;
    this.reservaPendienteEliminar = null;
  }

  confirmarEliminarReservaDesdeModal(): void {
    const item = this.reservaPendienteEliminar;
    if (!item?.reserva?.id || this.eliminandoReserva) {
      return;
    }

    this.eliminandoReserva = true;
    this.error = '';
    this.reservaService.eliminarReserva(item.reserva.id).subscribe({
      next: () => {
        this.eliminandoReserva = false;
        this.mostrarModalEliminarReserva = false;
        this.reservaPendienteEliminar = null;
        this.cerrarDetalle();
        this.cargarCalendarioSemana();
      },
      error: (errorResponse: unknown) => {
        this.eliminandoReserva = false;
        this.mostrarModalEliminarReserva = false;
        this.reservaPendienteEliminar = null;
        this.error = extractBackendErrorMessage(errorResponse, 'No fue posible eliminar la reserva.');
      },
    });
  }

  editarEstanciaDesdeModal(item: CalendarioRegistroView): void {
    if (!this.puedeGestionarEstanciaModal(item) || this.eliminandoEstancia) {
      return;
    }

    this.cerrarDetalle();
    this.router.navigate(['/estancias/nueva'], {
      queryParams: {
        codigo: item.codigoUnidad,
        tipo: item.tipoUnidad,
        editar: true,
        estanciaId: item.estancia?.id,
      },
      state: { returnTo: this.router.url },
    });
  }

  eliminarEstanciaDesdeModal(item: CalendarioRegistroView): void {
    if (!this.puedeGestionarEstanciaModal(item) || this.eliminandoEstancia || !item.estancia?.id) {
      return;
    }

    this.estanciaPendienteEliminar = item;
    this.mostrarModalEliminarEstancia = true;
  }

  cerrarModalEliminarEstancia(): void {
    if (this.eliminandoEstancia) {
      return;
    }

    this.mostrarModalEliminarEstancia = false;
    this.estanciaPendienteEliminar = null;
  }

  confirmarEliminarEstanciaDesdeModal(): void {
    const item = this.estanciaPendienteEliminar;
    if (!item?.estancia?.id || this.eliminandoEstancia) {
      return;
    }

    this.eliminandoEstancia = true;
    this.error = '';
    this.estanciaService.eliminarEstancia(item.estancia.id).subscribe({
      next: () => {
        this.eliminandoEstancia = false;
        this.mostrarModalEliminarEstancia = false;
        this.estanciaPendienteEliminar = null;
        this.cerrarDetalle();
        this.cargarCalendarioSemana();
      },
      error: (errorResponse: unknown) => {
        this.eliminandoEstancia = false;
        this.mostrarModalEliminarEstancia = false;
        this.estanciaPendienteEliminar = null;
        this.error = extractBackendErrorMessage(errorResponse, 'No fue posible eliminar la estancia.');
      },
    });
  }

  darSalidaDesdeModal(item: CalendarioRegistroView): void {
    if (!this.puedeGestionarEstanciaModal(item) || this.eliminandoEstancia || !item.estancia?.id) {
      return;
    }

    this.estanciaService.obtenerEstanciaPorId(item.estancia.id).subscribe({
      next: (estancia) => {
        const ocupantes = this.obtenerOcupantesEstancia(estancia);
        const cliente = ocupantes.find((ocupante) => ocupante.tipoOcupante === 'CLIENTE');
        const nombreCliente = [cliente?.nombres, cliente?.apellidos]
          .filter((valor) => Boolean(valor?.trim()))
          .join(' ');

        this.cerrarDetalle();
        this.router.navigate(['/estancias/salida'], {
          state: {
            returnTo: this.router.url,
            estanciaId: estancia.id,
            codigo: item.codigoUnidad,
            tipo: item.tipoUnidad,
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
          'No fue posible preparar la finalizacion de la estancia.',
        );
      },
    });
  }

  formatearHabitacionesAfectadas(habitaciones: string[] | null | undefined): string {
    if (!habitaciones?.length) {
      return '-';
    }
    return habitaciones.join(', ');
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

  private obtenerInicioSemanaLunes(fecha: Date): Date {
    const base = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
    const diaSemana = base.getDay();
    const desplazamiento = diaSemana === 0 ? -6 : 1 - diaSemana;
    base.setDate(base.getDate() + desplazamiento);
    return base;
  }

  private ultimoDiaVisible(): Date {
    const fin = new Date(this.semanaInicio);
    fin.setDate(fin.getDate() + (this.diasVisibles - 1));
    return fin;
  }

  private inicioDelDia(fecha: Date): Date {
    return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
  }

  private diferenciaDias(inicio: Date, fin: Date): number {
    const msDia = 24 * 60 * 60 * 1000;
    return Math.floor((this.inicioDelDia(fin).getTime() - this.inicioDelDia(inicio).getTime()) / msDia);
  }

  private formatearDia(fecha: Date): string {
    const year = fecha.getFullYear();
    const month = `${fecha.getMonth() + 1}`.padStart(2, '0');
    const day = `${fecha.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatearFechaIsoLocal(fecha: Date, finDelDia: boolean): string {
    const date = new Date(fecha);
    if (finDelDia) {
      date.setHours(23, 59, 59, 0);
    } else {
      date.setHours(0, 0, 0, 0);
    }

    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    const hours = `${date.getHours()}`.padStart(2, '0');
    const minutes = `${date.getMinutes()}`.padStart(2, '0');
    const seconds = `${date.getSeconds()}`.padStart(2, '0');
    const millis = `${date.getMilliseconds()}`.padStart(3, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}.${millis}`;
  }

  private construirClaveFiltros(
    tipoUnidad: TipoUnidad | undefined,
    codigoUnidad: string | undefined,
    estadosReserva: EstadoReserva[],
    estadosEstancia: EstadoEstancia[],
  ): string {
    const reservas = [...estadosReserva].sort().join(',');
    const estancias = [...estadosEstancia].sort().join(',');
    return `${tipoUnidad ?? ''}|${codigoUnidad ?? ''}|${reservas}|${estancias}`;
  }

  private obtenerPrefillSeleccion():
    | {
        codigo: string;
        tipo: TipoUnidad;
        entrada: string;
      }
    | undefined {
    if (!this.seleccion) {
      return undefined;
    }

    const fila = this.filas.find((item) => item.id === this.seleccion?.filaId);
    if (!fila) {
      return undefined;
    }

    if (fila.nivel === 'habitacion') {
      return {
        codigo: fila.nombre,
        tipo: 'HABITACION',
        entrada: this.seleccion.dia,
      };
    }

    if (!fila.tipoUnidad) {
      return undefined;
    }

    return {
      codigo: fila.nombre,
      tipo: fila.tipoUnidad,
      entrada: this.seleccion.dia,
    };
  }

  private mostrarTooltipSeleccionTemporal(filaId: string, dia: string): void {
    this.tooltipSeleccionVisible = { filaId, dia };

    if (this.tooltipSeleccionTimeout) {
      clearTimeout(this.tooltipSeleccionTimeout);
    }

    this.tooltipSeleccionTimeout = setTimeout(() => {
      this.tooltipSeleccionVisible = null;
      this.tooltipSeleccionTimeout = null;
    }, 1800);
  }

  private ocultarTooltipSeleccion(): void {
    this.tooltipSeleccionVisible = null;
    if (this.tooltipSeleccionTimeout) {
      clearTimeout(this.tooltipSeleccionTimeout);
      this.tooltipSeleccionTimeout = null;
    }
  }

  private ajustarSeleccionSegunVista(): void {
    if (!this.seleccion) {
      return;
    }

    const fila = this.filas.find((item) => item.id === this.seleccion?.filaId);
    if (!fila) {
      this.seleccion = null;
      this.ocultarTooltipSeleccion();
      return;
    }

    const fechaSeleccionada = this.parsearFechaLocal(this.seleccion.dia);
    if (!fechaSeleccionada || this.esCuadroBloqueadoParaSeleccion(fila, fechaSeleccionada)) {
      this.seleccion = null;
      this.ocultarTooltipSeleccion();
    }
  }
}
