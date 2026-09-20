import {
  CalendarDays,
  CarFront,
  ClipboardList,
  LayoutDashboard,
  Search,
  Users,
} from 'lucide-react';

const administrationItems = [
  { label: 'Vehiculos', icon: CarFront },
  { label: 'Clientes', icon: Users },
  { label: 'Reservas', icon: CalendarDays },
];

const queryItems = [
  { label: 'Disponibilidad', icon: Search },
  { label: 'Consulta de reservas', icon: ClipboardList },
  { label: 'Historial de alquileres', icon: ClipboardList },
];

function App() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">R</span>
          <div>
            <strong>Rentar</strong>
            <span>Gestion</span>
          </div>
        </div>

        <nav className="sidebar-nav" aria-label="Navegacion principal">
          <span className="nav-section-label">Administracion</span>
          {administrationItems.map(({ label, icon: Icon }) => (
            <button className="nav-item" key={label} type="button">
              <Icon aria-hidden="true" size={18} />
              {label}
            </button>
          ))}

          <span className="nav-section-label">Consultas</span>
          {queryItems.map(({ label, icon: Icon }) => (
            <button className="nav-item" key={label} type="button">
              <Icon aria-hidden="true" size={18} />
              {label}
            </button>
          ))}
        </nav>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div>
            <p className="eyebrow">Panel administrativo</p>
            <h1>Gestion de Rentar</h1>
          </div>
          <LayoutDashboard aria-hidden="true" className="topbar-icon" size={24} />
        </header>

        <section className="workspace" aria-labelledby="workspace-title">
          <div className="workspace-heading">
            <h2 id="workspace-title">Modulos disponibles</h2>
            <p>Selecciona una opcion del menu para gestionar la informacion.</p>
          </div>

          <div className="module-grid">
            <article className="module-card">
              <CarFront aria-hidden="true" size={24} />
              <h3>Vehiculos</h3>
              <p>Administra la flota, sus datos y su estado.</p>
            </article>
            <article className="module-card">
              <Users aria-hidden="true" size={24} />
              <h3>Clientes</h3>
              <p>Registra y mantiene los datos de los clientes.</p>
            </article>
            <article className="module-card">
              <CalendarDays aria-hidden="true" size={24} />
              <h3>Reservas</h3>
              <p>Registra nuevas reservas y sus cancelaciones.</p>
            </article>
          </div>
        </section>
      </main>
    </div>
  );
}

export default App;
