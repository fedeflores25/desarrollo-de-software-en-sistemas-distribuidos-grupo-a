import { API_CONFIG } from '../../config/api';
import type { SolicitudVehiculo, SolicitudVehiculoRequest } from '../../types/solicitudVehiculo';
import { request } from './http';

const SOLICITUDES_URL = `${API_CONFIG.rest.baseUrl}/vehiculos/solicitudes-externas`;

interface SolicitudGatewayResponse {
  id: number;
  cliente_id: number;
  patente: string;
  marca: string;
  modelo: string;
  estado: SolicitudVehiculo['estado'];
  resultado_patente: SolicitudVehiculo['resultadoPatente'];
  resultado_titularidad: SolicitudVehiculo['resultadoTitularidad'];
  vehiculo_id?: number;
}

function toSolicitud(response: SolicitudGatewayResponse): SolicitudVehiculo {
  return {
    id: Number(response.id),
    clienteId: Number(response.cliente_id),
    patente: response.patente,
    marca: response.marca,
    modelo: response.modelo,
    estado: response.estado,
    resultadoPatente: response.resultado_patente || '',
    resultadoTitularidad: response.resultado_titularidad || '',
    vehiculoId: response.vehiculo_id ? Number(response.vehiculo_id) : null,
  };
}

export function crearSolicitudVehiculo(data: SolicitudVehiculoRequest): Promise<SolicitudVehiculo> {
  return request<SolicitudGatewayResponse>(SOLICITUDES_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      documento: data.documento,
      patente: data.patente.trim().toUpperCase(),
      marca: data.marca,
      modelo: data.modelo,
      anio: data.anio,
      color: data.color ?? '',
      tipo: data.tipo,
      precio_diario: data.precioDiario,
    }),
  }).then(toSolicitud);
}

export function consultarSolicitudVehiculo(id: number): Promise<SolicitudVehiculo> {
  return request<SolicitudGatewayResponse>(`${SOLICITUDES_URL}/${id}`).then(toSolicitud);
}
