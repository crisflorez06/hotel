import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { OcupanteDTO, OcupanteNuevoRequest } from '../models/ocupante.model';

@Injectable({
  providedIn: 'root',
})
export class OcupanteService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/ocupantes`;

  crearOcupante(request: OcupanteNuevoRequest) {
    return this.http.post<OcupanteDTO>(this.baseUrl, request);
  }

  buscarPorDocumento(documento: string) {
    return this.http.get<OcupanteDTO[]>(`${this.baseUrl}/buscar`, {
      params: { documento },
    });
  }
}
