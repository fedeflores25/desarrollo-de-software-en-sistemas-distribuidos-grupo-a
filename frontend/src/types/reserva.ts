export type EstadoReserva = 'CONFIRMADA' | 'CANCELADA' | 'FINALIZADA';

export interface ReservaRequest {
  clienteId: number;
  vehiculoId: number;
  fechaInicio: string;
  fechaFin: string;
}

export interface Reserva {
  id: number;
  clienteId: number;
  vehiculoId: number;
  patente: string;
  fechaInicio: string;
  fechaFin: string;
  precioDiario: number;
  cantidadDias: number;
  importeTotal: number;
  estado: EstadoReserva;
  fechaAlta: string;
  fechaCancelacion: string | null;
}
