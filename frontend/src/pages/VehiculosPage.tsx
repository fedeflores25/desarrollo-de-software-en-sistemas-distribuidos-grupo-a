import { useEffect, useMemo, useState } from 'react';
import { Plus, Search } from 'lucide-react';
import { ApiError } from '../api/rest/http';
import { listarVehiculos } from '../api/rest/vehiculos.api';
import { VehiculoTable } from '../components/vehiculos/VehiculoTable';
import type { Vehiculo } from '../types/vehiculo';

export function VehiculosPage() {
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([]);
  const [busqueda, setBusqueda] = useState('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

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

  return (
    <section className="page-content" aria-labelledby="vehiculos-title">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Administracion</p>
          <h2 id="vehiculos-title">Vehiculos</h2>
          <p>Gestiona la flota disponible para alquiler.</p>
        </div>
        <button className="primary-button" type="button">
          <Plus aria-hidden="true" size={18} />
          Nuevo vehiculo
        </button>
      </div>

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
        <VehiculoTable vehiculos={vehiculosFiltrados} />
      )}
    </section>
  );
}
