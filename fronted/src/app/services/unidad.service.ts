import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { EstadoOperativo, Piso, TipoUnidad } from '../models/enums';
import { UnidadDTO } from '../models/unidad.model';

@Injectable({
  providedIn: 'root',
})
export class UnidadService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/recepcion/unidades`;

  buscarUnidades(params?: {
    tipo?: TipoUnidad;
    estados?: EstadoOperativo[];
    pisos?: Piso[];
    codigo?: string;
  }) {
    let httpParams = new HttpParams();

    if (params?.tipo) {
      httpParams = httpParams.set('tipo', params.tipo);
    }

    if (params?.estados?.length) {
      params.estados.forEach((estado) => {
        httpParams = httpParams.append('estado', estado);
      });
    }

    if (params?.pisos?.length) {
      params.pisos.forEach((piso) => {
        httpParams = httpParams.append('piso', piso);
      });
    }

    if (params?.codigo) {
      httpParams = httpParams.set('codigo', params.codigo);
    }

    return this.http.get<UnidadDTO[]>(this.baseUrl, { params: httpParams });
  }
}
