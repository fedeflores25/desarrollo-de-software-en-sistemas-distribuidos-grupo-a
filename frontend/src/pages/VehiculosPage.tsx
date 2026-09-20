import { useEffect, useMemo, useState } from 'react';
import { Plus, Search } from 'lucide-react';
import { ApiError } from '../api/rest/http';
import {
  actualizarVehiculo,
  crearVehiculo,
  darDeBajaVehiculo,
  listarVehiculos,
} from '../api/rest/vehiculos.api';
import { VehiculoTable } from '../components/vehiculos/VehiculoTable';
import { VehiculoForm } from '../components/vehiculos/VehiculoForm';
import type {
  ActualizarVehiculoRequest,
  CrearVehiculoRequest,
  Vehiculo,
} from '../types/vehiculo';

export function VehiculosPage() {
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([]);
  const [busqueda, setBusqueda] = useState('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');
  const [formVisible, setFormVisible] = useState(false);
  const [vehiculoEnEdicion, setVehiculoEnEdicion] = useState<Vehiculo>();
  const [vehiculoABaja, setVehiculoABaja] = useState<Vehiculo>();
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    void cargarVehiculos();
  }, []);

  async function cargarVehiculos() {
    setCargando(true);
    setError('');

    try {
      setVehiculos(await listarVehiculos());
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : 'No fue posible cargar los vehiculos.',
      );
    } finally {
      setCargando(false);
    }
  }

  const vehiculosFiltrados = useMemo(() => {
    const patente = busqueda.trim().toUpperCase();

    if (!patente) {
      return vehiculos;
    }

    return vehiculos.filter((vehiculo) => vehiculo.patente.toUpperCase().includes(patente));
  }, [busqueda, vehiculos]);

  function abrirAlta() {
    setFormError('');
    setVehiculoEnEdicion(undefined);
    setFormVisible(true);
  }

  function abrirEdicion(vehiculo: Vehiculo) {
    setFormError('');
    setVehiculoEnEdicion(vehiculo);
    setFormVisible(true);
  }

  function cerrarFormulario() {
    setFormVisible(false);
    setVehiculoEnEdicion(undefined);
    setFormError('');
  }

  async function guardarVehiculo(data: CrearVehiculoRequest | ActualizarVehiculoRequest) {
    setGuardando(true);
    setFormError('');

    try {
      if (vehiculoEnEdicion) {
        await actualizarVehiculo(vehiculoEnEdicion.id, data as ActualizarVehiculoRequest);
      } else {
        await crearVehiculo(data as CrearVehiculoRequest);
      }

      cerrarFormulario();
      await cargarVehiculos();
    } catch (cause) {
      setFormError(cause instanceof ApiError ? cause.message : 'No fue posible guardar el vehiculo.');
    } finally {
      setGuardando(false);
    }
  }

  async function confirmarBaja() {
    if (!vehiculoABaja) {
      return;
    }

    setGuardando(true);
    setError('');

    try {
      await darDeBajaVehiculo(vehiculoABaja.id);
      setVehiculoABaja(undefined);
      await cargarVehiculos();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible dar de baja el vehiculo.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section className="page-content" aria-labelledby="vehiculos-title">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Administracion</p>
          <h2 id="vehiculos-title">Vehiculos</h2>
          <p>Gestiona la flota disponible para alquiler.</p>
        </div>
        <button className="primary-button" onClick={abrirAlta} type="button">
          <Plus aria-hidden="true" size={18} />
          Nuevo vehiculo
        </button>
      </div>

      {formVisible && (
        <VehiculoForm
          error={formError}
          onCancel={cerrarFormulario}
          onSubmit={guardarVehiculo}
          submitting={guardando}
          vehiculo={vehiculoEnEdicion}
        />
      )}

      <div className="toolbar">
        <label className="search-field">
          <Search aria-hidden="true" size={18} />
          <span className="sr-only">Buscar por patente</span>
          <input
            onChange={(event) => setBusqueda(event.target.value)}
            placeholder="Buscar por patente"
            type="search"
            value={busqueda}
          />
        </label>
      </div>

      {cargando && <p className="feedback-message">Cargando vehiculos...</p>}

      {!cargando && error && (
        <div className="feedback-message feedback-error" role="alert">
          <p>{error}</p>
          <button className="secondary-button" onClick={() => void cargarVehiculos()} type="button">
            Reintentar
          </button>
        </div>
      )}

      {!cargando && !error && vehiculosFiltrados.length === 0 && (
        <p className="feedback-message">No se encontraron vehiculos para la patente indicada.</p>
      )}

      {!cargando && !error && vehiculosFiltrados.length > 0 && (
        <VehiculoTable
          onDarDeBaja={setVehiculoABaja}
          onEditar={abrirEdicion}
          vehiculos={vehiculosFiltrados}
        />
      )}

      {vehiculoABaja && (
        <div className="modal-backdrop" role="presentation">
          <section aria-labelledby="baja-title" aria-modal="true" className="confirmation-dialog" role="dialog">
            <h3 id="baja-title">Dar de baja vehiculo</h3>
            <p>El vehiculo {vehiculoABaja.patente} quedara inactivo y no podra utilizarse en nuevas reservas.</p>
            <div className="form-actions">
              <button className="secondary-button" disabled={guardando} onClick={() => setVehiculoABaja(undefined)} type="button">
                Cancelar
              </button>
              <button className="danger-button" disabled={guardando} onClick={() => void confirmarBaja()} type="button">
                {guardando ? 'Procesando...' : 'Confirmar baja'}
              </button>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}
