import { useState, type FormEvent } from 'react';
import { iniciarSesion, type Sesion } from '../api/auth';

interface LoginPageProps { onLogin: (sesion: Sesion) => void; }

export function LoginPage({ onLogin }: LoginPageProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCargando(true); setError('');
    try { onLogin(await iniciarSesion(username, password)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'No fue posible iniciar sesion.'); }
    finally { setCargando(false); }
  }

  return <main className="login-page">
    <form className="login-card" onSubmit={(event) => void submit(event)}>
      <p className="eyebrow">Rentar</p><h1>Iniciar sesion</h1>
      <p>Accede con el perfil administrador o cliente.</p>
      <label className="form-field"><span>Usuario</span><input value={username} onChange={(e) => setUsername(e.target.value)} required autoComplete="username" /></label>
      <label className="form-field"><span>Contraseña</span><input value={password} onChange={(e) => setPassword(e.target.value)} required type="password" autoComplete="current-password" /></label>
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="primary-button" disabled={cargando} type="submit">{cargando ? 'Ingresando...' : 'Ingresar'}</button>
    </form>
  </main>;
}
