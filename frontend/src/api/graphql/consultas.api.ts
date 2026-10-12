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
  query ConsultarReservas($filtro: ReservaFiltroInput) {
    reservas(filtro: $filtro) {
      id
      cliente
      vehiculo
      patente
      fechaInicio: fecha_inicio
      fechaFinalizacion: fecha_fin
      precioDiario: precio_diario
      importeTotal: importe_total
      estado
    }
  }
`;

const HISTORIAL_QUERY = `
  query ConsultarHistorialAlquileres($clienteId: ID!) {
    historial(cliente_id: $clienteId) {
      vehiculo
      patente
      fechaInicio: fecha_inicio
      fechaFinalizacion: fecha_fin
      cantidadDias: cantidad_dias
      importeTotal: importe_total
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
  const data = await graphqlRequest<{ reservas: ConsultaReserva[] }>(
    CONSULTAR_RESERVAS_QUERY,
    {
      filtro: {
        ...(filtro.clienteId && { cliente_id: filtro.clienteId }),
        ...(filtro.vehiculoId && { vehiculo_id: filtro.vehiculoId }),
        ...(filtro.tipoVehiculo && { tipo_vehiculo: filtro.tipoVehiculo }),
        ...(filtro.estado && { estado: filtro.estado }),
        ...(filtro.fechaInicio && { desde: filtro.fechaInicio }),
        ...(filtro.fechaFin && { hasta: filtro.fechaFin }),
      },
    },
  );

  return data.reservas;
}

export async function consultarHistorialAlquileres(clienteId: number): Promise<HistorialAlquiler[]> {
  const data = await graphqlRequest<{ historial: HistorialAlquiler[] }>(
    HISTORIAL_QUERY,
    { clienteId },
  );

  return data.historial;
}
