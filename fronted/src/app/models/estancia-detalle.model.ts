import { ModoOcupacion } from './enums';
import { OcupanteDTO } from './ocupante.model';
import { PagoDTO } from './pago-detalle.model';

export interface EstanciaDTO {
  id: number;
  codigoFolio: string;
  fechaCreacion: string;
  entradaReal: string;
  salidaEstimada: string;
  modoOcupacion: ModoOcupacion;
  ocupantes: OcupanteDTO[];
  notas: string | null;
  pagoReserva?: PagoDTO | null;
  pagoEstancia?: PagoDTO | null;
}
