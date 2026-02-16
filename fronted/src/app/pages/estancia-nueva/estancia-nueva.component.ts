import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FlatpickrModule } from 'angularx-flatpickr';

import { EstanciaService } from '../../services/estancia.service';
import { OcupanteService } from '../../services/ocupante.service';
import { EstadoPago, MedioPago, TipoDocumento, TipoOcupante, TipoPago, TipoUnidad } from '../../models/enums';
import { EstanciaActivarRequest, EstanciaEditarRequest, EstanciaNuevoRequest } from '../../models/estancia.model';
import { OcupanteDTO, OcupanteNuevoRequest } from '../../models/ocupante.model';
import { PagoService } from '../../services/pago.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

@Component({
  selector: 'app-estancia-nueva',
  standalone: true,
  imports: [CommonModule, FormsModule, FlatpickrModule],
  templateUrl: './estancia-nueva.component.html',
  styleUrl: './estancia-nueva.component.css',
})
export class EstanciaNuevaComponent implements OnInit {
  codigo = '';
  tipoUnidad: TipoUnidad | '' = '';
  estanciaId: number | null = null;
  esEdicion = false;
  idReserva: number | null = null;
  idPagoReserva: number | null = null;

  idCliente: number | null = null;
  entradaReal = '';
  salidaEstimada = '';
  idAcompanantes = '';
  notas = '';

  conPago = false;
  tipoPago: TipoPago = 'ANTICIPO_ESTANCIA';
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
  mostrarToast = false;
  toastMensaje = '';

  mediosPago: MedioPago[] = [
    'EFECTIVO',
    'TARJETA_CREDITO',
    'TARJETA_DEBITO',
    'TRANSFERENCIA_BANCARIA',
    'PLATAFORMA',
  ];

  estadosPago: EstadoPago[] = ['PENDIENTE', 'COMPLETADO', 'FALLIDO'];
  tiposDocumento: TipoDocumento[] = ['CC', 'TI', 'CE', 'PA', 'NIT', 'RC'];
  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly estanciaService: EstanciaService,
    private readonly ocupanteService: OcupanteService,
    private readonly pagoService: PagoService
  ) {}

  ngOnInit(): void {
    const state = (this.router.getCurrentNavigation()?.extras.state ??
      history.state) as Partial<Record<string, unknown>> | null;
    const queryParams = this.route.snapshot.queryParamMap;

    const codigo =
      (state?.['codigo'] as string | undefined) ?? queryParams.get('codigo') ?? '';
    const tipo =
      (state?.['tipo'] as TipoUnidad | undefined) ??
      (queryParams.get('tipo') as TipoUnidad | null) ??
      '';
    const idClienteParam =
      (state?.['idCliente'] as number | string | undefined) ?? queryParams.get('idCliente') ?? undefined;
    const idPagoReservaParam =
      (state?.['idPagoReserva'] as number | string | undefined) ??
      queryParams.get('idPagoReserva') ??
      undefined;
    const nombreClienteParam =
      (state?.['nombreCliente'] as string | undefined) ?? queryParams.get('nombreCliente') ?? undefined;
    const idReservaParam =
      (state?.['idReserva'] as number | string | undefined) ?? queryParams.get('idReserva') ?? undefined;
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

    this.codigo = codigo;
    this.tipoUnidad = tipo;

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

    if (idPagoReservaParam !== undefined && idPagoReservaParam !== null && idPagoReservaParam !== '') {
      const parsed =
        typeof idPagoReservaParam === 'number'
          ? idPagoReservaParam
          : Number.parseInt(idPagoReservaParam, 10);
      this.idPagoReserva = Number.isNaN(parsed) ? null : parsed;
    } else {
      this.idPagoReserva = null;
    }
    this.esEdicion = editarParam === 'true' || editarParam === '1' || !!estanciaIdParam;
    this.estanciaId = estanciaIdParam
      ? typeof estanciaIdParam === 'number'
        ? estanciaIdParam
        : Number.parseInt(estanciaIdParam, 10)
      : null;

    if (this.esEdicion) {
      if (!this.codigo || !this.tipoUnidad) {
        this.error = 'No se encontraron datos de la unidad para editar la estancia.';
        return;
      }
      this.cargarEstanciaParaEdicion(this.codigo, this.tipoUnidad as TipoUnidad);
      return;
    }

    if (entradaParam) {
      this.entradaReal = this.formatearFechaHora(entradaParam);
    }

    if (salidaParam) {
      this.salidaEstimada = this.formatearFechaHora(salidaParam);
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

    if (this.esEdicion) {
      if (!this.estanciaId) {
        this.guardando = false;
        this.error = 'No se encontro la estancia a editar.';
        return;
      }
      const pago = this.conPago ? this.buildPago() : null;
      if (this.conPago && !pago) {
        this.guardando = false;
        this.error = 'Completa los campos de pago obligatorios.';
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
        pago,
      };

      this.estanciaService.editarEstancia(this.estanciaId, request).subscribe({
        next: () => {
          this.guardando = false;
          this.exito = 'Estancia actualizada con exito.';
          this.mostrarToastExito('Estancia actualizada con exito.', true);
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

    if (this.idReserva) {
      const request: EstanciaActivarRequest = {
        idReserva: this.idReserva,
        idCliente: this.idCliente,
        entradaReal: this.normalizarFechaHora(this.entradaReal),
        salidaEstimada: this.normalizarFechaHora(this.salidaEstimada),
        idAcompanantes: this.parseAcompanantes(),
        pago: this.conPago ? this.buildPago() : null,
      };

      this.estanciaService.activarEstancia(request).subscribe({
        next: () => {
          this.guardando = false;
          this.exito = 'Estancia activada con exito.';
          this.mostrarToastExito('Estancia activada con exito.', true);
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
      idReserva: this.idReserva ?? undefined,
      idCliente: this.idCliente,
      entradaReal: this.normalizarFechaHora(this.entradaReal),
      salidaEstimada: this.normalizarFechaHora(this.salidaEstimada),
      idAcompanantes: this.parseAcompanantes(),
      notas: this.notas || undefined,
      pago: this.conPago ? this.buildPago() : null,
    };

    this.estanciaService.crearEstancia(request).subscribe({
      next: () => {
        this.guardando = false;
        this.exito = 'Estancia registrada con exito.';
        this.mostrarToastExito('Estancia registrada con exito.', true);
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
    if (!this.conPago) {
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
        ...(this.idPagoReserva ? { idPagoReserva: this.idPagoReserva } : {}),
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
        this.mostrarToastExito(`Cliente creado: ${cliente.nombres} ${cliente.apellidos}.`);
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
        this.mostrarToastExito(
          `Acompanante creado: ${acompanante.nombres} ${acompanante.apellidos}.`
        );
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

  volver(): void {
    this.router.navigate(['/recepcion']);
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

  private buildPago() {
    if (!this.monto || !this.fechaPago) {
      return undefined;
    }

    return {
      tipoPago: this.tipoPago,
      monto: this.monto,
      medioPago: this.medioPago,
      fecha: this.normalizarFechaHora(this.fechaPago),
      estado: this.estadoPago,
    };
  }

  private normalizarFechaHora(valor: string): string {
    return valor.replace(' ', 'T');
  }

  private cargarEstanciaParaEdicion(codigo: string, tipoUnidad: TipoUnidad): void {
    this.estanciaService.obtenerEstanciaActiva(codigo, tipoUnidad).subscribe({
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

  private mostrarToastExito(mensaje: string, navegar = false): void {
    this.toastMensaje = mensaje;
    this.mostrarToast = true;
    setTimeout(() => {
      this.mostrarToast = false;
      if (navegar) {
        this.router.navigate(['/recepcion']);
      }
    }, 1500);
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
