import type { TipoVehiculo } from './vehiculo';

export interface DisponibilidadFiltro {
  fechaInicio: string;
  fechaFin: string;
  tipo?: TipoVehiculo;
  marca?: string;
  modelo?: string;
  precioMin?: number;
  precioMax?: number;
}

export interface VehiculoDisponible {
  id: number;
  patente: string;
  marca: string;
  modelo: string;
  anio: number;
  color: string | null;
  tipo: TipoVehiculo;
  precioDiario: number;
}
