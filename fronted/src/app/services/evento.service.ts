import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { PageResponse } from '../models/page.model';
import { EventoMonitorFiltros, EventoMonitorItem } from '../models/evento-monitor.model';

@Injectable({
  providedIn: 'root',
})
export class EventoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/eventos`;

  obtenerEventos(filtros: EventoMonitorFiltros, page = 0, size = 20, sort: string[] = ['fecha,desc']) {
    let params = new HttpParams()
      .set('page', `${page}`)
      .set('size', `${size}`);

    filtros.tiposEvento.forEach((tipoEvento) => {
      params = params.append('tiposEvento', tipoEvento);
    });

    filtros.entidades.forEach((entidad) => {
      params = params.append('entidades', entidad);
    });

    params = this.setParamIfValue(params, 'codigoReserva', filtros.codigoReserva);
    params = this.setParamIfValue(params, 'codigoEstancia', filtros.codigoEstancia);
    params = this.setParamIfValue(params, 'fechaDesde', filtros.fechaDesde);
    params = this.setParamIfValue(params, 'fechaHasta', filtros.fechaHasta);

    sort.forEach((orden) => {
      params = params.append('sort', orden);
    });

    return this.http.get<PageResponse<EventoMonitorItem>>(this.baseUrl, { params });
  }

  private setParamIfValue(params: HttpParams, key: string, value?: string): HttpParams {
    const valueTrimmed = value?.trim();
    if (!valueTrimmed) {
      return params;
    }

    return params.set(key, valueTrimmed);
  }
}
