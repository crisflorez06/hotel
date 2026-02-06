import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../../environments/environment';
import {
  EstanciaActivarRequest,
  EstanciaEditarRequest,
  EstanciaNuevoRequest,
  SalidaEstanciaRequest,
} from '../models/estancia.model';
import { EstanciaDTO } from '../models/estancia-detalle.model';
import { TipoUnidad } from '../models/enums';

@Injectable({
  providedIn: 'root',
})
export class EstanciaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/estancias`;

  crearEstancia(request: EstanciaNuevoRequest) {
    return this.http.post<void>(this.baseUrl, request);
  }

  activarEstancia(request: EstanciaActivarRequest) {
    return this.http.put<void>(`${this.baseUrl}/activar`, request);
  }

  editarEstancia(id: number, request: EstanciaEditarRequest) {
    return this.http.put<void>(`${this.baseUrl}/${id}`, request);
  }

  eliminarEstancia(id: number) {
    return this.http.put<void>(`${this.baseUrl}/eliminar/${id}`, {});
  }

  finalizarEstancia(request: SalidaEstanciaRequest) {
    return this.http.put<void>(`${this.baseUrl}/finalizar`, request);
  }

  obtenerEstanciaActiva(codigo: string, tipoUnidad: TipoUnidad) {
    return this.http.get<EstanciaDTO>(`${this.baseUrl}/activa`, {
      params: {
        codigo,
        tipoUnidad,
      },
    });
  }
}
