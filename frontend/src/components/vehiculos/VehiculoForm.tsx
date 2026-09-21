import { useEffect, useState } from 'react';
import type {
  ActualizarVehiculoRequest,
  CrearVehiculoRequest,
  EstadoVehiculo,
  TipoVehiculo,
  Vehiculo,
} from '../../types/vehiculo';

interface VehiculoFormProps {
  vehiculo?: Vehiculo;
  error?: string;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (data: CrearVehiculoRequest | ActualizarVehiculoRequest) => Promise<void>;
}

const tipos: TipoVehiculo[] = ['SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK'];
const estados: EstadoVehiculo[] = ['DISPONIBLE', 'RESERVADO', 'EN_ALQUILER'];

interface FormState {
  patente: string;
  marca: string;
  modelo: string;
  anio: string;
  color: string;
  tipo: TipoVehiculo;
  precioDiario: string;
  estado: EstadoVehiculo;
}

function getInitialState(vehiculo?: Vehiculo): FormState {
  return {
    patente: vehiculo?.patente ?? '',
    marca: vehiculo?.marca ?? '',
    modelo: vehiculo?.modelo ?? '',
    anio: vehiculo?.anio.toString() ?? '',
    color: vehiculo?.color ?? '',
    tipo: vehiculo?.tipo ?? 'SEDAN',
    precioDiario: vehiculo?.precioDiario.toString() ?? '',
    estado: vehiculo?.estado ?? 'DISPONIBLE',
  };
}

export function VehiculoForm({ vehiculo, error, submitting, onCancel, onSubmit }: VehiculoFormProps) {
  const [form, setForm] = useState<FormState>(() => getInitialState(vehiculo));

  useEffect(() => {
    setForm(getInitialState(vehiculo));
  }, [vehiculo]);

  function updateField<Key extends keyof FormState>(field: Key, value: FormState[Key]) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const commonData = {
      marca: form.marca.trim(),
      modelo: form.modelo.trim(),
      anio: Number(form.anio),
      color: form.color.trim() || undefined,
      tipo: form.tipo,
      precioDiario: Number(form.precioDiario),
    };

    if (vehiculo) {
      await onSubmit({ ...commonData, estado: form.estado });
      return;
    }

    await onSubmit({ ...commonData, patente: form.patente.trim().toUpperCase() });
  }

  return (
    <section className="form-panel" aria-labelledby="vehiculo-form-title">
      <div className="form-panel-heading">
        <div>
          <h3 id="vehiculo-form-title">{vehiculo ? 'Modificar vehiculo' : 'Nuevo vehiculo'}</h3>
          <p>{vehiculo ? 'La patente no puede modificarse.' : 'Completa los datos para incorporar un vehiculo.'}</p>
        </div>
      </div>

      <form className="form-grid" onSubmit={(event) => void handleSubmit(event)}>
        {!vehiculo && (
          <label className="form-field">
            <span>Patente</span>
            <input
              onChange={(event) => updateField('patente', event.target.value)}
              required
              value={form.patente}
            />
          </label>
        )}
        <label className="form-field">
          <span>Marca</span>
          <input onChange={(event) => updateField('marca', event.target.value)} required value={form.marca} />
        </label>
        <label className="form-field">
          <span>Modelo</span>
          <input onChange={(event) => updateField('modelo', event.target.value)} required value={form.modelo} />
        </label>
        <label className="form-field">
          <span>Anio</span>
          <input
            min="1900"
            onChange={(event) => updateField('anio', event.target.value)}
            required
            type="number"
            value={form.anio}
          />
        </label>
        <label className="form-field">
          <span>Color</span>
          <input onChange={(event) => updateField('color', event.target.value)} value={form.color} />
        </label>
        <label className="form-field">
          <span>Tipo</span>
          <select onChange={(event) => updateField('tipo', event.target.value as TipoVehiculo)} value={form.tipo}>
            {tipos.map((tipo) => <option key={tipo} value={tipo}>{tipo}</option>)}
          </select>
        </label>
        <label className="form-field">
          <span>Precio diario</span>
          <input
            min="0.01"
            onChange={(event) => updateField('precioDiario', event.target.value)}
            required
            step="0.01"
            type="number"
            value={form.precioDiario}
          />
        </label>
        {vehiculo && (
          <label className="form-field">
            <span>Estado</span>
            <select onChange={(event) => updateField('estado', event.target.value as EstadoVehiculo)} value={form.estado}>
              {estados.map((estado) => <option key={estado} value={estado}>{estado}</option>)}
            </select>
          </label>
        )}

        {error && <p className="form-error" role="alert">{error}</p>}

        <div className="form-actions">
          <button className="secondary-button" disabled={submitting} onClick={onCancel} type="button">
            Cancelar
          </button>
          <button className="primary-button" disabled={submitting} type="submit">
            {submitting ? 'Guardando...' : 'Guardar vehiculo'}
          </button>
        </div>
      </form>
    </section>
  );
}
