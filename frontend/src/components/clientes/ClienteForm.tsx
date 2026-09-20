import { useEffect, useState, type FormEvent } from 'react';
import type { Cliente, ClienteRequest } from '../../types/cliente';

interface ClienteFormProps {
  cliente?: Cliente;
  error?: string;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (data: ClienteRequest) => Promise<void>;
}

interface FormState {
  documento: string;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string;
  fechaNacimiento: string;
}

function getInitialState(cliente?: Cliente): FormState {
  return {
    documento: cliente?.documento ?? '',
    nombre: cliente?.nombre ?? '',
    apellido: cliente?.apellido ?? '',
    email: cliente?.email ?? '',
    telefono: cliente?.telefono ?? '',
    fechaNacimiento: cliente?.fechaNacimiento ?? '',
  };
}

export function ClienteForm({ cliente, error, submitting, onCancel, onSubmit }: ClienteFormProps) {
  const [form, setForm] = useState<FormState>(() => getInitialState(cliente));

  useEffect(() => {
    setForm(getInitialState(cliente));
  }, [cliente]);

  function updateField<Key extends keyof FormState>(field: Key, value: FormState[Key]) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    await onSubmit({
      documento: form.documento.trim(),
      nombre: form.nombre.trim(),
      apellido: form.apellido.trim(),
      email: form.email.trim(),
      telefono: form.telefono.trim() || undefined,
      fechaNacimiento: form.fechaNacimiento || undefined,
    });
  }

  return (
    <section className="form-panel" aria-labelledby="cliente-form-title">
      <div className="form-panel-heading">
        <div>
          <h3 id="cliente-form-title">{cliente ? 'Modificar cliente' : 'Nuevo cliente'}</h3>
          <p>Completa los datos de identificacion y contacto.</p>
        </div>
      </div>

      <form className="form-grid" onSubmit={(event) => void handleSubmit(event)}>
        <label className="form-field">
          <span>Documento</span>
          <input onChange={(event) => updateField('documento', event.target.value)} required value={form.documento} />
        </label>
        <label className="form-field">
          <span>Nombre</span>
          <input onChange={(event) => updateField('nombre', event.target.value)} required value={form.nombre} />
        </label>
        <label className="form-field">
          <span>Apellido</span>
          <input onChange={(event) => updateField('apellido', event.target.value)} required value={form.apellido} />
        </label>
        <label className="form-field">
          <span>Email</span>
          <input onChange={(event) => updateField('email', event.target.value)} required type="email" value={form.email} />
        </label>
        <label className="form-field">
          <span>Telefono</span>
          <input onChange={(event) => updateField('telefono', event.target.value)} type="tel" value={form.telefono} />
        </label>
        <label className="form-field">
          <span>Fecha de nacimiento</span>
          <input onChange={(event) => updateField('fechaNacimiento', event.target.value)} type="date" value={form.fechaNacimiento} />
        </label>

        {error && <p className="form-error" role="alert">{error}</p>}

        <div className="form-actions">
          <button className="secondary-button" disabled={submitting} onClick={onCancel} type="button">Cancelar</button>
          <button className="primary-button" disabled={submitting} type="submit">
            {submitting ? 'Guardando...' : 'Guardar cliente'}
          </button>
        </div>
      </form>
    </section>
  );
}
