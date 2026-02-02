import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { PageResponse } from '../models/page.model';
import { PagoDTO } from '../models/pago-detalle.model';
import { EstadoPago, MedioPago, TipoPago, TipoUnidad } from '../models/enums';

export interface CalcularPagoRequest {
  tipoUnidad: TipoUnidad;
  numeroPersonas: number;
  fechaEntrada: string;
  fechaSalida: string;
}

export interface BuscarPagosParams {
  page?: number;
  size?: number;
  estados?: EstadoPago[];
  mediosPago?: MedioPago[];
  tipoPago?: TipoPago;
  fechaDesde?: string;
  fechaHasta?: string;
}

@Injectable({
  providedIn: 'root',
})
export class PagoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/pagos`;

  buscarPagos(params?: BuscarPagosParams) {
    let httpParams = new HttpParams()
      .set('page', params?.page ?? 0)
      .set('size', params?.size ?? 10);

    if (params?.estados?.length) {
      params.estados.forEach((estado) => {
        httpParams = httpParams.append('estados', estado);
      });
    }

    if (params?.mediosPago?.length) {
      params.mediosPago.forEach((medio) => {
        httpParams = httpParams.append('mediosPago', medio);
      });
    }

    if (params?.tipoPago) {
      httpParams = httpParams.set('tipoPago', params.tipoPago);
    }

    if (params?.fechaDesde) {
      httpParams = httpParams.set('fechaDesde', params.fechaDesde);
    }

    if (params?.fechaHasta) {
      httpParams = httpParams.set('fechaHasta', params.fechaHasta);
    }

    return this.http.get<PageResponse<PagoDTO>>(this.baseUrl, { params: httpParams });
  }

  calcularTotal(request: CalcularPagoRequest) {
    return this.http.post<number>(`${this.baseUrl}/total`, request);
  }
}
