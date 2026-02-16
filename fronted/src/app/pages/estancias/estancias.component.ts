import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { PageResponse } from '../../models/page.model';
import { EstadoEstancia, TipoUnidad } from '../../models/enums';
import { EstanciaTablaFiltros, EstanciaTablaItem } from '../../models/estancia-tabla.model';
import { EstanciaService } from '../../services/estancia.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

type RangoRapido = 'HOY' | 'MANANA' | 'ESTA_SEMANA' | 'PROXIMOS_7_DIAS' | 'ESTE_MES';
type RangoRapidoCampo = 'rangoGeneral' | 'entrada' | 'salidaEstimada' | 'salidaReal';
type ResumenFiltroClave =
  | 'tipoUnidad'
  | 'estados'
  | 'reservaAsociada'
  | 'entrada'
  | 'salidaEstimada'
  | 'salidaReal';

interface ResumenFiltroItem {
  clave: ResumenFiltroClave;
  texto: string;
}

@Component({
  selector: 'app-estancias',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './estancias.component.html',
  styleUrl: './estancias.component.css',
})
export class EstanciasComponent implements OnInit, OnDestroy {
  readonly estadosEstanciaDisponibles: EstadoEstancia[] = [
    'ACTIVA',
    'RESERVADA',
    'FINALIZADA',
    'EXCEDIDA',
    'CANCELADA',
  ];

  readonly tiposUnidadChipDisponibles: TipoUnidad[] = ['APARTAMENTO', 'APARTAESTUDIO', 'HABITACION'];

  readonly pageSizeOptions = [10, 20, 50, 100];

  filtros: EstanciaTablaFiltros = this.crearFiltrosVacios();

  estancias: EstanciaTablaItem[] = [];
  cargando = false;
  error = '';
  accionError = '';
  accionExito = '';

  page = 0;
  size = 20;
  totalElements = 0;
  totalPages = 0;
  estanciaSeleccionadaId: number | null = null;
  eliminandoEstanciaId: number | null = null;
  filtrosAvanzadosAbiertos = false;
  rangoRapidoSeleccionado: Record<RangoRapidoCampo, RangoRapido | null> = {
    rangoGeneral: null,
    entrada: null,
    salidaEstimada: null,
    salidaReal: null,
  };

  private queryParamsSub: Subscription | null = null;

  constructor(
    private readonly estanciaService: EstanciaService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.queryParamsSub = this.route.queryParamMap.subscribe((params) => {
      const codigoEstancia = (params.get('codigoEstancia') ?? '').trim();
      if (codigoEstancia !== this.filtros.codigoEstancia) {
        this.filtros.codigoEstancia = codigoEstancia;
      }
      this.page = 0;
      this.cargarEstancias();
    });
  }

  ngOnDestroy(): void {
    this.queryParamsSub?.unsubscribe();
    this.queryParamsSub = null;
  }

  aplicarFiltros(): void {
    this.accionError = '';
    this.accionExito = '';
    this.page = 0;
    this.cargarEstancias();
  }

  limpiarFiltros(): void {
    this.filtros = this.crearFiltrosVacios();
    this.rangoRapidoSeleccionado = {
      rangoGeneral: null,
      entrada: null,
      salidaEstimada: null,
      salidaReal: null,
    };
    this.accionError = '';
    this.accionExito = '';
    this.page = 0;
    this.cargarEstancias();
  }

  aplicarRangoRapido(rango: RangoRapido, campo: RangoRapidoCampo = 'rangoGeneral'): void {
    if (this.rangoRapidoSeleccionado[campo] === rango) {
      this.limpiarRangoCampo(campo);
      this.rangoRapidoSeleccionado[campo] = null;
      return;
    }

    const ahora = new Date();
    let desde: Date;
    let hasta: Date;

    switch (rango) {
      case 'HOY':
        desde = new Date(ahora);
        hasta = new Date(ahora);
        break;
      case 'MANANA': {
        const manana = new Date(ahora);
        manana.setDate(manana.getDate() + 1);
        desde = manana;
        hasta = manana;
        break;
      }
      case 'ESTA_SEMANA': {
        const inicioSemana = new Date(ahora);
        const diaSemana = inicioSemana.getDay();
        const desplazamientoLunes = diaSemana === 0 ? -6 : 1 - diaSemana;
        inicioSemana.setDate(inicioSemana.getDate() + desplazamientoLunes);
        const finSemana = new Date(inicioSemana);
        finSemana.setDate(finSemana.getDate() + 6);
        desde = inicioSemana;
        hasta = finSemana;
        break;
      }
      case 'PROXIMOS_7_DIAS': {
        const fin = new Date(ahora);
        fin.setDate(fin.getDate() + 6);
        desde = new Date(ahora);
        hasta = fin;
        break;
      }
      case 'ESTE_MES':
      default: {
        const inicioMes = new Date(ahora.getFullYear(), ahora.getMonth(), 1);
        const finMes = new Date(ahora.getFullYear(), ahora.getMonth() + 1, 0);
        desde = inicioMes;
        hasta = finMes;
        break;
      }
    }

    if (campo === 'rangoGeneral') {
      this.filtros.rangoGeneralDesde = this.formatearFechaInput(desde);
      this.filtros.rangoGeneralHasta = this.formatearFechaInput(hasta);
    } else {
      this.asignarRangoFechaHora(campo, desde, hasta);
    }

    this.rangoRapidoSeleccionado[campo] = rango;
  }

  esRangoRapidoSeleccionado(campo: RangoRapidoCampo, rango: RangoRapido): boolean {
    return this.rangoRapidoSeleccionado[campo] === rango;
  }

  limpiarSeleccionRangoRapido(campo: RangoRapidoCampo): void {
    this.rangoRapidoSeleccionado[campo] = null;
  }

  seleccionarTipoUnidad(tipo: TipoUnidad): void {
    this.filtros.tipoUnidad = this.filtros.tipoUnidad === tipo ? '' : tipo;
  }

  esTipoUnidadSeleccionado(tipo: TipoUnidad): boolean {
    return this.filtros.tipoUnidad === tipo;
  }

  seleccionarReservaAsociada(valor: '' | 'SI' | 'NO'): void {
    this.filtros.tieneReservaAsociada = this.filtros.tieneReservaAsociada === valor ? '' : valor;
  }

  esReservaAsociadaSeleccionada(valor: '' | 'SI' | 'NO'): boolean {
    return this.filtros.tieneReservaAsociada === valor;
  }

  onToggleFiltrosAvanzados(event: Event): void {
    const details = event.target as HTMLDetailsElement | null;
    this.filtrosAvanzadosAbiertos = !!details?.open;
  }

  get filtrosAvanzadosResumen(): ResumenFiltroItem[] {
    const items: ResumenFiltroItem[] = [];

    if (this.filtros.tipoUnidad) {
      items.push({
        clave: 'tipoUnidad',
        texto: `Tipo unidad: ${this.formatearEtiqueta(this.filtros.tipoUnidad)}`,
      });
    }

    if (this.filtros.estados.length) {
      const estados = this.filtros.estados.map((estado) => this.formatearEtiqueta(estado)).join(', ');
      items.push({ clave: 'estados', texto: `Estado: ${estados}` });
    }

    if (this.filtros.tieneReservaAsociada === 'SI') {
      items.push({ clave: 'reservaAsociada', texto: 'Reserva asociada: Con reserva' });
    } else if (this.filtros.tieneReservaAsociada === 'NO') {
      items.push({ clave: 'reservaAsociada', texto: 'Reserva asociada: Sin reserva' });
    }

    const entrada = this.construirResumenRango('Entrada real', this.filtros.entradaDesde, this.filtros.entradaHasta);
    if (entrada) {
      items.push({ clave: 'entrada', texto: entrada });
    }

    const salidaEstimada = this.construirResumenRango(
      'Salida estimada',
      this.filtros.salidaEstimadaDesde,
      this.filtros.salidaEstimadaHasta
    );
    if (salidaEstimada) {
      items.push({ clave: 'salidaEstimada', texto: salidaEstimada });
    }

    const salidaReal = this.construirResumenRango(
      'Salida real',
      this.filtros.salidaRealDesde,
      this.filtros.salidaRealHasta
    );
    if (salidaReal) {
      items.push({ clave: 'salidaReal', texto: salidaReal });
    }

    return items;
  }

  quitarFiltroAvanzado(clave: ResumenFiltroClave): void {
    switch (clave) {
      case 'tipoUnidad':
        this.filtros.tipoUnidad = '';
        break;
      case 'estados':
        this.filtros.estados = [];
        break;
      case 'reservaAsociada':
        this.filtros.tieneReservaAsociada = '';
        break;
      case 'entrada':
        this.filtros.entradaDesde = '';
        this.filtros.entradaHasta = '';
        this.rangoRapidoSeleccionado.entrada = null;
        break;
      case 'salidaEstimada':
        this.filtros.salidaEstimadaDesde = '';
        this.filtros.salidaEstimadaHasta = '';
        this.rangoRapidoSeleccionado.salidaEstimada = null;
        break;
      case 'salidaReal':
        this.filtros.salidaRealDesde = '';
        this.filtros.salidaRealHasta = '';
        this.rangoRapidoSeleccionado.salidaReal = null;
        break;
      default:
        return;
    }

    this.aplicarFiltros();
  }

  trackByResumenFiltro(_: number, item: ResumenFiltroItem): ResumenFiltroClave {
    return item.clave;
  }

  toggleEstado(estado: EstadoEstancia, checked: boolean): void {
    this.filtros.estados = checked
      ? Array.from(new Set([...this.filtros.estados, estado]))
      : this.filtros.estados.filter((item) => item !== estado);
  }

  estaEstadoSeleccionado(estado: EstadoEstancia): boolean {
    return this.filtros.estados.includes(estado);
  }

  cambiarPageSize(size: number): void {
    this.size = size;
    this.page = 0;
    this.cargarEstancias();
  }

  irPaginaAnterior(): void {
    if (this.page <= 0 || this.cargando) {
      return;
    }

    this.page -= 1;
    this.cargarEstancias();
  }

  irPaginaSiguiente(): void {
    if (this.cargando || this.page + 1 >= this.totalPages) {
      return;
    }

    this.page += 1;
    this.cargarEstancias();
  }

  trackByEstanciaId(_: number, estancia: EstanciaTablaItem): number {
    return estancia.id;
  }

  seleccionarEstancia(estancia: EstanciaTablaItem): void {
    this.estanciaSeleccionadaId =
      this.estanciaSeleccionadaId === estancia.id ? null : estancia.id;
  }

  esEstanciaSeleccionada(estancia: EstanciaTablaItem): boolean {
    return this.estanciaSeleccionadaId === estancia.id;
  }

  editarEstanciaSeleccionada(): void {
    const estancia = this.obtenerEstanciaSeleccionada();
    if (!estancia || !this.puedeGestionarEstancia(estancia)) {
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.router.navigate(['/estancias/nueva'], {
      queryParams: {
        codigo: estancia.codigoUnidad,
        tipo: estancia.tipoUnidad,
        editar: true,
        estanciaId: estancia.id,
      },
    });
  }

  eliminarEstanciaSeleccionada(): void {
    const estancia = this.obtenerEstanciaSeleccionada();
    if (!estancia || !this.puedeGestionarEstancia(estancia) || this.eliminandoEstanciaId !== null) {
      return;
    }

    const confirmacion = window.confirm(
      `Se eliminara la estancia ${estancia.codigoEstancia}. Esta accion no se puede deshacer.`
    );
    if (!confirmacion) {
      return;
    }

    this.eliminandoEstanciaId = estancia.id;
    this.accionError = '';
    this.accionExito = '';
    this.estanciaService.eliminarEstancia(estancia.id).subscribe({
      next: () => {
        this.eliminandoEstanciaId = null;
        this.accionExito = 'Estancia eliminada correctamente.';
        this.cargarEstancias();
      },
      error: (errorResponse: unknown) => {
        this.eliminandoEstanciaId = null;
        this.accionError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible eliminar la estancia.'
        );
        this.accionExito = '';
      },
    });
  }

  irAPagosPorEstancia(estancia: EstanciaTablaItem): void {
    if (!this.tienePagoAsociado(estancia)) {
      this.accionError = 'La estancia no tiene pago asociado para consultar.';
      this.accionExito = '';
      return;
    }

    const codigoEstancia = estancia.codigoEstancia?.trim() ?? '';
    if (!codigoEstancia) {
      this.accionError = 'La estancia no tiene codigo de estancia asociado para consultar pagos.';
      this.accionExito = '';
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.router.navigate(['/pagos'], {
      queryParams: { codigoEstancia },
    });
  }

  puedeGestionarEstanciaSeleccionada(): boolean {
    const estancia = this.obtenerEstanciaSeleccionada();
    return !!estancia && this.puedeGestionarEstancia(estancia);
  }

  estaEliminandoEstanciaSeleccionada(): boolean {
    const estancia = this.obtenerEstanciaSeleccionada();
    return !!estancia && this.eliminandoEstanciaId === estancia.id;
  }

  obtenerTooltipEditarSeleccionada(): string | null {
    return this.obtenerTooltipAccionGestionSeleccionada('editar');
  }

  obtenerTooltipEliminarSeleccionada(): string | null {
    return this.obtenerTooltipAccionGestionSeleccionada('eliminar');
  }

  irATablaReservas(estancia: EstanciaTablaItem): void {
    const codigoReserva = estancia.codigoReservaAsociada?.trim() ?? '';
    if (!codigoReserva) {
      this.accionError = 'La estancia no tiene codigo de reserva asociado.';
      this.accionExito = '';
      return;
    }

    this.accionError = '';
    this.accionExito = '';
    this.router.navigate(['/reservas'], {
      queryParams: { codigoReserva },
    });
  }

  irATablaClientes(estancia: EstanciaTablaItem): void {
    const numeroDocumento = estancia.numeroDocumentoCliente?.trim() ?? '';

    this.router.navigate(['/ocupantes/tabla-clientes'], {
      queryParams: {
        numeroDocumento,
      },
    });
  }

  formatearEtiqueta(valor: string | null | undefined): string {
    return (valor ?? '')
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
  }

  formatearFecha(fecha: string | null | undefined): string {
    if (!fecha) {
      return '-';
    }

    const date = new Date(fecha);
    if (Number.isNaN(date.getTime())) {
      return fecha;
    }

    return new Intl.DateTimeFormat('es-CO', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
  }

  formatearSoloFecha(fecha: string | null | undefined): string {
    if (!fecha) {
      return '-';
    }

    const date = new Date(fecha);
    if (Number.isNaN(date.getTime())) {
      return fecha;
    }

    return new Intl.DateTimeFormat('es-CO', {
      dateStyle: 'medium',
    }).format(date);
  }

  formatearSoloHora(fecha: string | null | undefined): string {
    if (!fecha) {
      return '-';
    }

    const date = new Date(fecha);
    if (Number.isNaN(date.getTime())) {
      return fecha;
    }

    return new Intl.DateTimeFormat('es-CO', {
      timeStyle: 'short',
    }).format(date);
  }

  formatearMontoPago(monto: number | null | undefined): string {
    const valor = monto ?? 0;
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0,
    }).format(valor);
  }

  tienePagosModificados(estancia: EstanciaTablaItem): boolean {
    return (estancia.cantidadPagosModificadosOEliminados ?? 0) > 0;
  }

  obtenerTooltipPagosModificados(estancia: EstanciaTablaItem): string {
    const cantidad = estancia.cantidadPagosModificadosOEliminados ?? 0;
    const etiqueta = cantidad === 1 ? 'pago modificado' : 'pagos modificados';
    return `Tiene ${cantidad} ${etiqueta}`;
  }

  tienePagoAsociado(estancia: EstanciaTablaItem): boolean {
    const totalPagoEstancia = estancia.totalPagoEstancia ?? 0;
    if (totalPagoEstancia > 0) {
      return true;
    }

    return this.tienePagosModificados(estancia);
  }

  get indiceInicio(): number {
    return this.totalElements === 0 ? 0 : this.page * this.size + 1;
  }

  get indiceFin(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }

  private cargarEstancias(): void {
    this.cargando = true;
    this.error = '';

    const filtros: EstanciaTablaFiltros = {
      ...this.filtros,
      rangoGeneralDesde: this.normalizarRangoGeneralDesde(this.filtros.rangoGeneralDesde),
      rangoGeneralHasta: this.normalizarRangoGeneralHasta(this.filtros.rangoGeneralHasta),
      entradaDesde: this.normalizarFechaHoraDesde(this.filtros.entradaDesde),
      entradaHasta: this.normalizarFechaHoraHasta(this.filtros.entradaHasta),
      salidaEstimadaDesde: this.normalizarFechaHoraDesde(this.filtros.salidaEstimadaDesde),
      salidaEstimadaHasta: this.normalizarFechaHoraHasta(this.filtros.salidaEstimadaHasta),
      salidaRealDesde: this.normalizarFechaHoraDesde(this.filtros.salidaRealDesde),
      salidaRealHasta: this.normalizarFechaHoraHasta(this.filtros.salidaRealHasta),
    };

    const sort = ['entradaReal,desc'];

    this.estanciaService.obtenerTabla(filtros, this.page, this.size, sort).subscribe({
      next: (response: PageResponse<EstanciaTablaItem>) => {
        this.estancias = response.content;
        if (this.estanciaSeleccionadaId !== null) {
          const estanciaSigueDisponible = this.estancias.some(
            (estancia) => estancia.id === this.estanciaSeleccionadaId
          );
          if (!estanciaSigueDisponible) {
            this.estanciaSeleccionadaId = null;
          }
        }
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.page = response.number ?? this.page;
        this.size = response.size;
        this.cargando = false;
      },
      error: (errorResponse: unknown) => {
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar la tabla de estancias.'
        );
        this.estancias = [];
        this.estanciaSeleccionadaId = null;
        this.eliminandoEstanciaId = null;
        this.totalElements = 0;
        this.totalPages = 0;
        this.cargando = false;
      },
    });
  }

  private crearFiltrosVacios(): EstanciaTablaFiltros {
    return {
      estados: [],
      modoOcupacion: '',
      tipoUnidad: '',
      codigoEstancia: '',
      codigoUnidad: '',
      nombreCliente: '',
      numeroDocumentoCliente: '',
      rangoGeneralDesde: '',
      rangoGeneralHasta: '',
      entradaDesde: '',
      entradaHasta: '',
      salidaEstimadaDesde: '',
      salidaEstimadaHasta: '',
      salidaRealDesde: '',
      salidaRealHasta: '',
      tieneReservaAsociada: '',
    };
  }

  private normalizarRangoGeneralDesde(fecha: string): string {
    const valor = fecha.trim();
    if (!valor) {
      return '';
    }

    return valor.length === 10 ? `${valor}T00:00:00` : this.normalizarFechaHoraDesde(valor);
  }

  private normalizarRangoGeneralHasta(fecha: string): string {
    const valor = fecha.trim();
    if (!valor) {
      return '';
    }

    return valor.length === 10 ? `${valor}T23:59:59` : this.normalizarFechaHoraHasta(valor);
  }

  private normalizarFechaHoraDesde(fecha: string): string {
    const valor = fecha.trim();
    if (!valor) {
      return '';
    }

    return valor.length === 16 ? `${valor}:00` : valor;
  }

  private normalizarFechaHoraHasta(fecha: string): string {
    const valor = fecha.trim();
    if (!valor) {
      return '';
    }

    return valor.length === 16 ? `${valor}:59` : valor;
  }

  private formatearFechaInput(fecha: Date): string {
    const year = fecha.getFullYear();
    const month = `${fecha.getMonth() + 1}`.padStart(2, '0');
    const day = `${fecha.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatearFechaHoraInput(fecha: Date): string {
    const year = fecha.getFullYear();
    const month = `${fecha.getMonth() + 1}`.padStart(2, '0');
    const day = `${fecha.getDate()}`.padStart(2, '0');
    const hours = `${fecha.getHours()}`.padStart(2, '0');
    const minutes = `${fecha.getMinutes()}`.padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  private construirResumenRango(etiqueta: string, desde: string, hasta: string): string | null {
    const desdeTexto = this.formatearFechaResumen(desde);
    const hastaTexto = this.formatearFechaResumen(hasta);

    if (!desdeTexto && !hastaTexto) {
      return null;
    }

    return `${etiqueta}: ${desdeTexto || '-'} a ${hastaTexto || '-'}`;
  }

  private formatearFechaResumen(valor: string): string {
    const texto = valor.trim();
    if (!texto) {
      return '';
    }

    const fecha = new Date(texto);
    if (Number.isNaN(fecha.getTime())) {
      return texto;
    }

    const incluyeHora = texto.includes('T');
    return new Intl.DateTimeFormat('es-CO', {
      dateStyle: 'medium',
      ...(incluyeHora ? { timeStyle: 'short' } : {}),
    }).format(fecha);
  }

  private asignarRangoFechaHora(campo: Exclude<RangoRapidoCampo, 'rangoGeneral'>, desde: Date, hasta: Date): void {
    const desdeInicio = new Date(desde);
    desdeInicio.setHours(0, 0, 0, 0);
    const hastaFin = new Date(hasta);
    hastaFin.setHours(23, 59, 0, 0);

    const desdeValor = this.formatearFechaHoraInput(desdeInicio);
    const hastaValor = this.formatearFechaHoraInput(hastaFin);

    if (campo === 'entrada') {
      this.filtros.entradaDesde = desdeValor;
      this.filtros.entradaHasta = hastaValor;
      return;
    }

    if (campo === 'salidaEstimada') {
      this.filtros.salidaEstimadaDesde = desdeValor;
      this.filtros.salidaEstimadaHasta = hastaValor;
      return;
    }

    this.filtros.salidaRealDesde = desdeValor;
    this.filtros.salidaRealHasta = hastaValor;
  }

  private limpiarRangoCampo(campo: RangoRapidoCampo): void {
    if (campo === 'rangoGeneral') {
      this.filtros.rangoGeneralDesde = '';
      this.filtros.rangoGeneralHasta = '';
      return;
    }

    if (campo === 'entrada') {
      this.filtros.entradaDesde = '';
      this.filtros.entradaHasta = '';
      return;
    }

    if (campo === 'salidaEstimada') {
      this.filtros.salidaEstimadaDesde = '';
      this.filtros.salidaEstimadaHasta = '';
      return;
    }

    this.filtros.salidaRealDesde = '';
    this.filtros.salidaRealHasta = '';
  }

  private puedeGestionarEstancia(estancia: EstanciaTablaItem): boolean {
    return estancia.estadoEstancia !== 'FINALIZADA' && estancia.estadoEstancia !== 'CANCELADA';
  }

  private obtenerTooltipAccionGestionSeleccionada(accion: 'editar' | 'eliminar'): string | null {
    const estancia = this.obtenerEstanciaSeleccionada();
    if (!estancia) {
      return `Selecciona una estancia para ${accion}.`;
    }

    if (this.eliminandoEstanciaId === estancia.id) {
      return 'Se esta eliminando la estancia seleccionada.';
    }

    if (!this.puedeGestionarEstancia(estancia)) {
      return `No se puede ${accion} una estancia finalizada o cancelada.`;
    }

    return null;
  }

  private obtenerEstanciaSeleccionada(): EstanciaTablaItem | null {
    if (this.estanciaSeleccionadaId === null) {
      return null;
    }

    return this.estancias.find((estancia) => estancia.id === this.estanciaSeleccionadaId) ?? null;
  }
}
