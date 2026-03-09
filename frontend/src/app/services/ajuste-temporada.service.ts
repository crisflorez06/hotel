import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { Temporada } from '../models/enums';

@Injectable({
  providedIn: 'root',
})
export class AjusteTemporadaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/ajustes/temporada`;

  cambiarTemporada(temporada: Temporada) {
    return this.http.put<void>(this.baseUrl, null, {
      params: { temporada },
    });
  }

  obtenerTemporadaActiva() {
    return this.http.get<Temporada>(`${this.baseUrl}/activa`);
  }
}
