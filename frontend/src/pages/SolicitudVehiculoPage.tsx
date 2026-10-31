import { useEffect, useState, type FormEvent } from 'react';
import { RefreshCw, Send } from 'lucide-react';
import { obtenerSesion } from '../api/auth';
import { listarClientes } from '../api/rest/clientes.api';
import {
  consultarSolicitudVehiculo,
  crearSolicitudVehiculo,
} from '../api/rest/solicitudes-vehiculos.api';
import { ApiError } from '../api/rest/http';
import type { Cliente } from '../types/cliente';
import type { SolicitudVehiculo } from '../types/solicitudVehiculo';
import type { TipoVehiculo } from '../types/vehiculo';

const tipos: TipoVehiculo[] = ['SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK'];

function estadoClase(estado: SolicitudVehiculo['estado']): string {
  return `status-${estado.toLowerCase()}`;
}

export function SolicitudVehiculoPage() {
  const sesion = obtenerSesion();
  const esAdmin = sesion?.usuario.rol === 'ADMIN';
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [clienteId, setClienteId] = useState('');
  const [documento, setDocumento] = useState('');
  const [patente, setPatente] = useState('');
  const [marca, setMarca] = useState('');
  const [modelo, setModelo] = useState('');
  const [anio, setAnio] = useState('');
  const [color, setColor] = useState('');
  const [tipo, setTipo] = useState<TipoVehiculo>('SEDAN');
  const [precioDiario, setPrecioDiario] = useState('');
  const [solicitud, setSolicitud] = useState<SolicitudVehiculo>();
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!esAdmin) return;

    void listarClientes()
      .then((resultado) => setClientes(resultado.filter((cliente) => cliente.activo)))
      .catch(() => setError('No fue posible cargar los clientes activos.'));
  }, [esAdmin]);

  async function registrarSolicitud(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCargando(true);
    setError('');

    try {
      const resultado = await crearSolicitudVehiculo({
        documento,
        patente,
        marca,
        modelo,
        anio: Number(anio),
        color,
        tipo,
        precioDiario: Number(precioDiario),
        ...(esAdmin && { clienteId: Number(clienteId) }),
      });
      setSolicitud(resultado);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible registrar la solicitud.');
    } finally {
      setCargando(false);
    }
  }

  async function actualizarEstado() {
    if (!solicitud) return;

    setCargando(true);
    setError('');
    try {
      setSolicitud(await consultarSolicitudVehiculo(solicitud.id));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible consultar la solicitud.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <section className="page-content" aria-labelledby="vehiculo-propio-title">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Incorporacion</p>
          <h2 id="vehiculo-propio-title">Solicitud de vehiculo propio</h2>
          <p>Registra los datos para iniciar las validaciones de patente y titularidad.</p>
        </div>
      </div>

      {error && <p className="feedback-message feedback-error" role="alert">{error}</p>}

      <section className="form-panel" aria-labelledby="solicitud-form-title">
        <div className="form-panel-heading">
          <h3 id="solicitud-form-title">Datos del vehiculo</h3>
          <p>La solicitud queda pendiente hasta completar las validaciones.</p>
        </div>
        <form className="form-grid" onSubmit={(event) => void registrarSolicitud(event)}>
          {esAdmin && (
            <label className="form-field">
              <span>Cliente</span>
              <select onChange={(event) => setClienteId(event.target.value)} required value={clienteId}>
                <option value="">Seleccionar cliente</option>
                {clientes.map((cliente) => (
                  <option key={cliente.id} value={cliente.id}>
                    {cliente.documento} - {cliente.nombre} {cliente.apellido}
                  </option>
                ))}
              </select>
            </label>
          )}
          <label className="form-field"><span>Documento del titular</span><input onChange={(event) => setDocumento(event.target.value)} required value={documento} /></label>
          <label className="form-field"><span>Patente</span><input onChange={(event) => setPatente(event.target.value.toUpperCase())} required value={patente} /></label>
          <label className="form-field"><span>Marca</span><input onChange={(event) => setMarca(event.target.value)} required value={marca} /></label>
          <label className="form-field"><span>Modelo</span><input onChange={(event) => setModelo(event.target.value)} required value={modelo} /></label>
          <label className="form-field"><span>Año</span><input min="1900" onChange={(event) => setAnio(event.target.value)} required type="number" value={anio} /></label>
          <label className="form-field"><span>Color</span><input onChange={(event) => setColor(event.target.value)} value={color} /></label>
          <label className="form-field"><span>Tipo</span><select onChange={(event) => setTipo(event.target.value as TipoVehiculo)} value={tipo}>{tipos.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>
          <label className="form-field"><span>Precio diario</span><input min="1" onChange={(event) => setPrecioDiario(event.target.value)} required type="number" value={precioDiario} /></label>
          <div className="form-actions">
            <button className="primary-button" disabled={cargando} type="submit">
              <Send aria-hidden="true" size={18} />
              {cargando ? 'Registrando...' : 'Registrar solicitud'}
            </button>
          </div>
        </form>
      </section>

      {solicitud && (
        <section className="form-panel solicitud-resultado" aria-live="polite">
          <div className="form-panel-heading">
            <h3>Estado de la solicitud #{solicitud.id}</h3>
            <p>{solicitud.patente} - {solicitud.marca} {solicitud.modelo}</p>
          </div>
          <div className="solicitud-estados">
            <span className={`status-badge ${estadoClase(solicitud.estado)}`}>{solicitud.estado}</span>
            <span>Patente: {solicitud.resultadoPatente || 'Pendiente'}</span>
            <span>Titularidad: {solicitud.resultadoTitularidad || 'Pendiente'}</span>
          </div>
          {solicitud.vehiculoId && <p className="feedback-success">Vehiculo incorporado con ID {solicitud.vehiculoId}.</p>}
          {solicitud.estado === 'PENDIENTE' && (
            <button className="secondary-button" disabled={cargando} onClick={() => void actualizarEstado()} type="button">
              <RefreshCw aria-hidden="true" size={17} />
              Actualizar estado
            </button>
          )}
        </section>
      )}
    </section>
  );
}
