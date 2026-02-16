import { TipoUnidad } from './enums';

export interface TarifaBaseDTO {
  tipoUnidad: TipoUnidad;
  precioDiaTemBaja: number;
  precioDiaTemAlta: number;
  precioEstadiaCorta: number;
  precioPersonaAdicionalTemBaja: number;
  precioPersonaAdicionalTemAlta: number;
  precioEstadiaPersonaAdicionalCorta: number;
}

export interface TarifaBaseActualizacionDTO {
  tipoUnidad: TipoUnidad;
  precioDiaTemBaja: number;
  precioDiaTemAlta: number;
  precioEstadiaCorta: number;
  precioPersonaAdicionalTemBaja: number;
  precioPersonaAdicionalTemAlta: number;
  precioEstadiaPersonaAdicionalCorta: number;
}
