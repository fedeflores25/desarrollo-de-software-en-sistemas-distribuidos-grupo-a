import { ApiError } from '../rest/http';
import { API_CONFIG } from '../../config/api';
import type { DisponibilidadFiltro, VehiculoDisponible } from '../../types/disponibilidad';

interface GraphQLResponse<T> {
  data?: T;
  errors?: Array<{ message: string }>;
}

interface DisponibilidadData {
  vehiculosDisponibles: VehiculoDisponible[];
}

const DISPONIBILIDAD_QUERY = `
  query VehiculosDisponibles($filtro: DisponibilidadFiltro!) {
    vehiculosDisponibles(filtro: $filtro) {
      patente
      marca
      modelo
      anio
      color
      tipo
      precioDiario
    }
  }
`;

export async function buscarDisponibilidad(
  filtro: DisponibilidadFiltro,
): Promise<VehiculoDisponible[]> {
  const response = await fetch(API_CONFIG.graphql.endpoint, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query: DISPONIBILIDAD_QUERY, variables: { filtro } }),
  });

  const payload = await response.json() as GraphQLResponse<DisponibilidadData>;

  if (!response.ok || payload.errors?.length) {
    throw new ApiError(
      payload.errors?.[0]?.message ?? 'No fue posible consultar la disponibilidad.',
      response.status,
    );
  }

  return payload.data?.vehiculosDisponibles ?? [];
}
