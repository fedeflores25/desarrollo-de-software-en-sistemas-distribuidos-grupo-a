import type { TipoVehiculo } from './vehiculo';

export type EstadoSolicitudVehiculo = 'PENDIENTE' | 'INCORPORADA' | 'RECHAZADA';
export type ResultadoValidacion = 'VALIDADO' | 'RECHAZADO' | '';

export interface SolicitudVehiculoRequest {
  documento: string;
  patente: string;
  marca: string;
  modelo: string;
  anio: number;
  color?: string;
  tipo: TipoVehiculo;
  precioDiario: number;
}

export interface SolicitudVehiculo {
  id: number;
  clienteId: number;
  patente: string;
  marca: string;
  modelo: string;
  estado: EstadoSolicitudVehiculo;
  resultadoPatente: ResultadoValidacion;
  resultadoTitularidad: ResultadoValidacion;
  vehiculoId: number | null;
}
