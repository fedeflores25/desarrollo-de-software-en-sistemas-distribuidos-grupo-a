const jwt = require('jsonwebtoken');

const jwtSecret = () => process.env.JWT_SECRET || 'rentar-demo-jwt-secret';

function authenticate(req, res, next) {
  const authorization = req.get('authorization') || '';
  const token = authorization.startsWith('Bearer ') ? authorization.slice(7) : null;

  if (!token) {
    return res.status(401).json({ error: 'Se requiere una sesion autenticada.' });
  }

  try {
    req.user = jwt.verify(token, jwtSecret());
    return next();
  } catch {
    return res.status(401).json({ error: 'La sesion es invalida o vencio.' });
  }
}

function getOptionalUser(req) {
  const authorization = req.get('authorization') || '';
  const token = authorization.startsWith('Bearer ') ? authorization.slice(7) : null;
  if (!token) return null;
  try {
    return jwt.verify(token, jwtSecret());
  } catch {
    return null;
  }
}

function requireRoles(...roles) {
  return (req, res, next) => {
    if (!req.user) return res.status(401).json({ error: 'Se requiere una sesion autenticada.' });
    if (!roles.includes(req.user.rol)) return res.status(403).json({ error: 'No tiene permisos para esta operacion.' });
    return next();
  };
}

module.exports = { authenticate, requireRoles, jwtSecret, getOptionalUser };
