import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { DetalleCalendarioDTO } from '../models/detalle-calendario.model';
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
    let params = new HttpParams().set('mes', mes);

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

    return this.http.get<DetalleCalendarioDTO>(`${this.detalleBaseUrl}/calendario`, { params });
  }

  obtenerReservasCalendario(mes: string, tipoUnidad?: TipoUnidad, codigoUnidad?: string) {
    let params = new HttpParams().set('mes', mes);

    if (tipoUnidad) {
      params = params.set('tipoUnidad', tipoUnidad);
    }

    if (codigoUnidad) {
      params = params.set('codigoUnidad', codigoUnidad);
    }

    return this.http.get<ReservaCalendarioDTO[]>(`${this.baseUrl}/calendario`, { params });
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
    return this.http.post<ReservaDTO>(this.baseUrl, request);
  }

  editarReserva(id: number, request: ReservaNuevoRequest) {
    return this.http.put<void>(`${this.baseUrl}/${id}`, request);
  }

  obtenerReservaPorId(id: number) {
    return this.http.get<ReservaDTO>(`${this.baseUrl}/${id}`);
  }

  eliminarReserva(id: number) {
    return this.http.put<void>(`${this.baseUrl}/eliminar/${id}`, {});
  }

  buscarPorDocumento(numeroDocumento: string) {
    return this.http.get<ReservaCalendarioDTO[]>(`${this.baseUrl}/buscar-por-documento`, {
      params: { numero: numeroDocumento },
    });
  }

  private setParamIfValue(params: HttpParams, key: string, value?: string): HttpParams {
    const valueTrimmed = value?.trim();
    if (!valueTrimmed) {
      return params;
    }

    return params.set(key, valueTrimmed);
  }
}
