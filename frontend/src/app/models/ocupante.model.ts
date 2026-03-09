import { TipoDocumento, TipoOcupante } from './enums';

export interface OcupanteNuevoRequest {
  nombres: string;
  apellidos: string;
  tipoDocumento?: TipoDocumento;
  numeroDocumento?: string;
  telefono?: string;
  email?: string;
  tipoOcupante: TipoOcupante;
}

export interface OcupanteDTO {
  id: number;
  nombres: string;
  apellidos: string;
  tipoDocumento?: TipoDocumento;
  numeroDocumento?: string;
  telefono?: string;
  email?: string;
  tipoOcupante: TipoOcupante;
  creadoEn?: string;
}
