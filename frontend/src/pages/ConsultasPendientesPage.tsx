import { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import {
  consultarHistorialAlquileres,
  consultarReservas,
} from '../api/graphql/consultas.api';
import { ApiError } from '../api/rest/http';
import { listarClientes } from '../api/rest/clientes.api';
import { listarVehiculos } from '../api/rest/vehiculos.api';
import type { Cliente } from '../types/cliente';
import type {
  ConsultaReserva,
  HistorialAlquiler,
  ReservaConsultaFiltro,
} from '../types/consultas';
import type { EstadoReserva } from '../types/reserva';
import type { TipoVehiculo, Vehiculo } from '../types/vehiculo';

interface ConsultasPendientesPageProps {
  tipo: 'reservas' | 'historial';
}

const tiposVehiculo: TipoVehiculo[] = ['SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK'];
const estadosReserva: EstadoReserva[] = ['CONFIRMADA', 'CANCELADA', 'FINALIZADA'];

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    maximumFractionDigits: 0,
  }).format(value);
}

function formatDateTime(value: string): string {
  return value.replace('T', ' ');
}

export function ConsultasPendientesPage({ tipo }: ConsultasPendientesPageProps) {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([]);
  const [clienteId, setClienteId] = useState('');
  const [vehiculoId, setVehiculoId] = useState('');
  const [tipoVehiculo, setTipoVehiculo] = useState('');
  const [estado, setEstado] = useState('');
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [reservas, setReservas] = useState<ConsultaReserva[]>();
  const [historial, setHistorial] = useState<HistorialAlquiler[]>();
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (tipo === 'reservas') {
      void cargarFiltros();
      return;
    }

    void cargarHistorial();
  }, [tipo]);

  async function cargarFiltros() {
    setCargando(true);
    setError('');

    try {
      const [clientesResult, vehiculosResult] = await Promise.all([listarClientes(), listarVehiculos()]);
      setClientes(clientesResult);
      setVehiculos(vehiculosResult);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible cargar los filtros.');
    } finally {
      setCargando(false);
    }
  }

  async function buscarReservas() {
    setCargando(true);
    setError('');

    const filtro: ReservaConsultaFiltro = {
      ...(clienteId && { clienteId: Number(clienteId) }),
      ...(vehiculoId && { vehiculoId: Number(vehiculoId) }),
      ...(tipoVehiculo && { tipoVehiculo: tipoVehiculo as TipoVehiculo }),
      ...(estado && { estado: estado as EstadoReserva }),
      ...(fechaInicio && { fechaInicio }),
      ...(fechaFin && { fechaFin }),
    };

    try {
      setReservas(await consultarReservas(filtro));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible consultar las reservas.');
    } finally {
      setCargando(false);
    }
  }

  async function cargarHistorial() {
    setCargando(true);
    setError('');

    try {
      setHistorial(await consultarHistorialAlquileres());
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible cargar el historial.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <section className="page-content" aria-labelledby="consulta-pendiente-title">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Consulta</p>
          <h2 id="consulta-pendiente-title">
            {tipo === 'reservas' ? 'Consulta de reservas' : 'Historial de alquileres'}
          </h2>
          <p>
            {tipo === 'reservas'
              ? 'Filtra reservas por cliente, vehiculo, tipo, estado y rango de fechas.'
              : 'Consulta alquileres finalizados y reservas canceladas.'}
          </p>
        </div>
      </div>

      {tipo === 'reservas' && (
        <section className="form-panel" aria-label="Filtros de reservas">
          <div className="form-grid">
            <label className="form-field">
              <span>Cliente</span>
              <select onChange={(event) => setClienteId(event.target.value)} value={clienteId}>
                <option value="">Todos los clientes</option>
                {clientes.map((cliente) => <option key={cliente.id} value={cliente.id}>{cliente.nombre} {cliente.apellido}</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Vehiculo</span>
              <select onChange={(event) => setVehiculoId(event.target.value)} value={vehiculoId}>
                <option value="">Todos los vehiculos</option>
                {vehiculos.map((vehiculo) => <option key={vehiculo.id} value={vehiculo.id}>{vehiculo.patente} - {vehiculo.marca} {vehiculo.modelo}</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Tipo de vehiculo</span>
              <select onChange={(event) => setTipoVehiculo(event.target.value)} value={tipoVehiculo}>
                <option value="">Todos los tipos</option>
                {tiposVehiculo.map((item) => <option key={item} value={item}>{item}</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Estado</span>
              <select onChange={(event) => setEstado(event.target.value)} value={estado}>
                <option value="">Todos los estados</option>
                {estadosReserva.map((item) => <option key={item} value={item}>{item}</option>)}
              </select>
            </label>
            <label className="form-field"><span>Desde</span><input onChange={(event) => setFechaInicio(event.target.value)} type="datetime-local" value={fechaInicio} /></label>
            <label className="form-field"><span>Hasta</span><input onChange={(event) => setFechaFin(event.target.value)} type="datetime-local" value={fechaFin} /></label>
            <div className="form-actions">
              <button className="primary-button" disabled={cargando} onClick={() => void buscarReservas()} type="button">
                <Search aria-hidden="true" size={18} />
                {cargando ? 'Consultando...' : 'Consultar reservas'}
              </button>
            </div>
          </div>
        </section>
      )}

      {cargando && <p className="feedback-message">Cargando datos...</p>}
      {error && <p className="feedback-message feedback-error" role="alert">{error}</p>}

      {tipo === 'reservas' && reservas && !cargando && !error && (
        reservas.length === 0 ? <p className="feedback-message">No se encontraron reservas con esos filtros.</p> : (
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>Cliente</th><th>Vehiculo</th><th>Patente</th><th>Inicio</th><th>Finalizacion</th><th>Precio diario</th><th>Importe total</th><th>Estado</th></tr></thead>
              <tbody>{reservas.map((reserva) => (
                <tr key={`${reserva.patente}-${reserva.fechaInicio}`}>
                  <td>{reserva.cliente}</td><td>{reserva.vehiculo}</td><td className="table-strong">{reserva.patente}</td>
                  <td>{formatDateTime(reserva.fechaInicio)}</td><td>{formatDateTime(reserva.fechaFinalizacion)}</td>
                  <td>{formatCurrency(reserva.precioDiario)}</td><td>{formatCurrency(reserva.importeTotal)}</td><td>{reserva.estado}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )
      )}

      {tipo === 'historial' && historial && !cargando && !error && (
        historial.length === 0 ? <p className="feedback-message">No hay alquileres finalizados ni reservas canceladas.</p> : (
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>Vehiculo</th><th>Patente</th><th>Inicio</th><th>Finalizacion</th><th>Cantidad de dias</th><th>Importe total</th><th>Estado</th></tr></thead>
              <tbody>{historial.map((alquiler) => (
                <tr key={`${alquiler.patente}-${alquiler.fechaInicio}`}>
                  <td>{alquiler.vehiculo}</td><td className="table-strong">{alquiler.patente}</td>
                  <td>{formatDateTime(alquiler.fechaInicio)}</td><td>{formatDateTime(alquiler.fechaFinalizacion)}</td>
                  <td>{alquiler.cantidadDias}</td><td>{formatCurrency(alquiler.importeTotal)}</td><td>{alquiler.estado}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )
      )}
    </section>
  );
}
