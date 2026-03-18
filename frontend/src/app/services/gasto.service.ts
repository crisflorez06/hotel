import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { GastoDTO, GastoRequest } from '../models/gasto.model';

@Injectable({
  providedIn: 'root',
})
export class GastoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/gastos`;

  listar() {
    return this.http.get<GastoDTO[]>(this.baseUrl);
  }

  crear(request: GastoRequest) {
    return this.http.post<GastoDTO>(this.baseUrl, request);
  }

  actualizar(id: number, request: GastoRequest) {
    return this.http.put<GastoDTO>(`${this.baseUrl}/${id}`, request);
  }

  eliminar(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
