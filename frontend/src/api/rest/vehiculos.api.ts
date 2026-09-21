import { API_CONFIG } from '../../config/api';
import type {
  ActualizarVehiculoRequest,
  CrearVehiculoRequest,
  Vehiculo,
} from '../../types/vehiculo';
import { request } from './http';

const VEHICULOS_URL = `${API_CONFIG.rest.baseUrl}/vehiculos`;

export function listarVehiculos(): Promise<Vehiculo[]> {
  return request<Vehiculo[]>(VEHICULOS_URL);
}

export function buscarVehiculoPorId(id: number): Promise<Vehiculo> {
  return request<Vehiculo>(`${VEHICULOS_URL}/${id}`);
}

export function crearVehiculo(data: CrearVehiculoRequest): Promise<Vehiculo> {
  return request<Vehiculo>(VEHICULOS_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export function actualizarVehiculo(
  id: number,
  data: ActualizarVehiculoRequest,
): Promise<Vehiculo> {
  return request<Vehiculo>(`${VEHICULOS_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export function darDeBajaVehiculo(id: number): Promise<void> {
  return request<void>(`${VEHICULOS_URL}/${id}`, { method: 'DELETE' });
}
