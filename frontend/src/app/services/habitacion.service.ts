import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { EstadoOperativo, Piso } from '../models/enums';
import { HabitacionDTO } from '../models/habitacion.model';

@Injectable({
  providedIn: 'root',
})
export class HabitacionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/recepcion/habitaciones`;

  buscarHabitaciones(params?: {
    estados?: EstadoOperativo[];
    pisos?: Piso[];
    codigo?: string;
  }) {
    let httpParams = new HttpParams();

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

    return this.http.get<HabitacionDTO[]>(this.baseUrl, { params: httpParams });
  }
}
