import { Pencil, Trash2 } from 'lucide-react';
import type { Cliente } from '../../types/cliente';

interface ClienteTableProps {
  clientes: Cliente[];
  onEditar: (cliente: Cliente) => void;
  onDarDeBaja: (cliente: Cliente) => void;
}

export function ClienteTable({ clientes, onEditar, onDarDeBaja }: ClienteTableProps) {
  return (
    <div className="data-table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            <th>Documento</th>
            <th>Cliente</th>
            <th>Email</th>
            <th>Telefono</th>
            <th>Estado</th>
            <th aria-label="Acciones" />
          </tr>
        </thead>
        <tbody>
          {clientes.map((cliente) => (
            <tr key={cliente.id}>
              <td className="table-strong">{cliente.documento}</td>
              <td>{cliente.nombre} {cliente.apellido}</td>
              <td>{cliente.email}</td>
              <td>{cliente.telefono || '-'}</td>
              <td>
                <span className={cliente.activo ? 'status-badge status-active' : 'status-badge status-inactive'}>
                  {cliente.activo ? 'Activo' : 'Inactivo'}
                </span>
              </td>
              <td className="table-actions">
                <button
                  aria-label={`Editar ${cliente.nombre} ${cliente.apellido}`}
                  className="icon-button"
                  onClick={() => onEditar(cliente)}
                  title="Editar cliente"
                  type="button"
                >
                  <Pencil aria-hidden="true" size={16} />
                </button>
                <button
                  aria-label={`Dar de baja ${cliente.nombre} ${cliente.apellido}`}
                  className="icon-button icon-button-danger"
                  onClick={() => onDarDeBaja(cliente)}
                  title="Dar de baja cliente"
                  type="button"
                >
                  <Trash2 aria-hidden="true" size={16} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
