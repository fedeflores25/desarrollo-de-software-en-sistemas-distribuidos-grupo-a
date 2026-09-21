import { API_CONFIG } from '../../config/api';
import type {
  ConsultaReserva,
  HistorialAlquiler,
  ReservaConsultaFiltro,
} from '../../types/consultas';
import { ApiError } from '../rest/http';

interface GraphQLResponse<T> {
  data?: T;
  errors?: Array<{ message: string }>;
}

const CONSULTAR_RESERVAS_QUERY = `
  query ConsultarReservas($filtro: ReservaFiltro) {
    consultarReservas(filtro: $filtro) {
      cliente
      vehiculo
      patente
      fechaInicio
      fechaFinalizacion
      precioDiario
      importeTotal
      estado
    }
  }
`;

const HISTORIAL_QUERY = `
  query ConsultarHistorialAlquileres {
    consultarHistorialAlquileres {
      vehiculo
      patente
      fechaInicio
      fechaFinalizacion
      cantidadDias
      importeTotal
      estado
    }
  }
`;

async function graphqlRequest<T>(query: string, variables?: Record<string, unknown>): Promise<T> {
  const response = await fetch(API_CONFIG.graphql.endpoint, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query, variables }),
  });

  const payload = await response.json() as GraphQLResponse<T>;

  if (!response.ok || payload.errors?.length) {
    throw new ApiError(
      payload.errors?.[0]?.message ?? 'No fue posible realizar la consulta.',
      response.status,
    );
  }

  if (!payload.data) {
    throw new ApiError('La consulta no devolvio datos.', response.status);
  }

  return payload.data;
}

export async function consultarReservas(
  filtro: ReservaConsultaFiltro,
): Promise<ConsultaReserva[]> {
  const data = await graphqlRequest<{ consultarReservas: ConsultaReserva[] }>(
    CONSULTAR_RESERVAS_QUERY,
    { filtro },
  );

  return data.consultarReservas;
}

export async function consultarHistorialAlquileres(): Promise<HistorialAlquiler[]> {
  const data = await graphqlRequest<{ consultarHistorialAlquileres: HistorialAlquiler[] }>(
    HISTORIAL_QUERY,
  );

  return data.consultarHistorialAlquileres;
}
