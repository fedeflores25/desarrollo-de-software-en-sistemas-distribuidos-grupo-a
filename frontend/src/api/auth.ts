import { API_CONFIG } from '../config/api';

export type RolUsuario = 'ADMIN' | 'CLIENTE';

export interface Sesion {
  token: string;
  usuario: { username: string; rol: RolUsuario; clienteId: number | null };
}

const SESSION_KEY = 'rentar.sesion';

export function obtenerSesion(): Sesion | null {
  const value = localStorage.getItem(SESSION_KEY);
  if (!value) return null;
  try { return JSON.parse(value) as Sesion; } catch { return null; }
}

export async function iniciarSesion(username: string, password: string): Promise<Sesion> {
  const response = await fetch(`${API_CONFIG.rest.baseUrl}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const payload = await response.json() as Sesion | { error: string };
  if (!response.ok || !('token' in payload)) {
    throw new Error('error' in payload ? payload.error : 'No fue posible iniciar sesion.');
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(payload));
  return payload;
}

export function cerrarSesion(): void {
  localStorage.removeItem(SESSION_KEY);
}
