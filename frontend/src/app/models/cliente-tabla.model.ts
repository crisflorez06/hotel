import { EstadoEstancia, EstadoReserva, TipoDocumento, TipoOcupante } from './enums';

export interface ClienteReservaResumen {
  id: number;
  codigoReserva: string;
  estado: EstadoReserva;
  entradaEstimada: string;
  salidaEstimada: string;
}

export interface ClienteEstanciaResumen {
  id: number;
  codigoEstancia: string;
  estado: EstadoEstancia;
  entradaReal: string;
  salidaEstimada: string;
  salidaReal: string | null;
}

export interface ClienteTablaItem {
  nombres: string;
  apellidos: string;
  tipoDocumento?: TipoDocumento;
  numeroDocumento?: string;
  telefono?: string;
  email?: string;
  tipoOcupante: TipoOcupante;
  reservas: ClienteReservaResumen[];
  estancias: ClienteEstanciaResumen[];
}

export interface ClienteTablaFiltros {
  nombre: string;
  apellido: string;
  tipoDocumento: TipoDocumento | '';
  numeroDocumento: string;
  telefono: string;
  email: string;
}
