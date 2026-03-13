import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { PageResponse } from '../models/page.model';
import { TipoDocumento } from '../models/enums';
import { ClienteTablaItem } from '../models/cliente-tabla.model';
import { OcupanteDTO, OcupanteNuevoRequest } from '../models/ocupante.model';

export interface BuscarClientesTablaParams {
  nombre?: string;
  apellido?: string;
  tipoDocumento?: TipoDocumento;
  numeroDocumento?: string;
  telefono?: string;
  email?: string;
  pageable?: {
    page: number;
    size: number;
    sort?: string[];
  };
}

@Injectable({
  providedIn: 'root',
})
export class OcupanteService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/ocupantes`;

  crearOcupante(request: OcupanteNuevoRequest) {
    return this.http.post<OcupanteDTO>(this.baseUrl, request);
  }

  editarOcupante(id: number, request: OcupanteNuevoRequest) {
    return this.http.put<OcupanteDTO>(`${this.baseUrl}/${id}`, request);
  }

  buscarPorDocumento(documento: string) {
    return this.http.get<OcupanteDTO[]>(`${this.baseUrl}/buscar`, {
      params: { documento },
    });
  }

  buscarTablaClientes(params?: BuscarClientesTablaParams) {
    const page = params?.pageable?.page ?? 0;
    const size = params?.pageable?.size ?? 20;
    const sort = params?.pageable?.sort;

    let httpParams = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (sort?.length) {
      sort.forEach((item) => {
        httpParams = httpParams.append('sort', item);
      });
    }

    httpParams = this.setParamIfValue(httpParams, 'nombre', params?.nombre);
    httpParams = this.setParamIfValue(httpParams, 'apellido', params?.apellido);
    httpParams = this.setParamIfValue(httpParams, 'tipoDocumento', params?.tipoDocumento);
    httpParams = this.setParamIfValue(httpParams, 'numeroDocumento', params?.numeroDocumento);
    httpParams = this.setParamIfValue(httpParams, 'telefono', params?.telefono);
    httpParams = this.setParamIfValue(httpParams, 'email', params?.email);

    return this.http.get<PageResponse<ClienteTablaItem>>(`${this.baseUrl}/tabla-clientes`, {
      params: httpParams,
    });
  }

  private setParamIfValue(
    params: HttpParams,
    key: string,
    value: string | undefined
  ): HttpParams {
    if (value === undefined || value === null) {
      return params;
    }

    const valueTrimmed = value.trim();
    return valueTrimmed ? params.set(key, valueTrimmed) : params;
  }
}
