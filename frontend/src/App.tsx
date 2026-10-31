import {
  CalendarDays,
  CarFront,
  ClipboardList,
  Search,
  Users,
} from 'lucide-react';
import { useState } from 'react';
import { cerrarSesion, obtenerSesion, type Sesion } from './api/auth';
import { ClientesPage } from './pages/ClientesPage';
import { ConsultasPendientesPage } from './pages/ConsultasPendientesPage';
import { DisponibilidadPage } from './pages/DisponibilidadPage';
import { ReservasPage } from './pages/ReservasPage';
import { SolicitudVehiculoPage } from './pages/SolicitudVehiculoPage';
import { VehiculosPage } from './pages/VehiculosPage';
import { LoginPage } from './pages/LoginPage';

const administrationItems = [
  { id: 'vehiculos', label: 'Vehiculos', icon: CarFront },
  { id: 'clientes', label: 'Clientes', icon: Users },
  { id: 'reservas', label: 'Reservas', icon: CalendarDays },
];

const queryItems = [
  { id: 'disponibilidad', label: 'Disponibilidad', icon: Search },
  { id: 'consulta-reservas', label: 'Consulta de reservas', icon: ClipboardList },
  { id: 'historial', label: 'Historial de alquileres', icon: ClipboardList },
];

function App() {
  const [sesion, setSesion] = useState<Sesion | null>(() => obtenerSesion());
  const [activeModule, setActiveModule] = useState('vehiculos');
  const esAdmin = sesion?.usuario.rol === 'ADMIN';
  const itemsAdministracion = esAdmin
    ? [...administrationItems, { id: 'vehiculo-propio', label: 'Solicitud externa', icon: CarFront }]
    : [
      { id: 'reservas', label: 'Reservas', icon: CalendarDays },
      { id: 'vehiculo-propio', label: 'Mi vehiculo', icon: CarFront },
    ];
  const itemsConsulta = esAdmin ? queryItems : queryItems.filter((item) => item.id !== 'consulta-reservas');

  if (!sesion) return <LoginPage onLogin={(nuevaSesion) => {
    setSesion(nuevaSesion);
    setActiveModule(nuevaSesion.usuario.rol === 'ADMIN' ? 'vehiculos' : 'disponibilidad');
  }} />;

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
          {itemsAdministracion.map(({ id, label, icon: Icon }) => (
            <button
              aria-current={activeModule === id ? 'page' : undefined}
              className={activeModule === id ? 'nav-item nav-item-active' : 'nav-item'}
              key={label}
              onClick={() => setActiveModule(id)}
              type="button"
            >
              <Icon aria-hidden="true" size={18} />
              {label}
            </button>
          ))}

          <span className="nav-section-label">Consultas</span>
          {itemsConsulta.map(({ id, label, icon: Icon }) => (
            <button
              aria-current={activeModule === id ? 'page' : undefined}
              className={activeModule === id ? 'nav-item nav-item-active' : 'nav-item'}
              key={label}
              onClick={() => setActiveModule(id)}
              type="button"
            >
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
          <button className="secondary-button" onClick={() => { cerrarSesion(); setSesion(null); }} type="button">
            Salir ({sesion.usuario.username})
          </button>
        </header>

        {activeModule === 'vehiculos' && <VehiculosPage />}
        {activeModule === 'clientes' && <ClientesPage />}
        {activeModule === 'reservas' && <ReservasPage />}
        {activeModule === 'vehiculo-propio' && <SolicitudVehiculoPage />}
        {activeModule === 'disponibilidad' && <DisponibilidadPage />}
        {activeModule === 'consulta-reservas' && <ConsultasPendientesPage tipo="reservas" />}
        {activeModule === 'historial' && <ConsultasPendientesPage tipo="historial" />}
        {activeModule === 'inicio' && (
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
        )}
      </main>
    </div>
  );
}

export default App;
