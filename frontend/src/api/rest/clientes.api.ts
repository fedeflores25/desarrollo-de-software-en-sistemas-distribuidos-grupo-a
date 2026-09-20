import { API_CONFIG } from '../../config/api';
import type { Cliente, ClienteRequest } from '../../types/cliente';
import { request } from './http';

const CLIENTES_URL = `${API_CONFIG.rest.baseUrl}/clientes`;

export function listarClientes(): Promise<Cliente[]> {
  return request<Cliente[]>(CLIENTES_URL);
}

export function buscarClientePorId(id: number): Promise<Cliente> {
  return request<Cliente>(`${CLIENTES_URL}/${id}`);
}

export function crearCliente(data: ClienteRequest): Promise<Cliente> {
  return request<Cliente>(CLIENTES_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export function actualizarCliente(id: number, data: ClienteRequest): Promise<Cliente> {
  return request<Cliente>(`${CLIENTES_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export function darDeBajaCliente(id: number): Promise<void> {
  return request<void>(`${CLIENTES_URL}/${id}`, { method: 'DELETE' });
}
