import { CommonModule, Location } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FlatpickrModule } from 'angularx-flatpickr';
import { Observable, concatMap, map, of } from 'rxjs';

import { ReservaService } from '../../services/reserva.service';
import { OcupanteService } from '../../services/ocupante.service';
import { HabitacionService } from '../../services/habitacion.service';
import { UnidadService } from '../../services/unidad.service';
import { PagoService } from '../../services/pago.service';
import {
  CanalReserva,
  MedioPago,
  TipoDocumento,
  TipoOcupante,
  TipoUnidad,
} from '../../models/enums';
import { ReservaNuevoRequest } from '../../models/reserva.model';
import { OcupanteDTO, OcupanteNuevoRequest } from '../../models/ocupante.model';
import { PagoNuevoRequest } from '../../models/pago.model';
import { getCurrentDateInput } from '../../core/utils/date-time.util';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import { getPreviousNavigationUrl } from '../../core/utils/navigation-return.util';

interface ReservaEditState {
  editMode?: boolean;
  returnTo?: string;
  reserva?: {
    id?: number;
    codigo?: string;
    tipoUnidad?: TipoUnidad;
    idOcupante?: number;
    nombreCliente?: string;
    numeroPersonas?: number;
    canalReserva?: CanalReserva;
    entradaEstimada?: string;
    salidaEstimada?: string;
  };
}

@Component({
  selector: 'app-reserva-nueva',
  standalone: true,
  imports: [CommonModule, FormsModule, FlatpickrModule],
  templateUrl: './reserva-nueva.component.html',
  styleUrl: './reserva-nueva.component.css',
})
export class ReservaNuevaComponent implements OnInit {
  private readonly mensajeExito = 'Operacion realizada con exito.';

  esEdicion = false;
  reservaEditandoId: number | null = null;
  rutaRetorno = '/calendario';
  private urlOrigen: string | null = null;
  private retornoExplicito = false;

  codigo = '';
  tipoUnidad: TipoUnidad | '' = '';
  codigosDisponibles: string[] = [];
  cargandoCodigos = false;
  codigosError = '';

  idOcupante: number | null = null;
  numeroPersonas: number | null = null;
  entradaEstimada = '';
  salidaEstimada = '';
  canalReserva: CanalReserva = 'MOSTRADOR';
  notas = '';

  conPago = false;
  mostrarModalPago = false;
  pagoModalError = '';
  monto: number | null = null;
  totalCalculado: number | null = null;
  calculandoPago = false;
  calculoError = '';
  medioPago: MedioPago = 'EFECTIVO';
  fechaPago = '';
  notasPago = '';

  mostrarModalCliente = false;
  creandoCliente = false;
  clienteError = '';
  clienteCreado: OcupanteDTO | null = null;
  clienteNuevo: OcupanteNuevoRequest = {
    nombres: '',
    apellidos: '',
    tipoDocumento: undefined,
    numeroDocumento: '',
    telefono: '',
    email: '',
    tipoOcupante: 'CLIENTE',
  };
  mostrarModalBusquedaCliente = false;
  buscandoCliente = false;
  clienteBusquedaDocumento = '';
  clienteBusquedaError = '';
  clientesEncontrados: OcupanteDTO[] = [];

  guardando = false;
  error = '';
  exito = '';
  mostrarToast = false;
  toastMensaje = '';
  private toastTimeoutId: ReturnType<typeof setTimeout> | null = null;

  tiposUnidad = [
    { label: 'Apartamento', value: 'APARTAMENTO' as TipoUnidad },
    { label: 'Apartaestudio', value: 'APARTAESTUDIO' as TipoUnidad },
    { label: 'Habitacion', value: 'HABITACION' as TipoUnidad },
  ];

  canalesReserva: { label: string; value: CanalReserva }[] = [
    { label: 'Airbnb', value: 'PLATAFORMA_AIRBINB' },
    { label: 'Booking', value: 'PLATAFORMA_BOOKING' },
    { label: 'Expedia', value: 'PLATAFORMA_EXPEDIA' },
    { label: 'Whatsapp', value: 'WHATSAPP' },
    { label: 'Telefono', value: 'TELEFONO' },
    { label: 'Email', value: 'EMAIL' },
    { label: 'Mostrador', value: 'MOSTRADOR' },
  ];

  mediosPago: MedioPago[] = [
    'EFECTIVO',
    'TARJETA_CREDITO',
    'TARJETA_DEBITO',
    'TRANSFERENCIA_BANCARIA',
    'PLATAFORMA',
  ];

  tiposDocumento: TipoDocumento[] = ['CC', 'TI', 'CE', 'PA', 'NIT', 'RC'];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly location: Location,
    private readonly reservaService: ReservaService,
    private readonly ocupanteService: OcupanteService,
    private readonly habitacionService: HabitacionService,
    private readonly unidadService: UnidadService,
    private readonly pagoService: PagoService
  ) {}

  ngOnInit(): void {
    const state = (history.state ?? {}) as ReservaEditState;

    if (state.returnTo) {
      this.rutaRetorno = state.returnTo;
      this.retornoExplicito = true;
    }

    this.resolverRutaRetornoDesdeOrigen();

    if (state.editMode && state.reserva?.id) {
      this.cargarModoEdicion(state);
      return;
    }

    const returnToDesdeQuery = this.route.snapshot.queryParamMap.get('returnTo');
    if (returnToDesdeQuery) {
      this.rutaRetorno = returnToDesdeQuery;
      this.retornoExplicito = true;
    }

    const codigoState = (history.state?.codigo as string | undefined) ?? '';
    const tipoState = (history.state?.tipo as TipoUnidad | undefined) ?? '';
    const entradaState = (history.state?.entrada as string | undefined) ?? '';

    this.codigo = codigoState || this.route.snapshot.queryParamMap.get('codigo') || '';
    this.tipoUnidad =
      (tipoState || (this.route.snapshot.queryParamMap.get('tipo') as TipoUnidad | null) || '') as
      | TipoUnidad
      | '';
    if (entradaState) {
      this.entradaEstimada = this.formatearFechaFlatpickr(entradaState);
    }
    if (this.tipoUnidad) {
      this.cargarCodigosDisponibles(this.tipoUnidad);
    }
  }

  onTipoUnidadChange(tipo: TipoUnidad | ''): void {
    if (this.esEdicion) {
      return;
    }

    this.tipoUnidad = tipo;
    this.codigo = '';
    this.codigosDisponibles = [];
    this.codigosError = '';
    if (tipo) {
      this.cargarCodigosDisponibles(tipo);
    }
    this.recalcularPago();
  }

  guardar(): void {
    if (
      !this.tipoUnidad ||
      !this.codigo.trim() ||
      this.idOcupante === null ||
      Number.isNaN(Number(this.idOcupante)) ||
      this.numeroPersonas === null ||
      Number.isNaN(Number(this.numeroPersonas)) ||
      !this.entradaEstimada ||
      !this.salidaEstimada ||
      !this.canalReserva
    ) {
      this.error = 'Completa los campos obligatorios.';
      return;
    }

    if (
      this.conPago &&
      (this.monto === null || Number.isNaN(Number(this.monto)) || this.monto <= 0 || !this.fechaPago)
    ) {
      this.error = 'Completa los campos de pago obligatorios.';
      return;
    }

    if (this.esEdicion && this.conPago) {
      this.error = 'El pago de la reserva debe registrarse por separado despues de guardar los cambios.';
      return;
    }

    this.guardando = true;
    this.error = '';
    this.exito = '';
    const pago = this.conPago ? this.buildPago() : null;

    const request: ReservaNuevoRequest = {
      tipoUnidad: this.tipoUnidad,
      codigo: this.codigo.trim(),
      idOcupante: this.idOcupante,
      numeroPersonas: this.numeroPersonas,
      entradaEstimada: this.normalizarFecha(this.entradaEstimada),
      salidaEstimada: this.normalizarFecha(this.salidaEstimada),
      canalReserva: this.canalReserva,
      notas: this.notas.trim() || undefined,
    };

    const request$: Observable<unknown> =
      this.esEdicion && this.reservaEditandoId !== null
        ? this.reservaService.editarReserva(this.reservaEditandoId, request)
        : this.reservaService.crearReserva(request).pipe(
            concatMap((reserva) => {
              if (!pago) {
                return of(reserva);
              }

              if (!reserva.idEstancia) {
                throw new Error('La reserva fue creada, pero no devolvio la estancia asociada para registrar el pago.');
              }

              return this.pagoService.crearPago(reserva.idEstancia, pago).pipe(map(() => reserva));
            })
          );

    request$.subscribe({
      next: () => {
        this.guardando = false;
        this.exito = this.mensajeExito;
        this.mostrarToastExito(this.exito, true);
      },
      error: (errorResponse: unknown) => {
        this.guardando = false;
        this.error = extractBackendErrorMessage(
          errorResponse,
          this.esEdicion
            ? 'No fue posible actualizar la reserva.'
            : 'No fue posible registrar la reserva.'
        );
      },
    });
  }

  recalcularPago(): void {
    if (!this.conPago && !this.mostrarModalPago) {
      this.totalCalculado = null;
      this.calculoError = '';
      return;
    }

    this.calculoError = '';
    this.totalCalculado = null;

    if (!this.tipoUnidad || !this.entradaEstimada || !this.salidaEstimada) {
      return;
    }

    if (!this.numeroPersonas || this.numeroPersonas < 1) {
      this.calculoError = 'El numero de personas debe ser al menos 1.';
      return;
    }

    this.calculandoPago = true;

    this.pagoService
      .calcularTotal({
        tipoUnidad: this.tipoUnidad,
        numeroPersonas: this.numeroPersonas,
        fechaEntrada: this.normalizarFechaInicio(this.entradaEstimada),
        fechaSalida: this.normalizarFechaInicio(this.salidaEstimada),
      })
      .subscribe({
        next: (total) => {
          this.totalCalculado = total;
          this.monto = total;
          this.calculandoPago = false;
        },
        error: (errorResponse: unknown) => {
          this.calculoError = extractBackendErrorMessage(
            errorResponse,
            'No fue posible calcular el pago.'
          );
          this.calculandoPago = false;
        },
      });
  }

  abrirModalPago(): void {
    if (!this.fechaPago) {
      this.fechaPago = this.obtenerFechaActual();
    }

    this.mostrarModalPago = true;
    this.pagoModalError = '';
    this.recalcularPago();
  }

  cerrarModalPago(): void {
    this.mostrarModalPago = false;
    this.pagoModalError = '';
  }

  guardarPagoModal(): void {
    if (
      this.monto === null ||
      Number.isNaN(Number(this.monto)) ||
      this.monto <= 0 ||
      !this.fechaPago
    ) {
      this.pagoModalError = 'Completa los campos de pago obligatorios.';
      return;
    }

    this.conPago = true;
    this.mostrarModalPago = false;
    this.pagoModalError = '';
  }

  quitarPago(): void {
    this.conPago = false;
    this.mostrarModalPago = false;
    this.pagoModalError = '';
    this.totalCalculado = null;
    this.calculoError = '';
  }

  mantenerModal(event: MouseEvent): void {
    event.stopPropagation();
  }

  abrirModalCliente(): void {
    this.mostrarModalCliente = true;
    this.clienteError = '';
  }

  cerrarModalCliente(): void {
    if (this.creandoCliente) {
      return;
    }
    this.mostrarModalCliente = false;
  }

  abrirModalBusquedaCliente(): void {
    this.mostrarModalBusquedaCliente = true;
    this.clienteBusquedaError = '';
    this.clientesEncontrados = [];
  }

  cerrarModalBusquedaCliente(): void {
    if (this.buscandoCliente) {
      return;
    }
    this.mostrarModalBusquedaCliente = false;
  }

  crearCliente(): void {
    const nombres = this.clienteNuevo.nombres.trim();
    const apellidos = this.clienteNuevo.apellidos.trim();

    if (!nombres || !apellidos) {
      this.clienteError = 'Completa los campos obligatorios.';
      return;
    }

    const request: OcupanteNuevoRequest = {
      nombres,
      apellidos,
      tipoDocumento: this.clienteNuevo.tipoDocumento,
      numeroDocumento: this.normalizarOpcional(this.clienteNuevo.numeroDocumento),
      telefono: this.normalizarOpcional(this.clienteNuevo.telefono),
      email: this.normalizarOpcional(this.clienteNuevo.email),
      tipoOcupante: 'CLIENTE',
    };

    this.creandoCliente = true;
    this.clienteError = '';

    this.ocupanteService.crearOcupante(request).subscribe({
      next: (cliente) => {
        this.creandoCliente = false;
        this.idOcupante = cliente.id;
        this.clienteCreado = cliente;
        this.mostrarModalCliente = false;
        this.limpiarClienteNuevo();
        this.mostrarToastExito(this.mensajeExito);
      },
      error: (errorResponse: unknown) => {
        this.creandoCliente = false;
        this.clienteError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible crear el cliente.'
        );
      },
    });
  }

  buscarCliente(): void {
    const documento = this.clienteBusquedaDocumento.trim();
    if (!documento) {
      this.clienteBusquedaError = 'Ingresa el documento para buscar.';
      return;
    }

    this.buscandoCliente = true;
    this.clienteBusquedaError = '';
    this.clientesEncontrados = [];

    this.ocupanteService.buscarPorDocumento(documento).subscribe({
      next: (ocupantes) => {
        this.buscandoCliente = false;
        this.clientesEncontrados = this.priorizarClienteEnDuplicados(ocupantes);
        if (!this.clientesEncontrados.length) {
          this.clienteBusquedaError = 'No se encontraron clientes.';
        }
      },
      error: (errorResponse: unknown) => {
        this.buscandoCliente = false;
        this.clienteBusquedaError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible buscar los clientes.'
        );
      },
    });
  }

  seleccionarCliente(cliente: OcupanteDTO): void {
    this.idOcupante = cliente.id;
    this.clienteCreado = cliente;
    this.mostrarModalBusquedaCliente = false;
  }

  volver(): void {
    if (this.retornoExplicito) {
      this.router.navigateByUrl(this.rutaRetorno);
      return;
    }

    if (this.urlOrigen) {
      this.location.back();
      return;
    }

    this.router.navigateByUrl(this.rutaRetorno);
  }

  get textoBotonPago(): string {
    return this.conPago ? 'Editar pago' : 'Registrar pago';
  }

  get anticipoReservaResumen(): number {
    return this.conPago && this.monto && this.monto > 0 ? this.monto : 0;
  }

  get totalPagoResumen(): number {
    return this.anticipoReservaResumen;
  }

  get totalDiasCalculoPago(): number {
    const entrada = this.parsearFechaLocal(this.entradaEstimada);
    const salida = this.parsearFechaLocal(this.salidaEstimada);
    if (!entrada || !salida) {
      return 0;
    }

    const diferencia = salida.getTime() - entrada.getTime();
    if (diferencia <= 0) {
      return 0;
    }

    return Math.ceil(diferencia / (1000 * 60 * 60 * 24));
  }

  get textoDiasCalculoPago(): string {
    const dias = this.totalDiasCalculoPago;
    return `${dias} ${dias === 1 ? 'dia' : 'dias'}`;
  }

  get textoPersonasCalculoPago(): string {
    const personas = this.numeroPersonas ?? 0;
    return `${personas} ${personas === 1 ? 'persona' : 'personas'}`;
  }

  private cargarCodigosDisponibles(tipo: TipoUnidad): void {
    this.cargandoCodigos = true;
    this.codigosError = '';

    const request$: Observable<string[]> =
      tipo === 'HABITACION'
        ? this.habitacionService.buscarHabitaciones().pipe(map((items) => items.map((i) => i.codigo)))
        : this.unidadService
            .buscarUnidades({ tipo })
            .pipe(map((items) => items.map((i) => i.codigo)));

    request$.subscribe({
      next: (codigos) => {
        const codigosLimpios = codigos.filter((codigo) => Boolean(codigo));
        this.codigosDisponibles = Array.from(new Set(codigosLimpios)).sort((a, b) =>
          a.localeCompare(b)
        );
        this.cargandoCodigos = false;
        if (!this.codigosDisponibles.length) {
          this.codigosError = 'No se encontraron codigos disponibles.';
        }
      },
      error: (errorResponse: unknown) => {
        this.cargandoCodigos = false;
        this.codigosError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar los codigos.'
        );
      },
    });
  }

  private buildPago(): PagoNuevoRequest {
    const monto = Number(this.monto ?? 0);

    return {
      tipoPago: 'ANTICIPO_RESERVA',
      monto,
      montoEstimado: this.totalCalculado ?? undefined,
      medioPago: this.medioPago,
      fecha: this.normalizarFechaPago(this.fechaPago),
      notas: this.notasPago.trim() || undefined,
      estado: 'COMPLETADO',
    };
  }

  private normalizarFecha(valor: string): string {
    return valor.replace(' ', 'T').slice(0, 10);
  }

  private normalizarFechaInicio(valor: string): string {
    return `${this.normalizarFecha(valor)}T00:00`;
  }

  private normalizarFechaHora(valor: string): string {
    return valor.replace(' ', 'T');
  }

  private normalizarFechaPago(valor: string): string {
    if (valor.includes(' ')) {
      return this.normalizarFechaHora(valor);
    }

    if (valor.includes('T')) {
      return valor;
    }

    const ahora = new Date();
    const horas = `${ahora.getHours()}`.padStart(2, '0');
    const minutos = `${ahora.getMinutes()}`.padStart(2, '0');
    return `${valor}T${horas}:${minutos}`;
  }

  private parsearFechaLocal(valor: string): Date | null {
    if (!valor) {
      return null;
    }

    const fecha = new Date(this.normalizarFechaInicio(valor));
    return Number.isNaN(fecha.getTime()) ? null : fecha;
  }

  private mostrarToastExito(mensaje: string, navegar = false): void {
    if (this.toastTimeoutId) {
      clearTimeout(this.toastTimeoutId);
      this.toastTimeoutId = null;
    }

    this.toastMensaje = mensaje;
    this.mostrarToast = true;
    this.toastTimeoutId = setTimeout(() => {
      this.mostrarToast = false;
      this.toastTimeoutId = null;
      if (navegar) {
        this.router.navigateByUrl(this.rutaRetorno);
      }
    }, 1500);
  }

  private cargarModoEdicion(state: ReservaEditState): void {
    const reserva = state.reserva;
    if (!reserva?.id || !reserva.codigo || !reserva.tipoUnidad) {
      return;
    }

    this.esEdicion = true;
    this.reservaEditandoId = reserva.id;
    this.rutaRetorno = state.returnTo || this.rutaRetorno || '/reservas';
    this.codigo = reserva.codigo;
    this.tipoUnidad = reserva.tipoUnidad;
    this.idOcupante = reserva.idOcupante ?? null;
    this.numeroPersonas = reserva.numeroPersonas ?? null;
    this.canalReserva = reserva.canalReserva ?? 'MOSTRADOR';
    this.entradaEstimada = this.formatearFechaFlatpickr(reserva.entradaEstimada);
    this.salidaEstimada = this.formatearFechaFlatpickr(reserva.salidaEstimada);

    if (reserva.nombreCliente) {
      const [nombres, ...resto] = reserva.nombreCliente.trim().split(' ');
      this.clienteCreado = {
        id: reserva.idOcupante ?? 0,
        nombres: nombres || reserva.nombreCliente,
        apellidos: resto.join(' '),
        tipoOcupante: 'CLIENTE',
      };
    }
  }

  private formatearFechaFlatpickr(fecha?: string): string {
    if (!fecha) {
      return '';
    }

    return fecha.replace('T', ' ').slice(0, 10);
  }

  private normalizarOpcional(valor?: string): string | undefined {
    const limpio = (valor ?? '').trim();
    return limpio ? limpio : undefined;
  }

  private obtenerFechaActual(): string {
    return getCurrentDateInput();
  }

  private limpiarClienteNuevo(): void {
    this.clienteNuevo = {
      nombres: '',
      apellidos: '',
      tipoDocumento: undefined,
      numeroDocumento: '',
      telefono: '',
      email: '',
      tipoOcupante: 'CLIENTE',
    };
  }

  private priorizarClienteEnDuplicados(ocupantes: OcupanteDTO[]): OcupanteDTO[] {
    return this.priorizarTipoEnDuplicados(ocupantes, 'CLIENTE');
  }

  private priorizarTipoEnDuplicados(
    ocupantes: OcupanteDTO[],
    tipo: TipoOcupante
  ): OcupanteDTO[] {
    const mapa = new Map<string, OcupanteDTO>();

    ocupantes.forEach((ocupante) => {
      const key = [
        ocupante.nombres?.toLowerCase().trim() ?? '',
        ocupante.apellidos?.toLowerCase().trim() ?? '',
        ocupante.tipoDocumento ?? '',
        ocupante.numeroDocumento?.toLowerCase().trim() ?? '',
      ].join('|');

      const actual = mapa.get(key);
      if (!actual || (actual.tipoOcupante !== tipo && ocupante.tipoOcupante === tipo)) {
        mapa.set(key, ocupante);
      }
    });

    return Array.from(mapa.values());
  }

  private resolverRutaRetornoDesdeOrigen(): void {
    if (this.retornoExplicito) {
      return;
    }

    const urlAnterior = getPreviousNavigationUrl(this.router, { excludePrefix: '/reservas/nueva' });

    if (!urlAnterior) {
      return;
    }

    this.urlOrigen = urlAnterior;
    this.rutaRetorno = urlAnterior;
  }
}
