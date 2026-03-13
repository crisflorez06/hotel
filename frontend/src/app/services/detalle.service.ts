import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { environment } from '../../environments/environment';
import {
  DashboardDistribucionFinancieraDTO,
  DashboardResumenDTO,
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

  obtenerDashboardResumen() {
    return this.http.get<DashboardResumenDTO>(`${this.baseUrl}/dashboard/resumen`);
  }

  obtenerDashboardDistribucionFinanciera(desde?: string, hasta?: string) {
    const params = this.construirRangoParams(desde, hasta);
    return this.http.get<DashboardDistribucionFinancieraDTO>(`${this.baseUrl}/dashboard/financiero/distribucion`, {
      params,
    });
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
