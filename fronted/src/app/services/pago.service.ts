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
  idPagoReserva?: number;
  idPagoEstancia?: number;
}

export interface BuscarPagosParams {
  page?: number;
  size?: number;
  sort?: string[];
  estados?: EstadoPago[];
  mediosPago?: MedioPago[];
  tipoPago?: TipoPago;
  codigoReserva?: string;
  codigoEstancia?: string;
  fechaDesde?: string;
  fechaHasta?: string;
  pageable?: {
    page: number;
    size: number;
    sort?: string[];
  };
}

@Injectable({
  providedIn: 'root',
})
export class PagoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/pagos`;

  buscarPagos(params?: BuscarPagosParams) {
    const page = params?.pageable?.page ?? params?.page ?? 0;
    const size = params?.pageable?.size ?? params?.size ?? 10;
    const sort = params?.pageable?.sort ?? params?.sort;

    let httpParams = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (sort?.length) {
      sort.forEach((item) => {
        httpParams = httpParams.append('sort', item);
      });
    }

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

    if (params?.codigoReserva) {
      httpParams = httpParams.set('codigoReserva', params.codigoReserva);
    }

    if (params?.codigoEstancia) {
      httpParams = httpParams.set('codigoEstancia', params.codigoEstancia);
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
