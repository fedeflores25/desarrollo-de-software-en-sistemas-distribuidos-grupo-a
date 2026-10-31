const express = require('express');
const jwt = require('jsonwebtoken');

const customerService = require('../../services/customerService');
const { jwtSecret } = require('../../middleware/auth');

const router = express.Router();

router.post('/login', async (req, res) => {
  const { username, password } = req.body || {};

  if (!username || !password) {
    return res.status(400).json({ error: 'Username y password son obligatorios.' });
  }

  try {
    const autenticacion = await customerService.authenticateUser(username, password);
    if (!autenticacion.autenticado) {
      return res.status(401).json({ error: 'Credenciales invalidas.' });
    }

    const rol = autenticacion.rol === 'USER_ROLE_ADMIN' ? 'ADMIN' : 'CLIENTE';
    const clienteId = autenticacion.cliente_id ? Number(autenticacion.cliente_id) : null;
    const token = jwt.sign({ username: autenticacion.username, rol, clienteId }, jwtSecret(), {
      expiresIn: '8h',
    });

    return res.json({ token, usuario: { username: autenticacion.username, rol, clienteId } });
  } catch (error) {
    return res.status(502).json({ error: error.details || 'No fue posible autenticar el usuario.' });
  }
});

module.exports = router;
