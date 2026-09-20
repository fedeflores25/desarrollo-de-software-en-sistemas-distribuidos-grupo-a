export interface Cliente {
  id: number;
  documento: string;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
  fechaNacimiento: string | null;
  activo: boolean;
}

export interface ClienteRequest {
  documento: string;
  nombre: string;
  apellido: string;
  email: string;
  telefono?: string;
  fechaNacimiento?: string;
}
