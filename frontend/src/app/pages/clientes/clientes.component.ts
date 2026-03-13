import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, Subject, catchError, finalize, forkJoin, of, switchMap, takeUntil, tap } from 'rxjs';

import { TipoDocumento, TipoOcupante } from '../../models/enums';
import { ClienteTablaFiltros, ClienteTablaItem } from '../../models/cliente-tabla.model';
import { PageResponse } from '../../models/page.model';
import { OcupanteDTO, OcupanteNuevoRequest } from '../../models/ocupante.model';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';
import { OcupanteService } from '../../services/ocupante.service';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clientes.component.html',
  styleUrl: './clientes.component.css',
})
export class ClientesComponent implements OnInit, OnDestroy {
  readonly tiposDocumentoDisponibles: Array<{ label: string; value: TipoDocumento | '' }> = [
    { label: 'Todos', value: '' },
    { label: 'CC', value: 'CC' },
    { label: 'TI', value: 'TI' },
    { label: 'CE', value: 'CE' },
    { label: 'PA', value: 'PA' },
    { label: 'NIT', value: 'NIT' },
    { label: 'RC', value: 'RC' },
  ];

  readonly pageSizeOptions = [10, 20, 50, 100];

  filtros: ClienteTablaFiltros = this.crearFiltrosVacios();

  clientes: ClienteTablaItem[] = [];
  cargando = false;
  error = '';

  page = 0;
  size = 20;
  totalElements = 0;
  totalPages = 0;
  clienteSeleccionadoKey: string | null = null;
  modalAbierto = false;
  modalTitulo = '';
  modalTipo: 'RESERVA' | 'ESTANCIA' | null = null;
  modalItems: Array<{ codigo: string; estado: string; entrada: string | null; salida: string | null }> =
    [];
  modalEditarAbierto = false;
  modalCrearAbierto = false;
  guardandoEdicion = false;
  guardandoCreacion = false;
  ocupanteEditandoId: number | null = null;
  errorEdicion = '';
  errorCreacion = '';
  readonly tiposOcupanteEdicion: TipoOcupante[] = ['CLIENTE', 'ACOMPANANTE'];
  formularioEdicion: OcupanteNuevoRequest = this.crearFormularioVacio();
  formularioCreacion: OcupanteNuevoRequest = this.crearFormularioVacio();

  private readonly destroy$ = new Subject<void>();
  private readonly recargaTabla$ = new Subject<void>();
  private documentosPrefiltro: string[] = [];

  constructor(
    private readonly ocupanteService: OcupanteService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.inicializarCargaTabla();

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.documentosPrefiltro = this.parsearDocumentos(params.get('documentos'));
      this.filtros.nombre = (params.get('nombre') ?? '').trim();
      this.filtros.apellido = (params.get('apellido') ?? '').trim();
      this.filtros.tipoDocumento = this.parsearTipoDocumento(params.get('tipoDocumento'));
      this.filtros.numeroDocumento = (params.get('numeroDocumento') ?? '').trim();
      this.filtros.telefono = (params.get('telefono') ?? '').trim();
      this.filtros.email = (params.get('email') ?? '').trim();
      this.page = 0;
      this.solicitarRecargaTabla();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  aplicarFiltros(): void {
    this.page = 0;
    this.solicitarRecargaTabla();
  }

  limpiarFiltros(): void {
    this.filtros = this.crearFiltrosVacios();
    this.page = 0;
    this.solicitarRecargaTabla();
  }

  cambiarPageSize(size: number): void {
    this.size = size;
    this.page = 0;
    this.solicitarRecargaTabla();
  }

  irPaginaAnterior(): void {
    if (this.page <= 0 || this.cargando) {
      return;
    }

    this.page -= 1;
    this.solicitarRecargaTabla();
  }

  irPaginaSiguiente(): void {
    if (this.cargando || this.page + 1 >= this.totalPages) {
      return;
    }

    this.page += 1;
    this.solicitarRecargaTabla();
  }

  trackByCliente(_: number, cliente: ClienteTablaItem): string {
    return `${cliente.tipoDocumento ?? ''}-${cliente.numeroDocumento ?? ''}-${cliente.email ?? ''}`;
  }

  seleccionarCliente(cliente: ClienteTablaItem): void {
    const key = this.trackByCliente(0, cliente);
    this.clienteSeleccionadoKey = this.clienteSeleccionadoKey === key ? null : key;
  }

  esClienteSeleccionado(cliente: ClienteTablaItem): boolean {
    return this.trackByCliente(0, cliente) === this.clienteSeleccionadoKey;
  }

  editarClienteSeleccionado(): void {
    const cliente = this.obtenerClienteSeleccionado();
    if (!cliente || this.cargando || this.guardandoEdicion) {
      return;
    }

    this.abrirModalEditarCliente(cliente);
  }

  obtenerTooltipEditarSeleccionado(): string | null {
    if (!this.obtenerClienteSeleccionado()) {
      return 'Selecciona un cliente para editar.';
    }

    return null;
  }

  abrirModalReservas(cliente: ClienteTablaItem): void {
    this.modalTitulo = `Reservas de ${cliente.nombres} ${cliente.apellidos}`;
    this.modalTipo = 'RESERVA';
    this.modalItems = (cliente.reservas ?? []).map((reserva) => ({
      codigo: reserva.codigoReserva,
      estado: reserva.estado,
      entrada: reserva.entradaEstimada,
      salida: reserva.salidaEstimada,
    }));
    this.modalAbierto = true;
  }

  abrirModalEstancias(cliente: ClienteTablaItem): void {
    this.modalTitulo = `Estancias de ${cliente.nombres} ${cliente.apellidos}`;
    this.modalTipo = 'ESTANCIA';
    this.modalItems = (cliente.estancias ?? []).map((estancia) => ({
      codigo: estancia.codigoEstancia,
      estado: estancia.estado,
      entrada: estancia.entradaReal,
      salida: estancia.salidaReal ?? estancia.salidaEstimada,
    }));
    this.modalAbierto = true;
  }

  cerrarModal(): void {
    this.modalAbierto = false;
    this.modalTitulo = '';
    this.modalTipo = null;
    this.modalItems = [];
  }

  abrirModalEditarCliente(cliente: ClienteTablaItem): void {
    const numeroDocumento = (cliente.numeroDocumento ?? '').trim();
    if (!numeroDocumento) {
      this.error = 'No se puede editar un cliente sin numero de documento.';
      return;
    }

    this.error = '';
    this.errorEdicion = '';
    this.cargando = true;
    this.ocupanteService.buscarPorDocumento(numeroDocumento).subscribe({
      next: (ocupantes) => {
        const ocupante = this.seleccionarOcupanteParaEdicion(ocupantes ?? [], cliente);
        if (!ocupante) {
          this.error = 'No se encontro un ocupante editable para el cliente seleccionado.';
          this.cargando = false;
          return;
        }

        this.ocupanteEditandoId = ocupante.id;
        this.formularioEdicion = {
          nombres: ocupante.nombres,
          apellidos: ocupante.apellidos,
          tipoDocumento: ocupante.tipoDocumento,
          numeroDocumento: ocupante.numeroDocumento,
          telefono: ocupante.telefono,
          email: ocupante.email,
          tipoOcupante: ocupante.tipoOcupante,
        };
        this.modalEditarAbierto = true;
        this.cargando = false;
      },
      error: (errorResponse: unknown) => {
        this.error = extractBackendErrorMessage(errorResponse, 'No fue posible cargar el cliente para editar.');
        this.cargando = false;
      },
    });
  }

  abrirModalCrearCliente(): void {
    if (this.cargando || this.guardandoCreacion || this.guardandoEdicion) {
      return;
    }

    this.error = '';
    this.errorCreacion = '';
    this.formularioCreacion = this.crearFormularioVacio();
    this.modalCrearAbierto = true;
  }

  cerrarModalCrearCliente(): void {
    if (this.guardandoCreacion) {
      return;
    }

    this.modalCrearAbierto = false;
    this.errorCreacion = '';
    this.formularioCreacion = this.crearFormularioVacio();
  }

  guardarNuevoCliente(): void {
    if (this.guardandoCreacion) {
      return;
    }

    const request = this.normalizarFormulario(this.formularioCreacion);
    if (!request.nombres || !request.apellidos || !request.tipoDocumento || !request.numeroDocumento) {
      this.errorCreacion = 'Nombres, apellidos, tipo y numero de documento son obligatorios.';
      return;
    }

    this.guardandoCreacion = true;
    this.errorCreacion = '';
    this.ocupanteService.crearOcupante(request).subscribe({
      next: () => {
        this.guardandoCreacion = false;
        this.modalCrearAbierto = false;
        this.formularioCreacion = this.crearFormularioVacio();
        this.solicitarRecargaTabla();
      },
      error: (errorResponse: unknown) => {
        this.guardandoCreacion = false;
        this.errorCreacion = extractBackendErrorMessage(errorResponse, 'No fue posible crear el cliente.');
      },
    });
  }

  cerrarModalEditarCliente(): void {
    if (this.guardandoEdicion) {
      return;
    }

    this.modalEditarAbierto = false;
    this.ocupanteEditandoId = null;
    this.errorEdicion = '';
    this.formularioEdicion = this.crearFormularioVacio();
  }

  guardarEdicionCliente(): void {
    if (this.guardandoEdicion || this.ocupanteEditandoId === null) {
      return;
    }

    const request = this.normalizarFormulario(this.formularioEdicion);
    if (!request.nombres || !request.apellidos || !request.tipoDocumento || !request.numeroDocumento) {
      this.errorEdicion = 'Nombres, apellidos, tipo y numero de documento son obligatorios.';
      return;
    }

    this.guardandoEdicion = true;
    this.errorEdicion = '';
    this.ocupanteService.editarOcupante(this.ocupanteEditandoId, request).subscribe({
      next: () => {
        this.guardandoEdicion = false;
        this.modalEditarAbierto = false;
        this.ocupanteEditandoId = null;
        this.formularioEdicion = this.crearFormularioVacio();
        this.solicitarRecargaTabla();
      },
      error: (errorResponse: unknown) => {
        this.guardandoEdicion = false;
        this.errorEdicion = extractBackendErrorMessage(errorResponse, 'No fue posible editar el cliente.');
      },
    });
  }

  verMas(codigo: string): void {
    const codigoLimpio = (codigo ?? '').trim();
    if (!codigoLimpio || !this.modalTipo) {
      return;
    }

    const destino = this.modalTipo;
    this.cerrarModal();

    if (destino === 'RESERVA') {
      this.router.navigate(['/reservas'], {
        queryParams: { codigoReserva: codigoLimpio },
      });
      return;
    }

    this.router.navigate(['/estancias'], {
      queryParams: { codigoEstancia: codigoLimpio },
    });
  }

  formatearEtiqueta(valor: string | null | undefined): string {
    return (valor ?? '')
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
  }

  obtenerClaseTipoOcupante(tipoOcupante: string | null | undefined): string {
    const sufijo = (tipoOcupante ?? '').toLowerCase().replace(/_/g, '-');
    return `chip--tipo-ocupante-${sufijo}`;
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
    }).format(date);
  }

  get indiceInicio(): number {
    return this.totalElements === 0 ? 0 : this.page * this.size + 1;
  }

  get indiceFin(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }

  private inicializarCargaTabla(): void {
    this.recargaTabla$
      .pipe(
        takeUntil(this.destroy$),
        tap(() => {
          this.cargando = true;
          this.error = '';
        }),
        switchMap(() =>
          this.obtenerClientes$().pipe(
            finalize(() => {
              this.cargando = false;
            }),
            catchError((errorResponse: unknown) => {
              this.manejarErrorCargaTabla(errorResponse);
              return EMPTY;
            })
          )
        )
      )
      .subscribe((response) => {
        this.actualizarTabla(response);
      });
  }

  private solicitarRecargaTabla(): void {
    this.recargaTabla$.next();
  }

  private obtenerClientes$() {
    if (this.documentosPrefiltro.length) {
      return this.obtenerClientesPorDocumentos$(this.documentosPrefiltro);
    }

    return this.ocupanteService.buscarTablaClientes({
      nombre: this.filtros.nombre,
      apellido: this.filtros.apellido,
      tipoDocumento: this.filtros.tipoDocumento || undefined,
      numeroDocumento: this.filtros.numeroDocumento,
      telefono: this.filtros.telefono,
      email: this.filtros.email,
      pageable: {
        page: this.page,
        size: this.size,
        sort: ['nombres,asc'],
      },
    });
  }

  private obtenerClientesPorDocumentos$(documentos: string[]) {
    const solicitudes = documentos.map((numeroDocumento) =>
      this.ocupanteService
        .buscarTablaClientes({
          numeroDocumento,
          pageable: { page: 0, size: 20, sort: ['nombres,asc'] },
        })
        .pipe(
          catchError(() =>
            of<PageResponse<ClienteTablaItem>>({
              content: [],
              totalElements: 0,
              totalPages: 0,
              number: 0,
              size: 20,
              first: true,
              last: true,
            })
          )
        )
    );

    return forkJoin(solicitudes).pipe(
      switchMap((responses) => {
        const clientesUnicos = new Map<string, ClienteTablaItem>();
        responses.forEach((response) => {
          (response.content ?? []).forEach((cliente) => {
            clientesUnicos.set(this.trackByCliente(0, cliente), cliente);
          });
        });

        const content = Array.from(clientesUnicos.values());
        return of<PageResponse<ClienteTablaItem>>({
          content,
          totalElements: content.length,
          totalPages: content.length ? 1 : 0,
          number: 0,
          size: 20,
          first: true,
          last: true,
        });
      })
    );
  }

  private actualizarTabla(response: PageResponse<ClienteTablaItem>): void {
    this.clientes = response.content ?? [];
    this.sincronizarSeleccionCliente();
    this.totalElements = response.totalElements;
    this.totalPages = response.totalPages;
    this.page = response.number;
  }

  private manejarErrorCargaTabla(errorResponse: unknown): void {
    this.clientes = [];
    this.totalElements = 0;
    this.totalPages = 0;
    this.page = 0;
    this.clienteSeleccionadoKey = null;
    this.error = extractBackendErrorMessage(errorResponse, 'No fue posible cargar la tabla de clientes.');
  }

  private crearFiltrosVacios(): ClienteTablaFiltros {
    return {
      nombre: '',
      apellido: '',
      tipoDocumento: '',
      numeroDocumento: '',
      telefono: '',
      email: '',
    };
  }

  private parsearTipoDocumento(valor: string | null): TipoDocumento | '' {
    if (!valor) {
      return '';
    }

    const tiposDocumento: TipoDocumento[] = ['CC', 'TI', 'CE', 'PA', 'NIT', 'RC'];
    return tiposDocumento.includes(valor as TipoDocumento) ? (valor as TipoDocumento) : '';
  }

  private parsearDocumentos(valor: string | null): string[] {
    if (!valor) {
      return [];
    }

    return Array.from(
      new Set(
        valor
          .split(',')
          .map((item) => item.trim())
          .filter((item) => Boolean(item))
      )
    );
  }

  private seleccionarOcupanteParaEdicion(
    ocupantes: OcupanteDTO[],
    clienteTabla: ClienteTablaItem
  ): OcupanteDTO | null {
    const documentoObjetivo = (clienteTabla.numeroDocumento ?? '').trim().toLowerCase();
    const exactos = ocupantes.filter(
      (ocupante) => (ocupante.numeroDocumento ?? '').trim().toLowerCase() === documentoObjetivo
    );
    const candidatos = exactos.length ? exactos : ocupantes;

    if (!candidatos.length) {
      return null;
    }

    const tipoConsolidado = (clienteTabla.tipoOcupante ?? '').toUpperCase();
    if (tipoConsolidado === 'CLIENTE') {
      return candidatos.find((ocupante) => ocupante.tipoOcupante === 'CLIENTE') ?? candidatos[0];
    }
    if (tipoConsolidado === 'ACOMPANANTE') {
      return candidatos.find((ocupante) => ocupante.tipoOcupante === 'ACOMPANANTE') ?? candidatos[0];
    }

    return candidatos.find((ocupante) => ocupante.tipoOcupante === 'CLIENTE') ?? candidatos[0];
  }

  private crearFormularioVacio(): OcupanteNuevoRequest {
    return {
      nombres: '',
      apellidos: '',
      tipoDocumento: undefined,
      numeroDocumento: '',
      telefono: '',
      email: '',
      tipoOcupante: 'CLIENTE',
    };
  }

  private normalizarFormulario(formulario: OcupanteNuevoRequest): OcupanteNuevoRequest {
    return {
      nombres: formulario.nombres.trim(),
      apellidos: formulario.apellidos.trim(),
      tipoDocumento: formulario.tipoDocumento,
      numeroDocumento: formulario.numeroDocumento?.trim(),
      telefono: formulario.telefono?.trim(),
      email: formulario.email?.trim(),
      tipoOcupante: formulario.tipoOcupante,
    };
  }

  private obtenerClienteSeleccionado(): ClienteTablaItem | undefined {
    if (!this.clienteSeleccionadoKey) {
      return undefined;
    }

    return this.clientes.find((cliente) => this.trackByCliente(0, cliente) === this.clienteSeleccionadoKey);
  }

  private sincronizarSeleccionCliente(): void {
    if (!this.clienteSeleccionadoKey) {
      return;
    }

    const existe = this.clientes.some(
      (cliente) => this.trackByCliente(0, cliente) === this.clienteSeleccionadoKey
    );
    if (!existe) {
      this.clienteSeleccionadoKey = null;
    }
  }
}
