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

export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | 'COMPLETADA' | 'EXPIRADA';

export type EstadoEstancia = 'ACTIVA' | 'RESERVADA' | 'FINALIZADA' | 'EXCEDIDA' | 'CANCELADA';

export type ModoOcupacion = 'COMPLETO' | 'INDIVIDUAL';

export type CanalReserva =
  | 'PLATAFORMA_AIRBINB'
  | 'PLATAFORMA_BOOKING'
  | 'PLATAFORMA_EXPEDIA'
  | 'WHATSAPP'
  | 'TELEFONO'
  | 'EMAIL'
  | 'MOSTRADOR';

export type TipoDocumento = 'CC' | 'TI' | 'CE' | 'PA' | 'NIT' | 'RC';

export type TipoOcupante = 'CLIENTE' | 'ACOMPANANTE';

export type Temporada = 'ALTA' | 'BAJA';
