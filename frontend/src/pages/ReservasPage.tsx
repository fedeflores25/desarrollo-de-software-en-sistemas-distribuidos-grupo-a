import { useEffect, useState, type FormEvent } from 'react';
import { CalendarPlus, XCircle } from 'lucide-react';
import { ApiError } from '../api/rest/http';
import { obtenerSesion } from '../api/auth';
import { listarClientes } from '../api/rest/clientes.api';
import { cancelarReserva, crearReserva } from '../api/rest/reservas.api';
import { consultarReservas } from '../api/graphql/consultas.api';
import { buscarDisponibilidad } from '../api/graphql/disponibilidad.api';
import type { Cliente } from '../types/cliente';
import type { ConsultaReserva } from '../types/consultas';
import type { Reserva } from '../types/reserva';
import type { Vehiculo } from '../types/vehiculo';

// Agrega segundos cuando datetime-local entrega solo horas y minutos.
function fechaConSegundos(valor: string): string {
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(valor)
    ? `${valor}:00`
    : valor;
}

export function ReservasPage() {
  const sesion = obtenerSesion();
  const esCliente = sesion?.usuario.rol === 'CLIENTE';

  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([]);
  const [busquedaRealizada, setBusquedaRealizada] = useState(false);
  const [clienteId, setClienteId] = useState('');
  const [vehiculoId, setVehiculoId] = useState('');
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [reservasConfirmadas, setReservasConfirmadas] =
    useState<ConsultaReserva[]>([]);
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
      if (esCliente && sesion.usuario.clienteId) {
        setClienteId(String(sesion.usuario.clienteId));
        await cargarReservasConfirmadas();
        return;
      }

      const clientesResult = await listarClientes();
      setClientes(clientesResult.filter((cliente) => cliente.activo));

      await cargarReservasConfirmadas();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : 'No fue posible cargar los datos de reserva.'
      );
    } finally {
      setCargando(false);
    }
  }

  async function cargarVehiculosDisponibles() {
    // Evita conservar vehículos de consultas anteriores.
    setVehiculos([]);
    setVehiculoId('');
    setBusquedaRealizada(false);

    if (!fechaInicio || !fechaFin) {
      setError(
        'Indica inicio y finalizacion antes de consultar disponibilidad.'
      );
      return;
    }

    if (fechaFin <= fechaInicio) {
      setError('La finalizacion debe ser posterior al inicio.');
      return;
    }

    setGuardando(true);
    setError('');

    try {
      const disponibles = await buscarDisponibilidad({
        fechaInicio: fechaConSegundos(fechaInicio),
        fechaFin: fechaConSegundos(fechaFin),
      });

      setVehiculos(
        disponibles.map((vehiculo) => ({
          ...vehiculo,
          estado: 'DISPONIBLE',
          activo: true,
        }))
      );

      setBusquedaRealizada(true);
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : 'No fue posible consultar vehiculos disponibles.'
      );
    } finally {
      setGuardando(false);
    }
  }

  async function cargarReservasConfirmadas() {
    setReservasConfirmadas(
      await consultarReservas({ estado: 'CONFIRMADA' })
    );
  }

  async function handleCrearReserva(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    setGuardando(true);
    setError('');
    setResultado(undefined);

    try {
      const reserva = await crearReserva({
        clienteId: Number(clienteId),
        vehiculoId: Number(vehiculoId),
        fechaInicio: fechaConSegundos(fechaInicio),
        fechaFin: fechaConSegundos(fechaFin),
      });

      setResultado(reserva);

      if (!esCliente) {
        setClienteId('');
      }

      setVehiculoId('');
      setVehiculos([]);
      setBusquedaRealizada(false);
      setFechaInicio('');
      setFechaFin('');

      await cargarReservasConfirmadas();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : 'No fue posible crear la reserva.'
      );
    } finally {
      setGuardando(false);
    }
  }

  async function handleCancelarReserva(
    reserva: ConsultaReserva
  ) {
    setGuardando(true);
    setError('');
    setResultado(undefined);

    try {
      setResultado(await cancelarReserva(reserva.id));
      await cargarReservasConfirmadas();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : 'No fue posible cancelar la reserva.'
      );
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section
      className="page-content"
      aria-labelledby="reservas-title"
    >
      <div className="page-heading">
        <div>
          <p className="eyebrow">Administracion</p>
          <h2 id="reservas-title">Reservas</h2>
          <p>
            Registra reservas y cancela solicitudes antes de su inicio.
          </p>
        </div>
      </div>

      {cargando && (
        <p className="feedback-message">
          Cargando clientes y vehiculos...
        </p>
      )}

      {!cargando && error && (
        <p
          className="feedback-message feedback-error"
          role="alert"
        >
          {error}
        </p>
      )}

      {!cargando && (
        <div className="two-column-layout">
          <section
            className="form-panel"
            aria-labelledby="nueva-reserva-title"
          >
            <div className="form-panel-heading">
              <h3 id="nueva-reserva-title">Nueva reserva</h3>
            </div>

            <form
              className="form-grid"
              onSubmit={(event) =>
                void handleCrearReserva(event)
              }
            >
              {esCliente ? (
                <p className="feedback-message">
                  Reserva para tu cuenta.
                </p>
              ) : (
                <label className="form-field">
                  <span>Cliente</span>
                  <select
                    onChange={(event) =>
                      setClienteId(event.target.value)
                    }
                    required
                    value={clienteId}
                  >
                    <option value="">
                      Seleccionar cliente
                    </option>

                    {clientes.map((cliente) => (
                      <option
                        key={cliente.id}
                        value={cliente.id}
                      >
                        {cliente.documento} - {cliente.nombre}{' '}
                        {cliente.apellido}
                      </option>
                    ))}
                  </select>
                </label>
              )}

              <label className="form-field">
                <span>Vehiculo</span>
                <select
                  disabled={
                    !busquedaRealizada ||
                    vehiculos.length === 0 ||
                    guardando
                  }
                  onChange={(event) =>
                    setVehiculoId(event.target.value)
                  }
                  required
                  value={vehiculoId}
                >
                  <option value="">
                    {!busquedaRealizada
                      ? 'Primero buscar vehiculos disponibles'
                      : vehiculos.length === 0
                        ? 'No hay vehiculos disponibles'
                        : 'Seleccionar vehiculo'}
                  </option>

                  {vehiculos.map((vehiculo) => (
                    <option
                      key={vehiculo.id}
                      value={vehiculo.id}
                    >
                      {vehiculo.patente} - {vehiculo.marca}{' '}
                      {vehiculo.modelo}
                    </option>
                  ))}
                </select>
              </label>

              <label className="form-field">
                <span>Inicio</span>
                <input
                  onChange={(event) => {
                    setFechaInicio(event.target.value);
                    setVehiculoId('');
                    setVehiculos([]);
                    setBusquedaRealizada(false);
                    setError('');
                  }}
                  required
                  type="datetime-local"
                  value={fechaInicio}
                />
              </label>

              <label className="form-field">
                <span>Finalizacion</span>
                <input
                  min={fechaInicio || undefined}
                  onChange={(event) => {
                    setFechaFin(event.target.value);
                    setVehiculoId('');
                    setVehiculos([]);
                    setBusquedaRealizada(false);
                    setError('');
                  }}
                  required
                  type="datetime-local"
                  value={fechaFin}
                />
              </label>

              {busquedaRealizada && (
                <p
                  className="feedback-message"
                  style={{ gridColumn: '1 / -1' }}
                  role="status"
                >
                  {vehiculos.length === 0
                    ? 'No hay vehiculos disponibles para las fechas seleccionadas.'
                    : `${vehiculos.length} vehiculo(s) disponible(s). Selecciona uno para crear la reserva.`}
                </p>
              )}

              <div className="form-actions">
                <button
                  className="secondary-button"
                  disabled={guardando}
                  onClick={() =>
                    void cargarVehiculosDisponibles()
                  }
                  type="button"
                >
                  Buscar vehiculos disponibles
                </button>

                <button
                  className="primary-button"
                  disabled={
                    guardando ||
                    !busquedaRealizada ||
                    !clienteId ||
                    !vehiculoId
                  }
                  type="submit"
                >
                  <CalendarPlus
                    aria-hidden="true"
                    size={18}
                  />
                  {guardando
                    ? 'Procesando...'
                    : 'Crear reserva'}
                </button>
              </div>
            </form>
          </section>

          <section
            className="form-panel"
            aria-labelledby="cancelar-reserva-title"
          >
            <div className="form-panel-heading">
              <h3 id="cancelar-reserva-title">
                Reservas confirmadas
              </h3>
              <p>
                Selecciona una reserva para cancelarla
                antes de su inicio.
              </p>
            </div>

            {reservasConfirmadas.length === 0 ? (
              <p className="feedback-message">
                No hay reservas confirmadas para cancelar.
              </p>
            ) : (
              <div className="reservation-selection-list">
                {reservasConfirmadas.map((reserva) => (
                  <div
                    className="reservation-selection-item"
                    key={reserva.id}
                  >
                    <div>
                      <strong>{reserva.cliente}</strong>
                      <span>
                        {reserva.patente} - {reserva.vehiculo}
                      </span>
                    </div>

                    <button
                      className="danger-button"
                      disabled={guardando}
                      onClick={() =>
                        void handleCancelarReserva(reserva)
                      }
                      type="button"
                    >
                      <XCircle
                        aria-hidden="true"
                        size={16}
                      />
                      {guardando
                        ? 'Procesando...'
                        : 'Cancelar'}
                    </button>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      )}

      {resultado && (
        <div
          className="feedback-message feedback-success"
          role="status"
        >
          Reserva #{resultado.id} - estado {resultado.estado} -
          importe total ${resultado.importeTotal}.
        </div>
      )}
    </section>
  );
}
