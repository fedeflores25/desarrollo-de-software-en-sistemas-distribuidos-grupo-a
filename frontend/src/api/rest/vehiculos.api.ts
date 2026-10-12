import { API_CONFIG } from '../../config/api';
import type {
  ActualizarVehiculoRequest,
  CrearVehiculoRequest,
  Vehiculo,
} from '../../types/vehiculo';
import { request } from './http';

const VEHICULOS_URL = `${API_CONFIG.rest.baseUrl}/vehiculos`;

interface VehiculoGatewayResponse {
  id: number;
  patente: string;
  marca: string;
  modelo: string;
  anio: number;
  color: string | null;
  tipo: Vehiculo['tipo'];
  precio_diario: number;
  estado: Vehiculo['estado'];
  activo: boolean;
}

function toVehiculo(response: VehiculoGatewayResponse): Vehiculo {
  return {
    id: Number(response.id),
    patente: response.patente,
    marca: response.marca,
    modelo: response.modelo,
    anio: response.anio,
    color: response.color,
    tipo: response.tipo,
    precioDiario: response.precio_diario,
    estado: response.estado,
    activo: response.activo,
  };
}

function toVehiculoPayload(data: CrearVehiculoRequest | ActualizarVehiculoRequest) {
  return {
    ...("patente" in data && { patente: data.patente }),
    marca: data.marca,
    modelo: data.modelo,
    anio: data.anio,
    color: data.color ?? '',
    tipo: data.tipo,
    precio_diario: data.precioDiario,
  };
}

export function listarVehiculos(): Promise<Vehiculo[]> {
  return request<VehiculoGatewayResponse[]>(VEHICULOS_URL).then((vehicles) => vehicles.map(toVehiculo));
}

export function buscarVehiculoPorId(id: number): Promise<Vehiculo> {
  return request<VehiculoGatewayResponse>(`${VEHICULOS_URL}/${id}`).then(toVehiculo);
}

export function buscarVehiculoPorPatente(patente: string): Promise<Vehiculo> {
  return request<VehiculoGatewayResponse>(
    `${VEHICULOS_URL}/patente/${encodeURIComponent(patente.trim().toUpperCase())}`,
  ).then(toVehiculo);
}

export function crearVehiculo(data: CrearVehiculoRequest): Promise<Vehiculo> {
  return request<VehiculoGatewayResponse>(VEHICULOS_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(toVehiculoPayload(data)),
  }).then(toVehiculo);
}

export function actualizarVehiculo(
  id: number,
  data: ActualizarVehiculoRequest,
): Promise<Vehiculo> {
  return request<VehiculoGatewayResponse>(`${VEHICULOS_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(toVehiculoPayload(data)),
  }).then(toVehiculo);
}

export function actualizarEstadoVehiculo(
  id: number,
  estado: Vehiculo['estado'],
): Promise<Vehiculo> {
  return request<VehiculoGatewayResponse>(`${VEHICULOS_URL}/${id}/estado`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ estado }),
  }).then(toVehiculo);
}

export function darDeBajaVehiculo(id: number): Promise<Vehiculo> {
  return request<VehiculoGatewayResponse>(`${VEHICULOS_URL}/${id}`, { method: 'DELETE' })
    .then(toVehiculo);
}
