const express = require('express');
const router = express.Router();
const customerService = require('../../services/customerService');
const { requireRoles } = require('../../middleware/auth');

router.use(requireRoles('ADMIN'));

// GET /api/clientes (Lista todos o filtra por activos)
router.get('/', async (req, res) => {
    try {
        const soloActivos = req.query.soloActivos === 'true' ? true : null;
        const clientes = await customerService.listCustomers([], soloActivos);
        res.json(clientes);
    } catch (error) {
        res.status(500).json({ error: error.details || 'Error interno' });
    }
});

// GET /api/clientes/:id (Busca uno solo)
router.get('/:id', async (req, res) => {
    try {
        const cliente = await customerService.getCustomer(req.params.id);
        res.json(cliente);
    } catch (error) {
        // En gRPC el código 5 significa NOT_FOUND
        if (error.code === 5) return res.status(404).json({ error: 'Cliente no encontrado' });
        res.status(500).json({ error: error.details });
    }
});

// POST /api/clientes (Crea un cliente)
router.post('/', async (req, res) => {
    try {
        const nuevoCliente = await customerService.createCustomer(req.body);
        res.status(201).json(nuevoCliente);
    } catch (error) {
        if (error.code === 6) return res.status(409).json({ error: 'El documento o email ya existe' });
        res.status(400).json({ error: error.details });
    }
});

// PUT /api/clientes/:id (Actualiza un cliente)
router.put('/:id', async (req, res) => {
    try {
        const data = { id: req.params.id, ...req.body };
        const clienteActualizado = await customerService.updateCustomer(data);
        res.json(clienteActualizado);
    } catch (error) {
        res.status(400).json({ error: error.details });
    }
});

// DELETE /api/clientes/:id (Baja lógica)
router.delete('/:id', async (req, res) => {
    try {
        const clienteBaja = await customerService.deactivateCustomer(req.params.id);
        res.json(clienteBaja);
    } catch (error) {
        res.status(500).json({ error: error.details });
    }
});

module.exports = router;
