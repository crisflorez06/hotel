import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { environment } from '../../environments/environment';
import {
  EstanciaActivarRequest,
  EstanciaEditarRequest,
  EstanciaNuevoRequest,
  SalidaEstanciaRequest,
} from '../models/estancia.model';
import { EstanciaDTO } from '../models/estancia-detalle.model';
import { TipoUnidad } from '../models/enums';
import { PageResponse } from '../models/page.model';
import { EstanciaTablaFiltros, EstanciaTablaItem } from '../models/estancia-tabla.model';

@Injectable({
  providedIn: 'root',
})
export class EstanciaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/estancias`;

  crearEstancia(request: EstanciaNuevoRequest) {
    return this.http.post<void>(this.baseUrl, request);
  }

  activarEstancia(request: EstanciaActivarRequest) {
    return this.http.put<void>(`${this.baseUrl}/activar`, request);
  }

  editarEstancia(id: number, request: EstanciaEditarRequest) {
    return this.http.put<void>(`${this.baseUrl}/${id}`, request);
  }

  eliminarEstancia(id: number) {
    return this.http.put<void>(`${this.baseUrl}/eliminar/${id}`, {});
  }

  finalizarEstancia(request: SalidaEstanciaRequest) {
    return this.http.put<void>(`${this.baseUrl}/finalizar`, request);
  }

  obtenerEstanciaActiva(codigo: string, tipoUnidad: TipoUnidad) {
    return this.http.get<EstanciaDTO>(`${this.baseUrl}/activa`, {
      params: {
        codigo,
        tipoUnidad,
      },
    });
  }

  obtenerTabla(filtros: EstanciaTablaFiltros, page: number, size: number, sort: string[]) {
    let params = new HttpParams()
      .set('page', `${page}`)
      .set('size', `${size}`);

    filtros.estados.forEach((estado) => {
      params = params.append('estados', estado);
    });

    params = this.setParamIfValue(params, 'modoOcupacion', filtros.modoOcupacion);
    params = this.setParamIfValue(params, 'tipoUnidad', filtros.tipoUnidad);
    params = this.setParamIfValue(params, 'codigoEstancia', filtros.codigoEstancia);
    params = this.setParamIfValue(params, 'codigoUnidad', filtros.codigoUnidad);
    params = this.setParamIfValue(params, 'nombreCliente', filtros.nombreCliente);
    params = this.setParamIfValue(params, 'numeroDocumentoCliente', filtros.numeroDocumentoCliente);
    params = this.setParamIfValue(params, 'rangoGeneralDesde', filtros.rangoGeneralDesde);
    params = this.setParamIfValue(params, 'rangoGeneralHasta', filtros.rangoGeneralHasta);
    params = this.setParamIfValue(params, 'entradaDesde', filtros.entradaDesde);
    params = this.setParamIfValue(params, 'entradaHasta', filtros.entradaHasta);
    params = this.setParamIfValue(params, 'salidaEstimadaDesde', filtros.salidaEstimadaDesde);
    params = this.setParamIfValue(params, 'salidaEstimadaHasta', filtros.salidaEstimadaHasta);
    params = this.setParamIfValue(params, 'salidaRealDesde', filtros.salidaRealDesde);
    params = this.setParamIfValue(params, 'salidaRealHasta', filtros.salidaRealHasta);

    if (filtros.tieneReservaAsociada === 'SI') {
      params = params.set('tieneReservaAsociada', 'true');
    }
    if (filtros.tieneReservaAsociada === 'NO') {
      params = params.set('tieneReservaAsociada', 'false');
    }

    sort.forEach((orden) => {
      params = params.append('sort', orden);
    });

    return this.http.get<PageResponse<EstanciaTablaItem>>(`${this.baseUrl}/tabla`, {
      params,
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
