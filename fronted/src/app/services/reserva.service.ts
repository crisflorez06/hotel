import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { ReservaCalendarioDTO } from '../models/reserva-calendario.model';
import { TipoUnidad } from '../models/enums';

@Injectable({
  providedIn: 'root',
})
export class ReservaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reservas`;

  obtenerReservasCalendario(mes: string, tipoUnidad?: TipoUnidad, codigoUnidad?: string) {
    return this.http.get<ReservaCalendarioDTO[]>(`${this.baseUrl}/calendario`, {
      params: {
        mes,
        ...(tipoUnidad ? { tipoUnidad } : {}),
        ...(codigoUnidad ? { codigoUnidad } : {}),
      },
    });
  }
}
