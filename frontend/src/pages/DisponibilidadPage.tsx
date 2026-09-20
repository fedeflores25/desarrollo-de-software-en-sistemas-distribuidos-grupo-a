import { useState, type FormEvent } from 'react';
import { Search } from 'lucide-react';
import { ApiError } from '../api/rest/http';
import { buscarDisponibilidad } from '../api/graphql/disponibilidad.api';
import type { DisponibilidadFiltro, VehiculoDisponible } from '../types/disponibilidad';
import type { TipoVehiculo } from '../types/vehiculo';

const tipos: TipoVehiculo[] = ['SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK'];

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 }).format(value);
}

export function DisponibilidadPage() {
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [tipo, setTipo] = useState('');
  const [marca, setMarca] = useState('');
  const [modelo, setModelo] = useState('');
  const [precioMin, setPrecioMin] = useState('');
  const [precioMax, setPrecioMax] = useState('');
  const [resultados, setResultados] = useState<VehiculoDisponible[]>();
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCargando(true);
    setError('');

    const filtro: DisponibilidadFiltro = {
      fechaInicio,
      fechaFin,
      ...(tipo && { tipo: tipo as TipoVehiculo }),
      ...(marca.trim() && { marca: marca.trim() }),
      ...(modelo.trim() && { modelo: modelo.trim() }),
      ...(precioMin && { precioMin: Number(precioMin) }),
      ...(precioMax && { precioMax: Number(precioMax) }),
    };

    try {
      setResultados(await buscarDisponibilidad(filtro));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible consultar la disponibilidad.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <section className="page-content" aria-labelledby="disponibilidad-title">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Consulta</p>
          <h2 id="disponibilidad-title">Disponibilidad</h2>
          <p>Consulta vehiculos disponibles durante el periodo seleccionado.</p>
        </div>
      </div>

      <section className="form-panel" aria-label="Filtros de disponibilidad">
        <form className="form-grid" onSubmit={(event) => void handleSubmit(event)}>
          <label className="form-field">
            <span>Inicio</span>
            <input onChange={(event) => setFechaInicio(event.target.value)} required type="datetime-local" value={fechaInicio} />
          </label>
          <label className="form-field">
            <span>Finalizacion</span>
            <input onChange={(event) => setFechaFin(event.target.value)} required type="datetime-local" value={fechaFin} />
          </label>
          <label className="form-field">
            <span>Tipo</span>
            <select onChange={(event) => setTipo(event.target.value)} value={tipo}>
              <option value="">Todos los tipos</option>
              {tipos.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
          <label className="form-field">
            <span>Marca</span>
            <input onChange={(event) => setMarca(event.target.value)} value={marca} />
          </label>
          <label className="form-field">
            <span>Modelo</span>
            <input onChange={(event) => setModelo(event.target.value)} value={modelo} />
          </label>
          <label className="form-field">
            <span>Precio minimo</span>
            <input min="0" onChange={(event) => setPrecioMin(event.target.value)} type="number" value={precioMin} />
          </label>
          <label className="form-field">
            <span>Precio maximo</span>
            <input min="0" onChange={(event) => setPrecioMax(event.target.value)} type="number" value={precioMax} />
          </label>
          <div className="form-actions">
            <button className="primary-button" disabled={cargando} type="submit">
              <Search aria-hidden="true" size={18} />
              {cargando ? 'Consultando...' : 'Consultar disponibilidad'}
            </button>
          </div>
        </form>
      </section>

      {error && <p className="feedback-message feedback-error" role="alert">{error}</p>}
      {resultados && resultados.length === 0 && !error && <p className="feedback-message">No hay vehiculos disponibles para esos filtros.</p>}
      {resultados && resultados.length > 0 && (
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>Patente</th><th>Vehiculo</th><th>Tipo</th><th>Anio</th><th>Color</th><th>Precio diario</th></tr></thead>
            <tbody>
              {resultados.map((vehiculo) => (
                <tr key={vehiculo.patente}>
                  <td className="table-strong">{vehiculo.patente}</td>
                  <td>{vehiculo.marca} {vehiculo.modelo}</td>
                  <td>{vehiculo.tipo}</td>
                  <td>{vehiculo.anio}</td>
                  <td>{vehiculo.color || '-'}</td>
                  <td>{formatCurrency(vehiculo.precioDiario)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
