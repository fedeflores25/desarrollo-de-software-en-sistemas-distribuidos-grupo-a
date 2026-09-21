export type TipoVehiculo = 'SEDAN' | 'SUV' | 'PICKUP' | 'COUPE' | 'HATCHBACK';

export type EstadoVehiculo = 'DISPONIBLE' | 'RESERVADO' | 'EN_ALQUILER';

export interface Vehiculo {
  id: number;
  patente: string;
  marca: string;
  modelo: string;
  anio: number;
  color: string | null;
  tipo: TipoVehiculo;
  precioDiario: number;
  estado: EstadoVehiculo;
  activo: boolean;
}

export interface CrearVehiculoRequest {
  patente: string;
  marca: string;
  modelo: string;
  anio: number;
  color?: string;
  tipo: TipoVehiculo;
  precioDiario: number;
}

export interface ActualizarVehiculoRequest {
  marca: string;
  modelo: string;
  anio: number;
  color?: string;
  tipo: TipoVehiculo;
  precioDiario: number;
  estado: EstadoVehiculo;
}
