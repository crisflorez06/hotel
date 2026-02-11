import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FlatpickrModule } from 'angularx-flatpickr';
import { Observable, map } from 'rxjs';

import { ReservaService } from '../../services/reserva.service';
import { OcupanteService } from '../../services/ocupante.service';
import { HabitacionService } from '../../services/habitacion.service';
import { UnidadService } from '../../services/unidad.service';
import { PagoService } from '../../services/pago.service';
import {
  CanalReserva,
  EstadoPago,
  MedioPago,
  TipoDocumento,
  TipoOcupante,
  TipoUnidad,
} from '../../models/enums';
import { ReservaNuevoRequest } from '../../models/reserva.model';
import { OcupanteDTO, OcupanteNuevoRequest } from '../../models/ocupante.model';
import { PagoNuevoRequest } from '../../models/pago.model';

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
  esEdicion = false;
  reservaEditandoId: number | null = null;
  rutaRetorno = '/calendario';

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
  monto: number | null = null;
  totalCalculado: number | null = null;
  calculandoPago = false;
  calculoError = '';
  medioPago: MedioPago = 'EFECTIVO';
  fechaPago = '';
  estadoPago: EstadoPago = 'PENDIENTE';

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

  tiposUnidad = [
    { label: 'Unidad', value: 'APARTAMENTO' as TipoUnidad },
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

  estadosPago: EstadoPago[] = ['PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO'];
  tiposDocumento: TipoDocumento[] = ['CC', 'TI', 'CE', 'PA', 'NIT', 'RC'];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly reservaService: ReservaService,
    private readonly ocupanteService: OcupanteService,
    private readonly habitacionService: HabitacionService,
    private readonly unidadService: UnidadService,
    private readonly pagoService: PagoService
  ) {}

  ngOnInit(): void {
    const state = (history.state ?? {}) as ReservaEditState;
    if (state.editMode && state.reserva?.id) {
      this.cargarModoEdicion(state);
      return;
    }

    this.codigo = this.route.snapshot.queryParamMap.get('codigo') ?? '';
    this.tipoUnidad = (this.route.snapshot.queryParamMap.get('tipo') as TipoUnidad) ?? '';
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

    this.guardando = true;
    this.error = '';
    this.exito = '';

    const request: ReservaNuevoRequest = {
      tipoUnidad: this.tipoUnidad,
      codigo: this.codigo.trim(),
      idOcupante: this.idOcupante,
      numeroPersonas: this.numeroPersonas,
      entradaEstimada: this.normalizarFechaHora(this.entradaEstimada),
      salidaEstimada: this.normalizarFechaHora(this.salidaEstimada),
      canalReserva: this.canalReserva,
      notas: this.notas.trim() || undefined,
      pago: this.conPago ? this.buildPago() : null,
    };

    const request$ =
      this.esEdicion && this.reservaEditandoId !== null
        ? this.reservaService.editarReserva(this.reservaEditandoId, request)
        : this.reservaService.crearReserva(request);

    request$.subscribe({
      next: () => {
        this.guardando = false;
        this.exito = this.esEdicion ? 'Reserva actualizada con exito.' : 'Reserva registrada con exito.';
        this.mostrarToastExito(this.exito, true);
      },
      error: () => {
        this.guardando = false;
        this.error = this.esEdicion
          ? 'No fue posible actualizar la reserva.'
          : 'No fue posible registrar la reserva.';
      },
    });
  }

  recalcularPago(): void {
    if (!this.conPago) {
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
        fechaEntrada: this.normalizarFechaHora(this.entradaEstimada),
        fechaSalida: this.normalizarFechaHora(this.salidaEstimada),
      })
      .subscribe({
        next: (total) => {
          this.totalCalculado = total;
          this.monto = total;
          this.calculandoPago = false;
        },
        error: () => {
          this.calculoError = 'No fue posible calcular el pago.';
          this.calculandoPago = false;
        },
      });
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
        this.mostrarToastExito(`Cliente creado: ${cliente.nombres} ${cliente.apellidos}.`);
      },
      error: () => {
        this.creandoCliente = false;
        this.clienteError = 'No fue posible crear el cliente.';
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
      error: () => {
        this.buscandoCliente = false;
        this.clienteBusquedaError = 'No fue posible buscar los clientes.';
      },
    });
  }

  seleccionarCliente(cliente: OcupanteDTO): void {
    this.idOcupante = cliente.id;
    this.clienteCreado = cliente;
    this.mostrarModalBusquedaCliente = false;
  }

  volver(): void {
    this.router.navigate([this.rutaRetorno]);
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
      error: () => {
        this.cargandoCodigos = false;
        this.codigosError = 'No fue posible cargar los codigos.';
      },
    });
  }

  private buildPago(): PagoNuevoRequest {
    const monto = Number(this.monto ?? 0);

    return {
      tipoPago: 'RESERVA',
      monto,
      medioPago: this.medioPago,
      fecha: this.normalizarFechaHora(this.fechaPago),
      estado: this.estadoPago,
    };
  }

  private normalizarFechaHora(valor: string): string {
    return valor.replace(' ', 'T');
  }

  private mostrarToastExito(mensaje: string, navegar = false): void {
    this.toastMensaje = mensaje;
    this.mostrarToast = true;
    setTimeout(() => {
      this.mostrarToast = false;
      if (navegar) {
        this.router.navigate([this.rutaRetorno]);
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
    this.rutaRetorno = state.returnTo || '/reservas';
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

    const normalizada = fecha.replace('T', ' ').slice(0, 16);
    return normalizada;
  }

  private normalizarOpcional(valor?: string): string | undefined {
    const limpio = (valor ?? '').trim();
    return limpio ? limpio : undefined;
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
}
