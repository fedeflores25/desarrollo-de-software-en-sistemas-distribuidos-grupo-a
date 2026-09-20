interface ConsultasPendientesPageProps {
  tipo: 'reservas' | 'historial';
}

const content = {
  reservas: {
    title: 'Consulta de reservas',
    description: 'Filtra reservas por cliente, vehiculo, tipo, estado y rango de fechas.',
    columns: ['Cliente', 'Vehiculo', 'Patente', 'Inicio', 'Finalizacion', 'Precio diario', 'Importe total', 'Estado'],
  },
  historial: {
    title: 'Historial de alquileres',
    description: 'Consulta alquileres finalizados y reservas canceladas.',
    columns: ['Vehiculo', 'Patente', 'Inicio', 'Finalizacion', 'Cantidad de dias', 'Importe total', 'Estado'],
  },
} as const;

export function ConsultasPendientesPage({ tipo }: ConsultasPendientesPageProps) {
  const page = content[tipo];

  return (
    <section className="page-content" aria-labelledby="consulta-pendiente-title">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Consulta</p>
          <h2 id="consulta-pendiente-title">{page.title}</h2>
          <p>{page.description}</p>
        </div>
      </div>

      {tipo === 'reservas' && (
        <section className="form-panel" aria-label="Filtros de reservas">
          <div className="form-grid">
            <label className="form-field"><span>Cliente</span><input disabled placeholder="Pendiente de integracion" /></label>
            <label className="form-field"><span>Vehiculo</span><input disabled placeholder="Pendiente de integracion" /></label>
            <label className="form-field"><span>Tipo de vehiculo</span><select disabled><option>Pendiente de integracion</option></select></label>
            <label className="form-field"><span>Estado</span><select disabled><option>Pendiente de integracion</option></select></label>
            <label className="form-field"><span>Desde</span><input disabled type="datetime-local" /></label>
            <label className="form-field"><span>Hasta</span><input disabled type="datetime-local" /></label>
          </div>
        </section>
      )}

      <p className="feedback-message">Esta consulta estara disponible al finalizar la integracion del servicio GraphQL.</p>

      <div className="data-table-wrapper">
        <table className="data-table">
          <thead><tr>{page.columns.map((column) => <th key={column}>{column}</th>)}</tr></thead>
          <tbody><tr><td colSpan={page.columns.length}>Sin datos disponibles.</td></tr></tbody>
        </table>
      </div>
    </section>
  );
}
