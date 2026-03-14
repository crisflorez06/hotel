import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  DetalleCalendarioDTO,
  DetalleCalendarioUnidadDTO,
  EstanciaCalendarioDTO,
  ReservaCalendarioResumenDTO,
} from '../models/detalle-calendario.model';
import { EstanciaDTO } from '../models/estancia-detalle.model';
import { ReservaCalendarioDTO } from '../models/reserva-calendario.model';
import { ReservaDTO, ReservaNuevoRequest } from '../models/reserva.model';
import { EstadoEstancia, EstadoReserva, TipoUnidad } from '../models/enums';
import { PageResponse } from '../models/page.model';
import { ReservaTablaFiltros, ReservaTablaItem } from '../models/reserva-tabla.model';

@Injectable({
  providedIn: 'root',
})
export class ReservaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reservas`;
  private readonly detalleBaseUrl = `${environment.apiUrl}/detalle`;

  obtenerCalendario(
    mes: string,
    tipoUnidad?: TipoUnidad,
    codigoUnidad?: string,
    estadosReserva?: EstadoReserva[],
    estadosEstancia?: EstadoEstancia[],
  ) {
    const { desde, hasta } = this.construirRangoMes(mes);
    let params = new HttpParams().set('desde', desde).set('hasta', hasta);

    if (tipoUnidad) {
      params = params.set('tipoUnidad', tipoUnidad);
    }

    if (codigoUnidad) {
      params = params.set('codigoUnidad', codigoUnidad);
    }

    (estadosReserva ?? []).forEach((estado) => {
      params = params.append('estadosReserva', estado);
    });

    (estadosEstancia ?? []).forEach((estado) => {
      params = params.append('estadosEstancia', estado);
    });

    return this.http
      .get<DetalleCalendarioUnidadDTO[]>(`${this.detalleBaseUrl}/calendario`, { params })
      .pipe(
        map((unidades) => this.normalizarDetalleCalendarioUnidades(unidades ?? [])),
        map((unidades) => this.mapearDetalleCalendario(unidades))
      );
  }

  obtenerReservasCalendario(mes: string, tipoUnidad?: TipoUnidad, codigoUnidad?: string) {
    let params = new HttpParams().set('mes', mes);

    if (tipoUnidad) {
      params = params.set('tipoUnidad', tipoUnidad);
    }

    if (codigoUnidad) {
      params = params.set('codigoUnidad', codigoUnidad);
    }

    return this.http
      .get<ReservaCalendarioDTO[]>(`${this.baseUrl}/calendario`, { params })
      .pipe(map((reservas) => reservas ?? []));
  }

  obtenerCalendarioDetalle(
    desde: string,
    hasta: string,
    tipoUnidad?: TipoUnidad,
    codigoUnidad?: string,
    estadosReserva?: EstadoReserva[],
    estadosEstancia?: EstadoEstancia[],
  ) {
    let params = new HttpParams().set('desde', desde).set('hasta', hasta);

    if (tipoUnidad) {
      params = params.set('tipoUnidad', tipoUnidad);
    }

    if (codigoUnidad) {
      params = params.set('codigoUnidad', codigoUnidad);
    }

    (estadosReserva ?? []).forEach((estado) => {
      params = params.append('estadosReserva', estado);
    });

    (estadosEstancia ?? []).forEach((estado) => {
      params = params.append('estadosEstancia', estado);
    });

    return this.http
      .get<DetalleCalendarioUnidadDTO[]>(`${this.detalleBaseUrl}/calendario`, { params })
      .pipe(map((unidades) => this.normalizarDetalleCalendarioUnidades(unidades ?? [])));
  }

  obtenerTabla(filtros: ReservaTablaFiltros, page: number, size: number, sort: string[]) {
    let params = new HttpParams()
      .set('page', `${page}`)
      .set('size', `${size}`);

    filtros.estados.forEach((estado) => {
      params = params.append('estados', estado);
    });

    filtros.canales.forEach((canal) => {
      params = params.append('canales', canal);
    });

    params = this.setParamIfValue(params, 'modoOcupacion', filtros.modoOcupacion);
    params = this.setParamIfValue(params, 'tipoUnidad', filtros.tipoUnidad);
    if (typeof filtros.idReserva === 'number') {
      params = params.set('idReserva', `${filtros.idReserva}`);
    }
    params = this.setParamIfValue(params, 'codigoReserva', filtros.codigoReserva);
    params = this.setParamIfValue(params, 'codigoUnidad', filtros.codigoUnidad);
    params = this.setParamIfValue(params, 'nombreCliente', filtros.nombreCliente);
    params = this.setParamIfValue(
      params,
      'numeroDocumentoCliente',
      filtros.numeroDocumentoCliente,
    );
    params = this.setParamIfValue(params, 'rangoGeneralDesde', filtros.rangoGeneralDesde);
    params = this.setParamIfValue(params, 'rangoGeneralHasta', filtros.rangoGeneralHasta);
    params = this.setParamIfValue(params, 'fechaCreacionDesde', filtros.fechaCreacionDesde);
    params = this.setParamIfValue(params, 'fechaCreacionHasta', filtros.fechaCreacionHasta);
    params = this.setParamIfValue(params, 'entradaDesde', filtros.entradaDesde);
    params = this.setParamIfValue(params, 'entradaHasta', filtros.entradaHasta);
    params = this.setParamIfValue(params, 'salidaDesde', filtros.salidaDesde);
    params = this.setParamIfValue(params, 'salidaHasta', filtros.salidaHasta);

    sort.forEach((orden) => {
      params = params.append('sort', orden);
    });

    return this.http.get<PageResponse<ReservaTablaItem>>(`${this.baseUrl}/tabla`, {
      params,
    });
  }

  crearReserva(request: ReservaNuevoRequest) {
    return this.http
      .post<ReservaDTO>(this.baseUrl, request)
      .pipe(map((reserva) => this.normalizarReserva(reserva)));
  }

  editarReserva(id: number, request: ReservaNuevoRequest) {
    return this.http.put<void>(`${this.baseUrl}/${id}`, request);
  }

  obtenerReservaPorId(id: number) {
    return this.http
      .get<ReservaDTO>(`${this.baseUrl}/${id}`)
      .pipe(map((reserva) => this.normalizarReserva(reserva)));
  }

  eliminarReserva(id: number) {
    return this.http.put<void>(`${this.baseUrl}/eliminar/${id}`, {});
  }

  buscarPorDocumento(numeroDocumento: string) {
    return this.http.get<ReservaCalendarioDTO[]>(`${this.baseUrl}/buscar-por-documento`, {
      params: { numero: numeroDocumento },
    });
  }

  private normalizarReserva(reserva: ReservaDTO): ReservaDTO {
    return {
      ...reserva,
      cliente: reserva.cliente ?? null,
    };
  }

  private normalizarDetalleCalendarioUnidades(
    unidades: DetalleCalendarioUnidadDTO[]
  ): DetalleCalendarioUnidadDTO[] {
    return unidades.map((unidad) => ({
      ...unidad,
      reservas: (unidad.reservas ?? []).map((reserva) => this.normalizarReserva(reserva)),
      estancias: (unidad.estancias ?? []).map((estancia) => this.normalizarEstancia(estancia)),
      habitaciones: (unidad.habitaciones ?? []).map((habitacion) => ({
        ...habitacion,
        reservas: (habitacion.reservas ?? []).map((reserva) => this.normalizarReserva(reserva)),
        estancias: (habitacion.estancias ?? []).map((estancia) => this.normalizarEstancia(estancia)),
      })),
    }));
  }

  private normalizarEstancia(estancia: EstanciaDTO): EstanciaDTO {
    return {
      ...estancia,
      cliente: estancia.cliente ?? null,
      acompanantes: estancia.acompanantes ?? [],
    };
  }

  private obtenerNombreClienteReserva(reserva: ReservaDTO): string | null {
    const nombres = reserva.cliente?.nombres?.trim() ?? '';
    const apellidos = reserva.cliente?.apellidos?.trim() ?? '';
    const nombre = `${nombres} ${apellidos}`.trim();
    return nombre || null;
  }

  private obtenerNombreClienteEstancia(estancia: EstanciaDTO): string | null {
    const cliente = estancia.cliente;

    if (!cliente) {
      return null;
    }

    const nombre = `${cliente.nombres ?? ''} ${cliente.apellidos ?? ''}`.trim();
    return nombre || null;
  }

  private setParamIfValue(params: HttpParams, key: string, value?: string): HttpParams {
    const valueTrimmed = value?.trim();
    if (!valueTrimmed) {
      return params;
    }

    return params.set(key, valueTrimmed);
  }

  private construirRangoMes(mes: string): { desde: string; hasta: string } {
    const [anioTexto, mesTexto] = mes.split('-');
    const anio = Number.parseInt(anioTexto, 10);
    const numeroMes = Number.parseInt(mesTexto, 10);

    if (Number.isNaN(anio) || Number.isNaN(numeroMes) || numeroMes < 1 || numeroMes > 12) {
      const ahora = new Date();
      const inicio = new Date(ahora.getFullYear(), ahora.getMonth(), 1, 0, 0, 0);
      const fin = new Date(ahora.getFullYear(), ahora.getMonth() + 1, 0, 23, 59, 59);
      return {
        desde: this.formatearFechaIsoLocal(inicio),
        hasta: this.formatearFechaIsoLocal(fin),
      };
    }

    const inicio = new Date(anio, numeroMes - 1, 1, 0, 0, 0);
    const fin = new Date(anio, numeroMes, 0, 23, 59, 59);
    return {
      desde: this.formatearFechaIsoLocal(inicio),
      hasta: this.formatearFechaIsoLocal(fin),
    };
  }

  private formatearFechaIsoLocal(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = `${fecha.getMonth() + 1}`.padStart(2, '0');
    const dia = `${fecha.getDate()}`.padStart(2, '0');
    const horas = `${fecha.getHours()}`.padStart(2, '0');
    const minutos = `${fecha.getMinutes()}`.padStart(2, '0');
    const segundos = `${fecha.getSeconds()}`.padStart(2, '0');
    const milis = `${fecha.getMilliseconds()}`.padStart(3, '0');
    return `${anio}-${mes}-${dia}T${horas}:${minutos}:${segundos}.${milis}`;
  }

  private mapearDetalleCalendario(unidades: DetalleCalendarioUnidadDTO[]): DetalleCalendarioDTO {
    const reservasMap = new Map<number, ReservaCalendarioResumenDTO>();
    const estanciasMap = new Map<number, EstanciaCalendarioDTO>();

    unidades.forEach((detalle) => {
      const codigoUnidad = detalle.unidad?.codigo ?? null;
      const tipoUnidad = detalle.unidad?.tipo ?? null;

      (detalle.reservas ?? []).forEach((reservaRaw) => {
        const reserva = this.normalizarReserva(reservaRaw);
        if (!reserva?.id) {
          return;
        }
        reservasMap.set(reserva.id, {
          id: reserva.id,
          idEstancia: reserva.idEstancia ?? null,
          inicio: `${reserva.entradaEstimada}`,
          fin: `${reserva.salidaEstimada}`,
          codigoReserva: reserva.codigoReserva ?? null,
          codigoUnidad,
          tipoUnidad,
          numeroPersonas: reserva.numeroPersonas ?? null,
          nombreCliente: this.obtenerNombreClienteReserva(reserva),
          idCliente: reserva.cliente?.id ?? null,
          totalAnticipo: null,
          estadoReserva: reserva.estadoReserva as EstadoReserva,
        });
      });

      (detalle.estancias ?? []).forEach((estanciaRaw) => {
        const estancia = this.normalizarEstancia(estanciaRaw);
        if (!estancia?.id) {
          return;
        }
        const totalPagado = (estancia.pagos ?? [])
          .filter((pago) => pago.estado === 'COMPLETADO')
          .reduce((acc, pago) => acc + Number(pago.monto ?? 0), 0);

        estanciasMap.set(estancia.id, {
          id: estancia.id,
          inicio: `${estancia.entradaReal}`,
          fin: `${estancia.salidaEstimada}`,
          codigoEstancia: estancia.codigoFolio ?? null,
          codigoUnidad,
          tipoUnidad,
          numeroPersonas: this.contarPersonasEstancia(estancia),
          nombreCliente: this.obtenerNombreClienteEstancia(estancia),
          idCliente: estancia.cliente?.id ?? null,
          totalPagado,
          estadoEstancia: this.resolverEstadoEstancia(detalle, estancia.id),
        });
      });
    });

    return {
      reservas: Array.from(reservasMap.values()),
      estancias: Array.from(estanciasMap.values()),
    };
  }

  private contarPersonasEstancia(estancia: EstanciaDTO): number {
    const cliente = estancia.cliente ? 1 : 0;
    return cliente + (estancia.acompanantes?.length ?? 0);
  }

  private resolverEstadoEstancia(
    detalleUnidad: DetalleCalendarioUnidadDTO,
    idEstancia: number
  ): EstadoEstancia {
    const fuentes = [
      detalleUnidad.unidad?.informacionAdicional,
      ...(detalleUnidad.unidad?.habitaciones ?? []).map((habitacion) => habitacion.informacionAdicional),
    ];

    for (const informacionRaw of fuentes) {
      if (!informacionRaw) {
        continue;
      }
      try {
        const informacion = JSON.parse(informacionRaw) as {
          ESTANCIA?: { idEstancia?: number | string; estado?: EstadoEstancia };
        };
        const idInfo = Number(informacion?.ESTANCIA?.idEstancia);
        if (idInfo === idEstancia && informacion.ESTANCIA?.estado) {
          return informacion.ESTANCIA.estado;
        }
      } catch {
        continue;
      }
    }

    return 'ACTIVA';
  }
}
