import { CommonModule, Location } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FlatpickrModule } from 'angularx-flatpickr';
import { Observable, map } from 'rxjs';

import { EstanciaService } from '../../services/estancia.service';
import { OcupanteService } from '../../services/ocupante.service';
import { HabitacionService } from '../../services/habitacion.service';
import { MedioPago, TipoDocumento, TipoOcupante, TipoPago, TipoUnidad } from '../../models/enums';
import { EstanciaActivarRequest, EstanciaEditarRequest, EstanciaNuevoRequest } from '../../models/estancia.model';
import { EstanciaDTO } from '../../models/estancia-detalle.model';
import { OcupanteDTO, OcupanteNuevoRequest } from '../../models/ocupante.model';
import { PagoNuevoRequest } from '../../models/pago.model';
import { PagoService } from '../../services/pago.service';
import { UnidadService } from '../../services/unidad.service';
import { formatDateOnly, getCurrentDateInput } from '../../core/utils/date-time.util';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import { FeedbackToastService } from '../../core/services/feedback-toast.service';
import { getPreviousNavigationUrl } from '../../core/utils/navigation-return.util';

@Component({
  selector: 'app-estancia-nueva',
  standalone: true,
  imports: [CommonModule, FormsModule, FlatpickrModule],
  templateUrl: './estancia-nueva.component.html',
  styleUrl: './estancia-nueva.component.css',
})
export class EstanciaNuevaComponent implements OnInit {
  private readonly mensajeExito = 'Operacion realizada con exito.';

  codigo = '';
  tipoUnidad: TipoUnidad | '' = '';
  codigosDisponibles: string[] = [];
  cargandoCodigos = false;
  codigosError = '';
  estanciaId: number | null = null;
  esEdicion = false;
  esRegistroIngreso = false;
  idReserva: number | null = null;
  idEstanciaReservada: number | null = null;
  codigoReserva = '';
  nombreClienteReserva = '';
  entradaReserva = '';
  salidaReserva = '';
  numeroPersonasReserva: number | null = null;

  idCliente: number | null = null;
  entradaReal = '';
  salidaEstimada = '';
  idAcompanantes = '';
  notas = '';

  conPago = false;
  mostrarModalPago = false;
  pagoModalError = '';
  tipoPago: TipoPago = 'ANTICIPO_ESTANCIA';
  monto: number | null = null;
  totalCalculado: number | null = null;
  totalAnticipoReserva: number | null = null;
  totalAnticipoEstanciaRegistrado = 0;
  calculandoPago = false;
  calculoError = '';
  medioPago: MedioPago = 'EFECTIVO';
  fechaPago = '';

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
  clienteBusquedaCedula = '';
  clienteBusquedaError = '';
  clientesEncontrados: OcupanteDTO[] = [];

  mostrarModalAcompanante = false;
  creandoAcompanante = false;
  acompananteError = '';
  acompanantesCreados: OcupanteDTO[] = [];
  acompananteNuevo: OcupanteNuevoRequest = {
    nombres: '',
    apellidos: '',
    tipoDocumento: undefined,
    numeroDocumento: '',
    telefono: '',
    email: '',
    tipoOcupante: 'ACOMPANANTE',
  };
  mostrarModalBusquedaAcompanante = false;
  buscandoAcompanante = false;
  acompananteBusquedaDocumento = '';
  acompananteBusquedaError = '';
  acompanantesEncontrados: OcupanteDTO[] = [];

  guardando = false;
  error = '';
  exito = '';
  rutaRetorno = '/recepcion';
  private urlOrigen: string | null = null;
  private retornoExplicito = false;

  mediosPago: MedioPago[] = [
    'EFECTIVO',
    'TARJETA_CREDITO',
    'TARJETA_DEBITO',
    'TRANSFERENCIA_BANCARIA',
    'PLATAFORMA',
  ];
  tiposUnidad = [
    { label: 'Apartamento', value: 'APARTAMENTO' as TipoUnidad },
    { label: 'Apartaestudio', value: 'APARTAESTUDIO' as TipoUnidad },
    { label: 'Habitacion', value: 'HABITACION' as TipoUnidad },
  ];
  tiposDocumento: TipoDocumento[] = ['CC', 'TI', 'CE', 'PA', 'NIT', 'RC'];
  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly estanciaService: EstanciaService,
    private readonly ocupanteService: OcupanteService,
    private readonly pagoService: PagoService,
    private readonly habitacionService: HabitacionService,
    private readonly unidadService: UnidadService,
    private readonly location: Location,
    private readonly feedbackToastService: FeedbackToastService
  ) {}

  ngOnInit(): void {
    const state = (this.router.getCurrentNavigation()?.extras.state ??
      history.state) as Partial<Record<string, unknown>> | null;
    const queryParams = this.route.snapshot.queryParamMap;

    const returnToState = state?.['returnTo'];
    if (typeof returnToState === 'string' && returnToState.trim()) {
      this.rutaRetorno = returnToState;
      this.retornoExplicito = true;
    }

    const returnToQuery = queryParams.get('returnTo');
    if (returnToQuery) {
      this.rutaRetorno = returnToQuery;
      this.retornoExplicito = true;
    }

    this.resolverRutaRetornoDesdeOrigen();

    const codigo =
      (state?.['codigo'] as string | undefined) ?? queryParams.get('codigo') ?? '';
    const tipo =
      (state?.['tipo'] as TipoUnidad | undefined) ??
      (queryParams.get('tipo') as TipoUnidad | null) ??
      '';
    const idClienteParam =
      (state?.['idCliente'] as number | string | undefined) ?? queryParams.get('idCliente') ?? undefined;
    const nombreClienteParam =
      (state?.['nombreCliente'] as string | undefined) ?? queryParams.get('nombreCliente') ?? undefined;
    const codigoReservaParam =
      (state?.['codigoReserva'] as string | undefined) ?? queryParams.get('codigoReserva') ?? undefined;
    const idReservaParam =
      (state?.['idReserva'] as number | string | undefined) ?? queryParams.get('idReserva') ?? undefined;
    const flujoParam =
      (state?.['flujo'] as string | undefined) ?? queryParams.get('flujo') ?? undefined;
    const idEstanciaParam =
      (state?.['idEstancia'] as number | string | undefined) ??
      queryParams.get('idEstancia') ??
      undefined;
    const editarParam =
      (state?.['editar'] as string | undefined) ?? queryParams.get('editar') ?? undefined;
    const estanciaIdParam =
      (state?.['estanciaId'] as number | string | undefined) ??
      queryParams.get('estanciaId') ??
      undefined;
    const entradaParam =
      (state?.['entrada'] as string | undefined) ?? queryParams.get('entrada') ?? undefined;
    const salidaParam =
      (state?.['salida'] as string | undefined) ?? queryParams.get('salida') ?? undefined;
    const totalAnticipoParam =
      (state?.['totalAnticipo'] as number | string | undefined) ??
      queryParams.get('totalAnticipo') ??
      undefined;
    const numeroPersonasReservaParam =
      (state?.['numeroPersonasReserva'] as number | string | undefined) ??
      queryParams.get('numeroPersonasReserva') ??
      undefined;

    this.codigo = codigo;
    this.tipoUnidad = tipo;
    if (this.tipoUnidad) {
      this.cargarCodigosDisponibles(this.tipoUnidad);
    }

    if (idClienteParam !== undefined && idClienteParam !== null && idClienteParam !== '') {
      const parsed =
        typeof idClienteParam === 'number' ? idClienteParam : Number.parseInt(idClienteParam, 10);
      this.idCliente = Number.isNaN(parsed) || parsed <= 0 ? null : parsed;
    }
    if (this.idCliente && nombreClienteParam) {
      const nombreCompleto = nombreClienteParam.trim();
      if (nombreCompleto) {
        const partes = nombreCompleto.split(' ').filter((parte) => parte.trim());
        const apellidos = partes.length > 1 ? partes.at(-1) ?? '' : '';
        const nombres = partes.length > 1 ? partes.slice(0, -1).join(' ') : nombreCompleto;
        this.clienteCreado = {
          id: this.idCliente,
          nombres,
          apellidos,
          tipoOcupante: 'CLIENTE',
        };
      }
    }

    if (idReservaParam !== undefined && idReservaParam !== null && idReservaParam !== '') {
      const parsed =
        typeof idReservaParam === 'number' ? idReservaParam : Number.parseInt(idReservaParam, 10);
      this.idReserva = Number.isNaN(parsed) ? null : parsed;
    } else {
      this.idReserva = null;
    }

    this.esRegistroIngreso = flujoParam === 'INGRESO' || this.idReserva !== null;
    this.codigoReserva = (codigoReservaParam ?? '').trim();
    this.nombreClienteReserva = (nombreClienteParam ?? '').trim();
    if (
      numeroPersonasReservaParam !== undefined &&
      numeroPersonasReservaParam !== null &&
      numeroPersonasReservaParam !== ''
    ) {
      const parsed =
        typeof numeroPersonasReservaParam === 'number'
          ? numeroPersonasReservaParam
          : Number.parseInt(numeroPersonasReservaParam, 10);
      this.numeroPersonasReserva = Number.isNaN(parsed) ? null : parsed;
    }

    if (idEstanciaParam !== undefined && idEstanciaParam !== null && idEstanciaParam !== '') {
      const parsed =
        typeof idEstanciaParam === 'number'
          ? idEstanciaParam
          : Number.parseInt(idEstanciaParam, 10);
      this.idEstanciaReservada = Number.isNaN(parsed) ? null : parsed;
    } else {
      this.idEstanciaReservada = null;
    }

    if (
      totalAnticipoParam !== undefined &&
      totalAnticipoParam !== null &&
      totalAnticipoParam !== ''
    ) {
      const parsed =
        typeof totalAnticipoParam === 'number'
          ? totalAnticipoParam
          : Number.parseFloat(totalAnticipoParam);
      this.totalAnticipoReserva = Number.isNaN(parsed) ? null : parsed;
    }

    this.esEdicion = editarParam === 'true' || editarParam === '1' || !!estanciaIdParam;
    this.estanciaId = estanciaIdParam
      ? typeof estanciaIdParam === 'number'
        ? estanciaIdParam
        : Number.parseInt(estanciaIdParam, 10)
      : null;

    if (this.esEdicion) {
      if (!this.estanciaId) {
        this.error = 'No se encontro el id de la estancia para editar.';
        return;
      }
      this.cargarEstanciaParaEdicion(this.estanciaId);
      return;
    }

    if (entradaParam) {
      this.entradaReal = this.formatearFechaHora(entradaParam);
      this.entradaReserva = this.formatearFechaHora(entradaParam);
    }

    if (salidaParam) {
      this.salidaEstimada = this.formatearFechaHora(salidaParam);
      this.salidaReserva = this.formatearFechaHora(salidaParam);
    }

  }

  guardar(): void {
    if (
      !this.tipoUnidad ||
      !this.codigo ||
      this.idCliente === null ||
      Number.isNaN(Number(this.idCliente)) ||
      !this.entradaReal ||
      !this.salidaEstimada
    ) {
      this.error = 'Completa los campos obligatorios.';
      return;
    }

    this.guardando = true;
    this.error = '';
    this.exito = '';

    if (this.esRegistroIngreso && !this.idReserva) {
      this.guardando = false;
      this.error = 'No se encontro la reserva asociada para registrar el ingreso.';
      return;
    }

    if (this.esEdicion) {
      if (!this.estanciaId) {
        this.guardando = false;
        this.error = 'No se encontro la estancia a editar.';
        return;
      }

      const request: EstanciaEditarRequest = {
        tipoUnidad: this.tipoUnidad as TipoUnidad,
        codigo: this.codigo,
        idCliente: this.idCliente,
        entradaReal: this.normalizarFechaHora(this.entradaReal),
        salidaEstimada: this.normalizarFechaHora(this.salidaEstimada),
        idAcompanantes: this.parseAcompanantes(),
        notas: this.notas || undefined,
      };

      this.estanciaService
        .editarEstancia(this.estanciaId, request)
        .pipe(map(() => this.estanciaId as number))
        .subscribe({
          next: () => {
            this.registrarPagoYFinalizar(this.estanciaId as number, this.mensajeExito);
          },
          error: (errorResponse: unknown) => {
            this.guardando = false;
            this.error = extractBackendErrorMessage(
              errorResponse,
              'No fue posible actualizar la estancia.'
            );
          },
        });
      return;
    }

    if (this.esRegistroIngreso) {
      const request: EstanciaActivarRequest = {
        idReserva: this.idReserva as number,
        idCliente: this.idCliente,
        entradaReal: this.normalizarFechaHora(this.entradaReal),
        salidaEstimada: this.normalizarFechaHora(this.salidaEstimada),
        idAcompanantes: this.parseAcompanantes(),
        notas: this.notas || undefined,
      };

      this.estanciaService.activarEstancia(request).subscribe({
        next: (estancia) => {
          this.registrarPagoYFinalizar(estancia.id, this.mensajeExito);
        },
        error: (errorResponse: unknown) => {
          this.guardando = false;
          this.error = extractBackendErrorMessage(
            errorResponse,
            'No fue posible activar la estancia.'
          );
        },
      });
      return;
    }

    const request: EstanciaNuevoRequest = {
      tipoUnidad: this.tipoUnidad,
      codigo: this.codigo,
      idCliente: this.idCliente,
      entradaReal: this.normalizarFechaHora(this.entradaReal),
      salidaEstimada: this.normalizarFechaHora(this.salidaEstimada),
      idAcompanantes: this.parseAcompanantes(),
      notas: this.notas || undefined,
    };

    this.estanciaService.crearEstancia(request).subscribe({
      next: (estancia) => {
        this.registrarPagoYFinalizar(estancia.id, this.mensajeExito);
      },
      error: (errorResponse: unknown) => {
        this.guardando = false;
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible registrar la estancia.'
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

    if (!this.tipoUnidad || !this.entradaReal || !this.salidaEstimada) {
      return;
    }

    const numeroPersonas = this.obtenerNumeroPersonas();
    if (numeroPersonas < 1) {
      this.calculoError = 'El numero de personas debe ser al menos 1.';
      return;
    }

    this.calculandoPago = true;

    this.pagoService
      .calcularTotal({
        tipoUnidad: this.tipoUnidad,
        numeroPersonas,
        fechaEntrada: this.normalizarFechaHora(this.entradaReal),
        fechaSalida: this.normalizarFechaHora(this.salidaEstimada),
        ...((this.estanciaId ?? this.idEstanciaReservada) !== null
          ? { idEstancia: this.estanciaId ?? this.idEstanciaReservada ?? undefined }
          : {}),
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
    if (this.esEdicion) {
      return;
    }

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

  mantenerModal(event: MouseEvent): void {
    event.stopPropagation();
  }

  guardarPagoModal(): void {
    if (!this.buildPago()) {
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
        this.idCliente = cliente.id;
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
    const cedula = this.clienteBusquedaCedula.trim();
    if (!cedula) {
      this.clienteBusquedaError = 'Ingresa la cedula para buscar.';
      return;
    }

    this.buscandoCliente = true;
    this.clienteBusquedaError = '';
    this.clientesEncontrados = [];

    this.ocupanteService.buscarPorDocumento(cedula).subscribe({
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
    this.idCliente = cliente.id;
    this.clienteCreado = cliente;
    this.mostrarModalBusquedaCliente = false;
  }

  abrirModalAcompanante(): void {
    this.mostrarModalAcompanante = true;
    this.acompananteError = '';
  }

  cerrarModalAcompanante(): void {
    if (this.creandoAcompanante) {
      return;
    }
    this.mostrarModalAcompanante = false;
  }

  abrirModalBusquedaAcompanante(): void {
    this.mostrarModalBusquedaAcompanante = true;
    this.acompananteBusquedaError = '';
    this.acompanantesEncontrados = [];
  }

  cerrarModalBusquedaAcompanante(): void {
    if (this.buscandoAcompanante) {
      return;
    }
    this.mostrarModalBusquedaAcompanante = false;
  }

  crearAcompanante(): void {
    const nombres = this.acompananteNuevo.nombres.trim();
    const apellidos = this.acompananteNuevo.apellidos.trim();

    if (!nombres || !apellidos) {
      this.acompananteError = 'Completa los campos obligatorios.';
      return;
    }

    const request: OcupanteNuevoRequest = {
      nombres,
      apellidos,
      tipoDocumento: this.acompananteNuevo.tipoDocumento,
      numeroDocumento: this.normalizarOpcional(this.acompananteNuevo.numeroDocumento),
      telefono: this.normalizarOpcional(this.acompananteNuevo.telefono),
      email: this.normalizarOpcional(this.acompananteNuevo.email),
      tipoOcupante: 'ACOMPANANTE',
    };

    this.creandoAcompanante = true;
    this.acompananteError = '';

    this.ocupanteService.crearOcupante(request).subscribe({
      next: (acompanante) => {
        this.creandoAcompanante = false;
        if (!this.acompanantesCreados.some((item) => item.id === acompanante.id)) {
          this.acompanantesCreados = [...this.acompanantesCreados, acompanante];
          this.recalcularPago();
        }
        this.mostrarModalAcompanante = false;
        this.limpiarAcompananteNuevo();
        this.mostrarToastExito(this.mensajeExito);
      },
      error: (errorResponse: unknown) => {
        this.creandoAcompanante = false;
        this.acompananteError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible crear el acompanante.'
        );
      },
    });
  }

  quitarAcompanante(id: number): void {
    this.acompanantesCreados = this.acompanantesCreados.filter((item) => item.id !== id);
    this.recalcularPago();
  }

  buscarAcompanante(): void {
    const documento = this.acompananteBusquedaDocumento.trim();
    if (!documento) {
      this.acompananteBusquedaError = 'Ingresa el documento para buscar.';
      return;
    }

    this.buscandoAcompanante = true;
    this.acompananteBusquedaError = '';
    this.acompanantesEncontrados = [];

    this.ocupanteService.buscarPorDocumento(documento).subscribe({
      next: (ocupantes) => {
        this.buscandoAcompanante = false;
        this.acompanantesEncontrados = this.priorizarTipoEnDuplicados(
          ocupantes,
          'ACOMPANANTE'
        );
        if (!this.acompanantesEncontrados.length) {
          this.acompananteBusquedaError = 'No se encontraron acompanantes.';
        }
      },
      error: (errorResponse: unknown) => {
        this.buscandoAcompanante = false;
        this.acompananteBusquedaError = extractBackendErrorMessage(
          errorResponse,
          'No fue posible buscar los acompanantes.'
        );
      },
    });
  }

  seleccionarAcompanante(acompanante: OcupanteDTO): void {
    if (!this.acompanantesCreados.some((item) => item.id === acompanante.id)) {
      this.acompanantesCreados = [...this.acompanantesCreados, acompanante];
      this.recalcularPago();
    }
  }

  acompananteSeleccionado(id: number): boolean {
    return this.acompanantesCreados.some((item) => item.id === id);
  }

  onTipoUnidadChange(tipo: TipoUnidad | ''): void {
    this.tipoUnidad = tipo;
    this.codigo = '';
    this.codigosDisponibles = [];
    this.codigosError = '';

    if (!tipo) {
      this.recalcularPago();
      return;
    }

    this.cargarCodigosDisponibles(tipo);
    this.recalcularPago();
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

  private parseAcompanantes(): number[] | undefined {
    const idsManual = this.idAcompanantes
      .split(',')
      .map((value) => Number(value.trim()))
      .filter((value) => !Number.isNaN(value) && value > 0);

    const idsCreados = this.acompanantesCreados.map((item) => item.id);
    const ids = Array.from(new Set([...idsCreados, ...idsManual]));
    return ids.length ? ids : undefined;
  }

  obtenerNumeroPersonas(): number {
    const idsManual = this.idAcompanantes
      .split(',')
      .map((value) => Number(value.trim()))
      .filter((value) => !Number.isNaN(value) && value > 0);
    const idsCreados = this.acompanantesCreados.map((item) => item.id);
    const totalAcompanantes = new Set([...idsCreados, ...idsManual]).size;
    return 1 + totalAcompanantes;
  }

  get textoBotonPago(): string {
    return this.conPago ? 'Editar pago' : 'Registrar pago';
  }

  get anticipoEstanciaResumen(): number {
    const anticipoActual = this.conPago && this.monto && this.monto > 0 ? this.monto : 0;
    return this.totalAnticipoEstanciaRegistrado + anticipoActual;
  }

  get totalPagoResumen(): number {
    const anticipoReserva = this.totalAnticipoReserva ?? 0;
    return anticipoReserva + this.anticipoEstanciaResumen;
  }

  get totalDiasCalculoPago(): number {
    const entrada = this.parsearFechaHoraLocal(this.entradaReal);
    const salida = this.parsearFechaHoraLocal(this.salidaEstimada);
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
    const personas = this.obtenerNumeroPersonas();
    return `${personas} ${personas === 1 ? 'persona' : 'personas'}`;
  }

  formatearSoloFecha(valor: string): string {
    return valor ? formatDateOnly(valor.replace(' ', 'T')) : '-';
  }

  private buildPago(): PagoNuevoRequest | undefined {
    if (!this.monto || !this.fechaPago) {
      return undefined;
    }

    return {
      tipoPago: this.tipoPago,
      monto: this.monto,
      medioPago: this.medioPago,
      fecha: this.normalizarFechaPago(this.fechaPago),
      estado: 'COMPLETADO',
    };
  }

  private registrarPagoYFinalizar(idEstancia: number, mensajeExito: string): void {
    if (this.esEdicion) {
      this.guardando = false;
      this.exito = mensajeExito;
      this.mostrarToastExito(mensajeExito, true);
      return;
    }

    const pago = this.buildPago();
    if (!this.conPago || !pago) {
      this.guardando = false;
      this.exito = mensajeExito;
      this.mostrarToastExito(mensajeExito, true);
      return;
    }

    this.pagoService.crearPago(idEstancia, pago).subscribe({
      next: () => {
        this.guardando = false;
        this.exito = mensajeExito;
        this.mostrarToastExito(mensajeExito, true);
      },
      error: (errorResponse: unknown) => {
        this.guardando = false;
        this.error = extractBackendErrorMessage(
          errorResponse,
          'La estancia fue guardada, pero no fue posible registrar el pago.'
        );
      },
    });
  }

  private normalizarFechaHora(valor: string): string {
    return valor.replace(' ', 'T');
  }

  private cargarCodigosDisponibles(tipo: TipoUnidad): void {
    this.cargandoCodigos = true;
    this.codigosError = '';

    const request$: Observable<string[]> =
      tipo === 'HABITACION'
        ? this.habitacionService.buscarHabitaciones().pipe(map((items) => items.map((item) => item.codigo)))
        : this.unidadService.buscarUnidades({ tipo }).pipe(map((items) => items.map((item) => item.codigo)));

    request$.subscribe({
      next: (codigos) => {
        const codigosLimpios = codigos.filter((codigoItem) => Boolean(codigoItem));
        this.codigosDisponibles = Array.from(new Set(codigosLimpios)).sort((a, b) =>
          a.localeCompare(b)
        );
        if (this.codigo && !this.codigosDisponibles.includes(this.codigo)) {
          this.codigosDisponibles = [this.codigo, ...this.codigosDisponibles];
        }
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

  private obtenerFechaActual(): string {
    return getCurrentDateInput();
  }

  private parsearFechaHoraLocal(valor: string): Date | null {
    if (!valor) {
      return null;
    }

    const normalizado = valor.replace(' ', 'T');
    const fecha = new Date(normalizado);
    return Number.isNaN(fecha.getTime()) ? null : fecha;
  }

  private cargarEstanciaParaEdicion(estanciaId: number): void {
    this.estanciaService.obtenerEstanciaPorId(estanciaId).subscribe({
      next: (estancia) => {
        this.estanciaId = estancia.id;
        this.entradaReal = this.formatearFechaHora(estancia.entradaReal);
        this.salidaEstimada = this.formatearFechaHora(estancia.salidaEstimada);
        this.notas = estancia.notas ?? '';
        this.conPago = false;

        const cliente = estancia.ocupantes.find((ocupante) => ocupante.tipoOcupante === 'CLIENTE');
        this.idCliente = cliente?.id ?? null;
        this.clienteCreado = cliente ?? null;

        this.acompanantesCreados = estancia.ocupantes.filter(
          (ocupante) => ocupante.tipoOcupante === 'ACOMPANANTE'
        );
        this.idAcompanantes = '';
        this.totalAnticipoReserva = this.sumarPagosPorTipo(estancia, 'ANTICIPO_RESERVA');
        this.totalAnticipoEstanciaRegistrado = this.sumarPagosPorTipo(estancia, 'ANTICIPO_ESTANCIA');
      },
      error: (errorResponse: unknown) => {
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar la estancia para editar.'
        );
      },
    });
  }

  private formatearFechaHora(valor: string | null | undefined): string {
    if (!valor) {
      return '';
    }
    const normalizado = valor.replace(' ', 'T');
    const [fecha, hora] = normalizado.split('T');
    if (!fecha || !hora) {
      return valor;
    }
    return `${fecha} ${hora.slice(0, 5)}`;
  }

  private sumarPagosPorTipo(estancia: EstanciaDTO, tipoPago: TipoPago): number {
    return (estancia.pagos ?? [])
      .filter((pago) => pago.estado === 'COMPLETADO' && pago.tipoPago === tipoPago)
      .reduce((total, pago) => total + Number(pago.monto ?? 0), 0);
  }

  private mostrarToastExito(mensaje: string, navegar = false): void {
    this.feedbackToastService.showSuccess(mensaje);
    if (navegar) {
      this.router.navigateByUrl(this.rutaRetorno);
    }
  }

  private resolverRutaRetornoDesdeOrigen(): void {
    if (this.retornoExplicito) {
      return;
    }

    const urlAnterior = getPreviousNavigationUrl(this.router, { excludePrefix: '/estancias/nueva' });

    if (!urlAnterior) {
      return;
    }

    this.urlOrigen = urlAnterior;
    this.rutaRetorno = urlAnterior;
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

  private limpiarAcompananteNuevo(): void {
    this.acompananteNuevo = {
      nombres: '',
      apellidos: '',
      tipoDocumento: undefined,
      numeroDocumento: '',
      telefono: '',
      email: '',
      tipoOcupante: 'ACOMPANANTE',
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
