import { Pencil, Trash2 } from 'lucide-react';
import type { Vehiculo } from '../../types/vehiculo';

interface VehiculoTableProps {
  vehiculos: Vehiculo[];
  onEditar?: (vehiculo: Vehiculo) => void;
  onDarDeBaja?: (vehiculo: Vehiculo) => void;
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    maximumFractionDigits: 0,
  }).format(value);
}

function getEstadoClass(estado: Vehiculo['estado']): string {
  return `status-badge status-${estado.toLowerCase().replace('_', '-')}`;
}

export function VehiculoTable({ vehiculos, onEditar, onDarDeBaja }: VehiculoTableProps) {
  return (
    <div className="data-table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            <th>Patente</th>
            <th>Vehiculo</th>
            <th>Tipo</th>
            <th>Anio</th>
            <th>Precio diario</th>
            <th>Estado</th>
            <th>Activo</th>
            {(onEditar || onDarDeBaja) && <th aria-label="Acciones" />}
          </tr>
        </thead>
        <tbody>
          {vehiculos.map((vehiculo) => (
            <tr key={vehiculo.id}>
              <td className="table-strong">{vehiculo.patente}</td>
              <td>
                {vehiculo.marca} {vehiculo.modelo}
              </td>
              <td>{vehiculo.tipo}</td>
              <td>{vehiculo.anio}</td>
              <td>{formatCurrency(vehiculo.precioDiario)}</td>
              <td>
                <span className={getEstadoClass(vehiculo.estado)}>{vehiculo.estado}</span>
              </td>
              <td>
                <span className={vehiculo.activo ? 'status-badge status-active' : 'status-badge status-inactive'}>
                  {vehiculo.activo ? 'Activo' : 'Inactivo'}
                </span>
              </td>
              {(onEditar || onDarDeBaja) && (
                <td className="table-actions">
                  {onEditar && (
                    <button
                      aria-label={`Editar ${vehiculo.patente}`}
                      className="icon-button"
                      onClick={() => onEditar(vehiculo)}
                      title="Editar vehiculo"
                      type="button"
                    >
                      <Pencil aria-hidden="true" size={16} />
                    </button>
                  )}
                  {onDarDeBaja && (
                    <button
                      aria-label={`Dar de baja ${vehiculo.patente}`}
                      className="icon-button icon-button-danger"
                      onClick={() => onDarDeBaja(vehiculo)}
                      title="Dar de baja vehiculo"
                      type="button"
                    >
                      <Trash2 aria-hidden="true" size={16} />
                    </button>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
