const test = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');

const { authenticate, requireRoles, jwtSecret } = require('../src/middleware/auth');

function response() {
  return {
    code: 200,
    body: null,
    status(code) { this.code = code; return this; },
    json(body) { this.body = body; return this; },
  };
}

test('acepta un token JWT valido', () => {
  const token = jwt.sign({ rol: 'CLIENTE', clienteId: 1 }, jwtSecret());
  const req = { get: () => `Bearer ${token}` };
  const res = response();
  let nextCalled = false;

  authenticate(req, res, () => { nextCalled = true; });

  assert.equal(nextCalled, true);
  assert.equal(req.user.clienteId, 1);
});

test('rechaza una peticion sin token', () => {
  const req = { get: () => '' };
  const res = response();

  authenticate(req, res, () => assert.fail('No debe continuar'));

  assert.equal(res.code, 401);
});

test('rechaza un rol no autorizado', () => {
  const req = { user: { rol: 'CLIENTE' } };
  const res = response();

  requireRoles('ADMIN')(req, res, () => assert.fail('No debe continuar'));

  assert.equal(res.code, 403);
});
