import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FlatpickrModule } from 'angularx-flatpickr';

import { EstanciaService } from '../../services/estancia.service';
import { OcupanteService } from '../../services/ocupante.service';
import { EstadoPago, MedioPago, TipoDocumento, TipoOcupante, TipoPago, TipoUnidad } from '../../models/enums';
import { EstanciaEditarRequest, EstanciaNuevoRequest } from '../../models/estancia.model';
import { OcupanteDTO, OcupanteNuevoRequest } from '../../models/ocupante.model';

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

  idCliente: number | null = null;
  entradaReal = '';
  salidaEstimada = '';
  idAcompanantes = '';
  notas = '';

  conPago = false;
  tipoPago: TipoPago = 'ESTANCIA';
  monto: number | null = null;
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

  estadosPago: EstadoPago[] = ['PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO'];
  tiposDocumento: TipoDocumento[] = ['CC', 'TI', 'CE', 'PA', 'NIT', 'RC'];
  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly estanciaService: EstanciaService,
    private readonly ocupanteService: OcupanteService
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.codigo = params.get('codigo') ?? '';
    this.tipoUnidad = (params.get('tipo') as TipoUnidad) ?? '';
    const editarParam = params.get('editar');
    const estanciaIdParam = params.get('estanciaId');

    this.esEdicion = editarParam === 'true' || editarParam === '1' || !!estanciaIdParam;
    this.estanciaId = estanciaIdParam ? Number.parseInt(estanciaIdParam, 10) : null;

    if (this.esEdicion) {
      if (!this.codigo || !this.tipoUnidad) {
        this.error = 'No se encontraron datos de la unidad para editar la estancia.';
        return;
      }
      this.cargarEstanciaParaEdicion(this.codigo, this.tipoUnidad as TipoUnidad);
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

      const request: EstanciaEditarRequest = {
        idCliente: this.idCliente,
        entradaReal: this.normalizarFechaHora(this.entradaReal),
        salidaEstimada: this.normalizarFechaHora(this.salidaEstimada),
        idAcompanantes: this.parseAcompanantes(),
        notas: this.notas || undefined,
      };

      this.estanciaService.editarEstancia(this.estanciaId, request).subscribe({
        next: () => {
          this.guardando = false;
          this.exito = 'Estancia actualizada con exito.';
          this.mostrarToastExito('Estancia actualizada con exito.', true);
        },
        error: () => {
          this.guardando = false;
          this.error = 'No fue posible actualizar la estancia.';
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
      pago: this.conPago ? this.buildPago() : null,
    };

    this.estanciaService.crearEstancia(request).subscribe({
      next: () => {
        this.guardando = false;
        this.exito = 'Estancia registrada con exito.';
        this.mostrarToastExito('Estancia registrada con exito.', true);
      },
      error: () => {
        this.guardando = false;
        this.error = 'No fue posible registrar la estancia.';
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
      error: () => {
        this.creandoCliente = false;
        this.clienteError = 'No fue posible crear el cliente.';
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
      error: () => {
        this.buscandoCliente = false;
        this.clienteBusquedaError = 'No fue posible buscar los clientes.';
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
        }
        this.mostrarModalAcompanante = false;
        this.limpiarAcompananteNuevo();
        this.mostrarToastExito(
          `Acompanante creado: ${acompanante.nombres} ${acompanante.apellidos}.`
        );
      },
      error: () => {
        this.creandoAcompanante = false;
        this.acompananteError = 'No fue posible crear el acompanante.';
      },
    });
  }

  quitarAcompanante(id: number): void {
    this.acompanantesCreados = this.acompanantesCreados.filter((item) => item.id !== id);
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
      error: () => {
        this.buscandoAcompanante = false;
        this.acompananteBusquedaError = 'No fue posible buscar los acompanantes.';
      },
    });
  }

  seleccionarAcompanante(acompanante: OcupanteDTO): void {
    if (!this.acompanantesCreados.some((item) => item.id === acompanante.id)) {
      this.acompanantesCreados = [...this.acompanantesCreados, acompanante];
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
      error: () => {
        this.error = 'No fue posible cargar la estancia para editar.';
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
