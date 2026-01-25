export type TipoUnidad = 'APARTAMENTO' | 'APARTAESTUDIO' | 'HABITACION';

export type TipoPago = 'RESERVA' | 'ESTANCIA';

export type MedioPago =
  | 'EFECTIVO'
  | 'TARJETA_CREDITO'
  | 'TARJETA_DEBITO'
  | 'TRANSFERENCIA_BANCARIA'
  | 'PLATAFORMA';

export type EstadoPago = 'PENDIENTE' | 'COMPLETADO' | 'FALLIDO' | 'REEMBOLSADO';

export type EstadoOperativo = 'DISPONIBLE' | 'OCUPADO' | 'RESERVADO' | 'PARCIALMENTE';

export type Piso = 'PISO_1' | 'PISO_2' | 'PISO_3';

export type ModoOcupacion = 'COMPLETO' | 'INDIVIDUAL';
