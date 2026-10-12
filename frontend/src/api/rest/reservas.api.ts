import { API_CONFIG } from '../../config/api';
import type { Reserva, ReservaRequest } from '../../types/reserva';
import { request } from './http';

const RESERVAS_URL = `${API_CONFIG.rest.baseUrl}/reservas`;

interface ReservaGatewayResponse {
  id: number;
  cliente_id: number;
  vehiculo_id: number;
  patente?: string;
  fecha_inicio: string;
  fecha_fin: string;
  precio_diario: number;
  cantidad_dias: number;
  importe_total: number;
  estado: Reserva['estado'];
  fecha_alta: string;
  fecha_cancelacion?: string;
}

function toReserva(response: ReservaGatewayResponse): Reserva {
  return {
    id: Number(response.id),
    clienteId: Number(response.cliente_id),
    vehiculoId: Number(response.vehiculo_id),
    patente: response.patente ?? '',
    fechaInicio: response.fecha_inicio,
    fechaFin: response.fecha_fin,
    precioDiario: response.precio_diario,
    cantidadDias: response.cantidad_dias,
    importeTotal: response.importe_total,
    estado: response.estado,
    fechaAlta: response.fecha_alta,
    fechaCancelacion: response.fecha_cancelacion || null,
  };
}

export function crearReserva(data: ReservaRequest): Promise<Reserva> {
  return request<ReservaGatewayResponse>(RESERVAS_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      cliente_id: data.clienteId,
      vehiculo_id: data.vehiculoId,
      fecha_inicio: data.fechaInicio,
      fecha_fin: data.fechaFin,
    }),
  }).then(toReserva);
}

export function cancelarReserva(id: number): Promise<Reserva> {
  return request<ReservaGatewayResponse>(`${RESERVAS_URL}/${id}/cancelar`, {
    method: 'PATCH',
  }).then(toReserva);
}
