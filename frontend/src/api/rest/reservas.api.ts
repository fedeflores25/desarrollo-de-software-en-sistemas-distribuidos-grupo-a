import { API_CONFIG } from '../../config/api';
import type { Reserva, ReservaRequest } from '../../types/reserva';
import { request } from './http';

const RESERVAS_URL = `${API_CONFIG.rest.baseUrl}/reservas`;

export function crearReserva(data: ReservaRequest): Promise<Reserva> {
  return request<Reserva>(RESERVAS_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export function cancelarReserva(id: number): Promise<Reserva> {
  return request<Reserva>(`${RESERVAS_URL}/${id}/cancelacion`, { method: 'POST' });
}
