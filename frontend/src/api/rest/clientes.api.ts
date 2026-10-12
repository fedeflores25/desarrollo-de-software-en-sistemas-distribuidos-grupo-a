import { API_CONFIG } from '../../config/api';
import type { Cliente, ClienteRequest } from '../../types/cliente';
import { request } from './http';

const CLIENTES_URL = `${API_CONFIG.rest.baseUrl}/clientes`;

interface ClienteGatewayResponse {
  id: number;
  documento: string;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
  fecha_nacimiento: string | null;
  activo: boolean;
}

function toCliente(response: ClienteGatewayResponse): Cliente {
  return {
    id: Number(response.id),
    documento: response.documento,
    nombre: response.nombre,
    apellido: response.apellido,
    email: response.email,
    telefono: response.telefono,
    fechaNacimiento: response.fecha_nacimiento,
    activo: response.activo,
  };
}

function toClientePayload(data: ClienteRequest) {
  return {
    documento: data.documento,
    nombre: data.nombre,
    apellido: data.apellido,
    email: data.email,
    telefono: data.telefono ?? '',
    fecha_nacimiento: data.fechaNacimiento ?? '',
  };
}

export function listarClientes(): Promise<Cliente[]> {
  return request<ClienteGatewayResponse[]>(CLIENTES_URL).then((customers) => customers.map(toCliente));
}

export function buscarClientePorId(id: number): Promise<Cliente> {
  return request<ClienteGatewayResponse>(`${CLIENTES_URL}/${id}`).then(toCliente);
}

export function crearCliente(data: ClienteRequest): Promise<Cliente> {
  return request<ClienteGatewayResponse>(CLIENTES_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(toClientePayload(data)),
  }).then(toCliente);
}

export function actualizarCliente(id: number, data: ClienteRequest): Promise<Cliente> {
  return request<ClienteGatewayResponse>(`${CLIENTES_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(toClientePayload(data)),
  }).then(toCliente);
}

export function darDeBajaCliente(id: number): Promise<Cliente> {
  return request<ClienteGatewayResponse>(`${CLIENTES_URL}/${id}`, { method: 'DELETE' }).then(toCliente);
}
