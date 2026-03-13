import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { TarifaBaseActualizacionDTO, TarifaBaseDTO } from '../models/tarifa-base.model';

@Injectable({
  providedIn: 'root',
})
export class TarifaBaseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/ajustes/tarifas-base`;

  listarTarifasBase() {
    return this.http.get<TarifaBaseDTO[]>(this.baseUrl);
  }

  actualizarTarifaBase(actualizacion: TarifaBaseActualizacionDTO) {
    return this.http.put<TarifaBaseDTO[]>(this.baseUrl, actualizacion);
  }
}
