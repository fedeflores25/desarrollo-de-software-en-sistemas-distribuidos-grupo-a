import { useEffect, useState, type FormEvent } from 'react';
import { CalendarPlus, XCircle } from 'lucide-react';
import { ApiError } from '../api/rest/http';
import { listarClientes } from '../api/rest/clientes.api';
import { cancelarReserva, crearReserva } from '../api/rest/reservas.api';
import { listarVehiculos } from '../api/rest/vehiculos.api';
import type { Cliente } from '../types/cliente';
import type { Reserva } from '../types/reserva';
import type { Vehiculo } from '../types/vehiculo';

export function ReservasPage() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([]);
  const [clienteId, setClienteId] = useState('');
  const [vehiculoId, setVehiculoId] = useState('');
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [reservaId, setReservaId] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  const [resultado, setResultado] = useState<Reserva>();

  useEffect(() => {
    void cargarOpciones();
  }, []);

  async function cargarOpciones() {
    setCargando(true);
    setError('');
    try {
      const [clientesResult, vehiculosResult] = await Promise.all([listarClientes(), listarVehiculos()]);
      setClientes(clientesResult.filter((cliente) => cliente.activo));
      setVehiculos(vehiculosResult.filter((vehiculo) => vehiculo.activo));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible cargar los datos de reserva.');
    } finally {
      setCargando(false);
    }
  }

  async function handleCrearReserva(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setGuardando(true);
    setError('');
    setResultado(undefined);

    try {
      const reserva = await crearReserva({
        clienteId: Number(clienteId),
        vehiculoId: Number(vehiculoId),
        fechaInicio,
        fechaFin,
      });
      setResultado(reserva);
      setClienteId('');
      setVehiculoId('');
      setFechaInicio('');
      setFechaFin('');
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible crear la reserva.');
    } finally {
      setGuardando(false);
    }
  }

  async function handleCancelarReserva(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setGuardando(true);
    setError('');
    setResultado(undefined);

    try {
      setResultado(await cancelarReserva(Number(reservaId)));
      setReservaId('');
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible cancelar la reserva.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section className="page-content" aria-labelledby="reservas-title">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Administracion</p>
          <h2 id="reservas-title">Reservas</h2>
          <p>Registra reservas y cancela solicitudes antes de su inicio.</p>
        </div>
      </div>

      {cargando && <p className="feedback-message">Cargando clientes y vehiculos...</p>}
      {!cargando && error && <p className="feedback-message feedback-error" role="alert">{error}</p>}

      {!cargando && (
        <div className="two-column-layout">
          <section className="form-panel" aria-labelledby="nueva-reserva-title">
            <div className="form-panel-heading">
              <h3 id="nueva-reserva-title">Nueva reserva</h3>
            </div>
            <form className="form-grid" onSubmit={(event) => void handleCrearReserva(event)}>
              <label className="form-field">
                <span>Cliente</span>
                <select onChange={(event) => setClienteId(event.target.value)} required value={clienteId}>
                  <option value="">Seleccionar cliente</option>
                  {clientes.map((cliente) => (
                    <option key={cliente.id} value={cliente.id}>{cliente.documento} - {cliente.nombre} {cliente.apellido}</option>
                  ))}
                </select>
              </label>
              <label className="form-field">
                <span>Vehiculo</span>
                <select onChange={(event) => setVehiculoId(event.target.value)} required value={vehiculoId}>
                  <option value="">Seleccionar vehiculo</option>
                  {vehiculos.map((vehiculo) => (
                    <option key={vehiculo.id} value={vehiculo.id}>{vehiculo.patente} - {vehiculo.marca} {vehiculo.modelo}</option>
                  ))}
                </select>
              </label>
              <label className="form-field">
                <span>Inicio</span>
                <input onChange={(event) => setFechaInicio(event.target.value)} required type="datetime-local" value={fechaInicio} />
              </label>
              <label className="form-field">
                <span>Finalizacion</span>
                <input onChange={(event) => setFechaFin(event.target.value)} required type="datetime-local" value={fechaFin} />
              </label>
              <div className="form-actions">
                <button className="primary-button" disabled={guardando} type="submit">
                  <CalendarPlus aria-hidden="true" size={18} />
                  {guardando ? 'Procesando...' : 'Crear reserva'}
                </button>
              </div>
            </form>
          </section>

          <section className="form-panel" aria-labelledby="cancelar-reserva-title">
            <div className="form-panel-heading">
              <h3 id="cancelar-reserva-title">Cancelar reserva</h3>
              <p>Ingresa el identificador de la reserva a cancelar.</p>
            </div>
            <form className="form-grid" onSubmit={(event) => void handleCancelarReserva(event)}>
              <label className="form-field">
                <span>ID de reserva</span>
                <input min="1" onChange={(event) => setReservaId(event.target.value)} required type="number" value={reservaId} />
              </label>
              <div className="form-actions">
                <button className="danger-button" disabled={guardando} type="submit">
                  <XCircle aria-hidden="true" size={18} />
                  {guardando ? 'Procesando...' : 'Cancelar reserva'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}

      {resultado && (
        <div className="feedback-message feedback-success" role="status">
          Reserva #{resultado.id} - patente {resultado.patente} - estado {resultado.estado} - importe total ${resultado.importeTotal}.
        </div>
      )}
    </section>
  );
}
