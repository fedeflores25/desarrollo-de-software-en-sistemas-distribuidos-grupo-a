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
  query VehiculosDisponibles($filtro: DisponibilidadFiltroInput!) {
    vehiculosDisponibles(filtro: $filtro) {
      patente
      marca
      modelo
      anio
      color
      tipo
      precioDiario: precio_diario
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
    body: JSON.stringify({
      query: DISPONIBILIDAD_QUERY,
      variables: {
        filtro: {
          fecha_inicio: filtro.fechaInicio,
          fecha_fin: filtro.fechaFin,
          ...(filtro.tipo && { tipo: filtro.tipo }),
          ...(filtro.marca && { marca: filtro.marca }),
          ...(filtro.modelo && { modelo: filtro.modelo }),
          ...(filtro.precioMin !== undefined && { precio_min: filtro.precioMin }),
          ...(filtro.precioMax !== undefined && { precio_max: filtro.precioMax }),
        },
      },
    }),
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
