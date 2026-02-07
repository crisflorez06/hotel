import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../../environments/environment';
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
}
