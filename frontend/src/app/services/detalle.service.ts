import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { environment } from '../../environments/environment';
import {
  DashboardAlertasResponseDTO,
  DashboardConteoUnidadDTO,
  DashboardDistribucionFinancieraDTO,
  DashboardGranularidad,
  DashboardResumenDTO,
  DashboardSerieFinancieraDTO,
} from '../models/dashboard.model';
import { DetalleEstanciaDTO } from '../models/estancia-detalle.model';

@Injectable({
  providedIn: 'root',
})
export class DetalleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/detalle`;

  obtenerDetalleEstancia(id: number) {
    return this.http.get<DetalleEstanciaDTO>(`${this.baseUrl}/estancia/${id}`);
  }

  obtenerDashboardResumen(desde?: string, hasta?: string) {
    const params = this.construirRangoParams(desde, hasta);
    return this.http.get<DashboardResumenDTO>(`${this.baseUrl}/dashboard/resumen`, { params });
  }

  obtenerDashboardConteosUnidad() {
    return this.http.get<DashboardConteoUnidadDTO[]>(`${this.baseUrl}/dashboard/operativo/conteos-unidad`);
  }

  obtenerDashboardSerieFinanciera(desde?: string, hasta?: string, granularidad: DashboardGranularidad = 'DAY') {
    let params = this.construirRangoParams(desde, hasta);
    params = params.set('granularidad', granularidad);
    return this.http.get<DashboardSerieFinancieraDTO[]>(`${this.baseUrl}/dashboard/financiero/serie`, { params });
  }

  obtenerDashboardDistribucionFinanciera(desde?: string, hasta?: string) {
    const params = this.construirRangoParams(desde, hasta);
    return this.http.get<DashboardDistribucionFinancieraDTO>(`${this.baseUrl}/dashboard/financiero/distribucion`, {
      params,
    });
  }

  obtenerDashboardAlertas(dias = 7, page = 0, size = 20) {
    const params = new HttpParams().set('dias', `${dias}`).set('page', `${page}`).set('size', `${size}`);
    return this.http.get<DashboardAlertasResponseDTO>(`${this.baseUrl}/dashboard/alertas`, { params });
  }

  private construirRangoParams(desde?: string, hasta?: string): HttpParams {
    let params = new HttpParams();
    params = this.setParamIfValue(params, 'desde', desde);
    params = this.setParamIfValue(params, 'hasta', hasta);
    return params;
  }

  private setParamIfValue(params: HttpParams, key: string, value?: string): HttpParams {
    const trimmed = value?.trim();
    if (!trimmed) {
      return params;
    }

    return params.set(key, trimmed);
  }
}
