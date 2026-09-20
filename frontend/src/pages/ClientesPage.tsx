import { useEffect, useMemo, useState } from 'react';
import { Plus, Search } from 'lucide-react';
import { ApiError } from '../api/rest/http';
import {
  actualizarCliente,
  crearCliente,
  darDeBajaCliente,
  listarClientes,
} from '../api/rest/clientes.api';
import { ClienteForm } from '../components/clientes/ClienteForm';
import { ClienteTable } from '../components/clientes/ClienteTable';
import type { Cliente, ClienteRequest } from '../types/cliente';

export function ClientesPage() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [busqueda, setBusqueda] = useState('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');
  const [formVisible, setFormVisible] = useState(false);
  const [clienteEnEdicion, setClienteEnEdicion] = useState<Cliente>();
  const [clienteABaja, setClienteABaja] = useState<Cliente>();
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    void cargarClientes();
  }, []);

  async function cargarClientes() {
    setCargando(true);
    setError('');

    try {
      setClientes(await listarClientes());
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible cargar los clientes.');
    } finally {
      setCargando(false);
    }
  }

  const clientesFiltrados = useMemo(() => {
    const search = busqueda.trim().toLowerCase();
    if (!search) return clientes;

    return clientes.filter((cliente) =>
      `${cliente.documento} ${cliente.nombre} ${cliente.apellido}`.toLowerCase().includes(search),
    );
  }, [busqueda, clientes]);

  function abrirAlta() {
    setFormError('');
    setClienteEnEdicion(undefined);
    setFormVisible(true);
  }

  function abrirEdicion(cliente: Cliente) {
    setFormError('');
    setClienteEnEdicion(cliente);
    setFormVisible(true);
  }

  function cerrarFormulario() {
    setFormError('');
    setClienteEnEdicion(undefined);
    setFormVisible(false);
  }

  async function guardarCliente(data: ClienteRequest) {
    setGuardando(true);
    setFormError('');

    try {
      if (clienteEnEdicion) {
        await actualizarCliente(clienteEnEdicion.id, data);
      } else {
        await crearCliente(data);
      }
      cerrarFormulario();
      await cargarClientes();
    } catch (cause) {
      setFormError(cause instanceof ApiError ? cause.message : 'No fue posible guardar el cliente.');
    } finally {
      setGuardando(false);
    }
  }

  async function confirmarBaja() {
    if (!clienteABaja) return;

    setGuardando(true);
    setError('');
    try {
      await darDeBajaCliente(clienteABaja.id);
      setClienteABaja(undefined);
      await cargarClientes();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'No fue posible dar de baja el cliente.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section className="page-content" aria-labelledby="clientes-title">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Administracion</p>
          <h2 id="clientes-title">Clientes</h2>
          <p>Gestiona los datos de las personas registradas.</p>
        </div>
        <button className="primary-button" onClick={abrirAlta} type="button">
          <Plus aria-hidden="true" size={18} />
          Nuevo cliente
        </button>
      </div>

      {formVisible && (
        <ClienteForm
          cliente={clienteEnEdicion}
          error={formError}
          onCancel={cerrarFormulario}
          onSubmit={guardarCliente}
          submitting={guardando}
        />
      )}

      <div className="toolbar">
        <label className="search-field">
          <Search aria-hidden="true" size={18} />
          <span className="sr-only">Buscar cliente</span>
          <input
            onChange={(event) => setBusqueda(event.target.value)}
            placeholder="Buscar por documento o nombre"
            type="search"
            value={busqueda}
          />
        </label>
      </div>

      {cargando && <p className="feedback-message">Cargando clientes...</p>}
      {!cargando && error && <p className="feedback-message feedback-error" role="alert">{error}</p>}
      {!cargando && !error && clientesFiltrados.length === 0 && (
        <p className="feedback-message">No se encontraron clientes.</p>
      )}
      {!cargando && !error && clientesFiltrados.length > 0 && (
        <ClienteTable clientes={clientesFiltrados} onDarDeBaja={setClienteABaja} onEditar={abrirEdicion} />
      )}

      {clienteABaja && (
        <div className="modal-backdrop" role="presentation">
          <section aria-labelledby="baja-cliente-title" aria-modal="true" className="confirmation-dialog" role="dialog">
            <h3 id="baja-cliente-title">Dar de baja cliente</h3>
            <p>{clienteABaja.nombre} {clienteABaja.apellido} quedara inactivo y no podra realizar nuevas reservas.</p>
            <div className="form-actions">
              <button className="secondary-button" disabled={guardando} onClick={() => setClienteABaja(undefined)} type="button">Cancelar</button>
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
