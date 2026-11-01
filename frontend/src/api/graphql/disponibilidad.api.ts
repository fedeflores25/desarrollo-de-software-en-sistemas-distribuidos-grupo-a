import { ApiError } from '../rest/http';
import { API_CONFIG } from '../../config/api';
import { obtenerSesion } from '../auth';
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
      id
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

// Se agregan los segundos cuando el campo de fecha no los incluye.
function fechaConSegundos(valor: string): string {
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(valor)
    ? `${valor}:00`
    : valor;
}

export async function buscarDisponibilidad(
  filtro: DisponibilidadFiltro,
): Promise<VehiculoDisponible[]> {
  const sesion = obtenerSesion();

  const response = await fetch(API_CONFIG.graphql.endpoint, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...(sesion?.token && { Authorization: `Bearer ${sesion.token}` }),
    },
    body: JSON.stringify({
      query: DISPONIBILIDAD_QUERY,
      variables: {
        filtro: {
          fecha_inicio: fechaConSegundos(filtro.fechaInicio),
          fecha_fin: fechaConSegundos(filtro.fechaFin),
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
