import { EstadoEstancia, ModoOcupacion } from './enums';
import { OcupanteDTO } from './ocupante.model';
import { PagoDTO } from './pago-detalle.model';
import { UnidadDTO } from './unidad.model';
import { ReservaCalendarioDTO } from './reserva-calendario.model';

export interface EstanciaDTO {
  id: number;
  codigoFolio: string;
  codigoReserva?: string | null;
  idReserva?: number | null;
  entradaReal: string;
  salidaEstimada: string;
  salidaReal?: string | null;
  estado?: EstadoEstancia | null;
  modoOcupacion: ModoOcupacion;
  cliente?: OcupanteDTO | null;
  acompanantes?: OcupanteDTO[];
  notas: string | null;
  pagos?: PagoDTO[];
}

export interface DetalleEstanciaDTO {
  unidad: UnidadDTO | null;
  estancia: EstanciaDTO;
  reserva: ReservaCalendarioDTO | null;
}
