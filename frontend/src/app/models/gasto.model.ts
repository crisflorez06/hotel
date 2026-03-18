export interface GastoDTO {
  id: number;
  concepto: string;
  descripcion: string | null;
  monto: number;
  fecha: string;
  metodoPago: string;
  referencia: string | null;
  estado: string;
}

export interface GastoRequest {
  concepto: string;
  descripcion?: string;
  monto: number;
  fecha: string;
  metodoPago: string;
  referencia?: string;
  estado: string;
}
