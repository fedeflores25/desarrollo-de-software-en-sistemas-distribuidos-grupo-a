import type { EstadoReserva } from './reserva';
import type { TipoVehiculo } from './vehiculo';

export interface ReservaConsultaFiltro {
  clienteId?: number;
  vehiculoId?: number;
  tipoVehiculo?: TipoVehiculo;
  estado?: EstadoReserva;
  fechaInicio?: string;
  fechaFin?: string;
}

export interface ConsultaReserva {
  cliente: string;
  vehiculo: string;
  patente: string;
  fechaInicio: string;
  fechaFinalizacion: string;
  precioDiario: number;
  importeTotal: number;
  estado: EstadoReserva;
}

export interface HistorialAlquiler {
  vehiculo: string;
  patente: string;
  fechaInicio: string;
  fechaFinalizacion: string;
  cantidadDias: number;
  importeTotal: number;
  estado: EstadoReserva;
}
