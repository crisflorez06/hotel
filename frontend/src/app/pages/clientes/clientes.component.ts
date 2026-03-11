import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, catchError, forkJoin, of } from 'rxjs';

import { TipoDocumento } from '../../models/enums';
import { ClienteTablaFiltros, ClienteTablaItem } from '../../models/cliente-tabla.model';
import { PageResponse } from '../../models/page.model';
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
  modalAbierto = false;
  modalTitulo = '';
  modalTipo: 'RESERVA' | 'ESTANCIA' | null = null;
  modalItems: Array<{ codigo: string; estado: string }> = [];

  private queryParamsSub: Subscription | null = null;
  private documentosPrefiltro: string[] = [];

  constructor(
    private readonly ocupanteService: OcupanteService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.queryParamsSub = this.route.queryParamMap.subscribe((params) => {
      this.documentosPrefiltro = this.parsearDocumentos(params.get('documentos'));
      this.filtros.nombre = (params.get('nombre') ?? '').trim();
      this.filtros.apellido = (params.get('apellido') ?? '').trim();
      this.filtros.tipoDocumento = this.parsearTipoDocumento(params.get('tipoDocumento'));
      this.filtros.numeroDocumento = (params.get('numeroDocumento') ?? '').trim();
      this.filtros.telefono = (params.get('telefono') ?? '').trim();
      this.filtros.email = (params.get('email') ?? '').trim();
      this.page = 0;

      if (this.documentosPrefiltro.length) {
        this.cargarClientesPorDocumentos(this.documentosPrefiltro);
        return;
      }

      this.cargarClientes();
    });
  }

  ngOnDestroy(): void {
    this.queryParamsSub?.unsubscribe();
    this.queryParamsSub = null;
  }

  aplicarFiltros(): void {
    this.page = 0;
    this.cargarClientes();
  }

  limpiarFiltros(): void {
    this.filtros = this.crearFiltrosVacios();
    this.page = 0;
    this.cargarClientes();
  }

  cambiarPageSize(size: number): void {
    this.size = size;
    this.page = 0;
    this.cargarClientes();
  }

  irPaginaAnterior(): void {
    if (this.page <= 0 || this.cargando) {
      return;
    }

    this.page -= 1;
    this.cargarClientes();
  }

  irPaginaSiguiente(): void {
    if (this.cargando || this.page + 1 >= this.totalPages) {
      return;
    }

    this.page += 1;
    this.cargarClientes();
  }

  trackByCliente(_: number, cliente: ClienteTablaItem): string {
    return `${cliente.tipoDocumento ?? ''}-${cliente.numeroDocumento ?? ''}-${cliente.email ?? ''}`;
  }

  abrirModalReservas(cliente: ClienteTablaItem): void {
    this.modalTitulo = `Reservas de ${cliente.nombres} ${cliente.apellidos}`;
    this.modalTipo = 'RESERVA';
    this.modalItems = (cliente.reservas ?? []).map((reserva) => ({
      codigo: reserva.codigoReserva,
      estado: reserva.estado,
    }));
    this.modalAbierto = true;
  }

  abrirModalEstancias(cliente: ClienteTablaItem): void {
    this.modalTitulo = `Estancias de ${cliente.nombres} ${cliente.apellidos}`;
    this.modalTipo = 'ESTANCIA';
    this.modalItems = (cliente.estancias ?? []).map((estancia) => ({
      codigo: estancia.codigoEstancia,
      estado: estancia.estado,
    }));
    this.modalAbierto = true;
  }

  cerrarModal(): void {
    this.modalAbierto = false;
    this.modalTitulo = '';
    this.modalTipo = null;
    this.modalItems = [];
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

  get indiceInicio(): number {
    return this.totalElements === 0 ? 0 : this.page * this.size + 1;
  }

  get indiceFin(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }

  private cargarClientes(): void {
    if (this.documentosPrefiltro.length) {
      this.cargarClientesPorDocumentos(this.documentosPrefiltro);
      return;
    }

    this.cargando = true;
    this.error = '';

    this.ocupanteService
      .buscarTablaClientes({
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
      })
      .subscribe({
        next: (response) => {
          this.clientes = response.content ?? [];
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.page = response.number;
          this.cargando = false;
        },
        error: (errorResponse: unknown) => {
          this.clientes = [];
          this.error = extractBackendErrorMessage(
            errorResponse,
            'No fue posible cargar la tabla de clientes.'
          );
          this.cargando = false;
        },
      });
  }

  private cargarClientesPorDocumentos(documentos: string[]): void {
    this.cargando = true;
    this.error = '';

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

    forkJoin(solicitudes).subscribe({
      next: (responses) => {
        const clientesUnicos = new Map<string, ClienteTablaItem>();
        responses.forEach((response) => {
          if (!response) {
            return;
          }
          (response.content ?? []).forEach((cliente: ClienteTablaItem) => {
            clientesUnicos.set(this.trackByCliente(0, cliente), cliente);
          });
        });

        this.clientes = Array.from(clientesUnicos.values());
        this.totalElements = this.clientes.length;
        this.totalPages = this.clientes.length ? 1 : 0;
        this.page = 0;
        this.cargando = false;
      },
      error: (errorResponse: unknown) => {
        this.clientes = [];
        this.error = extractBackendErrorMessage(
          errorResponse,
          'No fue posible cargar los clientes filtrados.'
        );
        this.totalElements = 0;
        this.totalPages = 0;
        this.page = 0;
        this.cargando = false;
      },
    });
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
}
