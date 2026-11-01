const express = require('express');
const router = express.Router();
const vehicleService = require('../../services/vehicleService');
const { requireRoles } = require('../../middleware/auth');

router.post('/solicitudes-externas', requireRoles('ADMIN', 'CLIENTE'), async (req, res) => {
    try {
        const clienteId = req.user.rol === 'CLIENTE' ? req.user.clienteId : req.body.cliente_id;
        const solicitud = await vehicleService.createExternalVehicleApplication({
            ...req.body,
            cliente_id: clienteId,
        });
        res.status(202).json(solicitud);
    } catch (error) {
        res.status(400).json({ error: error.details || 'No fue posible registrar la solicitud.' });
    }
});

router.get('/solicitudes-externas/:id', requireRoles('ADMIN', 'CLIENTE'), async (req, res) => {
    try {
        const solicitud = await vehicleService.getExternalVehicleApplication(req.params.id);
        if (req.user.rol === 'CLIENTE' && Number(solicitud.cliente_id) !== Number(req.user.clienteId)) {
            return res.status(403).json({ error: 'No tiene permisos para consultar esta solicitud.' });
        }
        return res.json(solicitud);
    } catch (error) {
        return res.status(error.code === 5 ? 404 : 400).json({ error: error.details || 'Solicitud no encontrada.' });
    }
});

router.use(requireRoles('ADMIN'));

router.get('/', async (req, res) => {
    try {
        const soloActivos = req.query.soloActivos === 'true' ? true : null;
        const vehiculos = await vehicleService.listVehicles({}, [], soloActivos);
        res.json(vehiculos);
    } catch (error) {
        res.status(500).json({ error: error.details || 'Error interno' });
    }
});

// GET /api/vehiculos/patente/:patente
router.get('/patente/:patente', async (req, res) => {
    try {
        const vehiculo = await vehicleService.getVehicleByPatente(req.params.patente);
        res.json(vehiculo);
    } catch (error) {
        if (error.code === 5) return res.status(404).json({ error: 'Vehículo no encontrado' });
        res.status(500).json({ error: error.details });
    }
});

// GET /api/vehiculos/:id
router.get('/:id', async (req, res) => {
    try {
        const vehiculo = await vehicleService.getVehicle(req.params.id);
        res.json(vehiculo);
    } catch (error) {
        if (error.code === 5) return res.status(404).json({ error: 'Vehículo no encontrado' });
        res.status(500).json({ error: error.details });
    }
});

// POST /api/vehiculos
router.post('/', async (req, res) => {
    try {
        const nuevoVehiculo = await vehicleService.createVehicle(req.body);
        res.status(201).json(nuevoVehiculo);
    } catch (error) {
        if (error.code === 6) return res.status(409).json({ error: 'La patente ya está registrada' });
        res.status(400).json({ error: error.details });
    }
});

// PUT /api/vehiculos/:id
router.put('/:id', async (req, res) => {
    try {
        const data = { id: req.params.id, ...req.body };
        const vehiculoActualizado = await vehicleService.updateVehicle(data);
        res.json(vehiculoActualizado);
    } catch (error) {
        res.status(400).json({ error: error.details });
    }
});

// DELETE /api/vehiculos/:id
router.delete('/:id', async (req, res) => {
    try {
        const vehiculoBaja = await vehicleService.deactivateVehicle(req.params.id);
        res.json(vehiculoBaja);
    } catch (error) {
        res.status(500).json({ error: error.details });
    }
});

// PATCH /api/vehiculos/:id/estado (Para cambiar solo el estado)
router.patch('/:id/estado', async (req, res) => {
    try {
        const vehiculoActualizado = await vehicleService.updateVehicleStatus(req.params.id, req.body.estado);
        res.json(vehiculoActualizado);
    } catch (error) {
        if (error.code === 9) return res.status(409).json({ error: 'Operación no permitida por el estado' });
        res.status(400).json({ error: error.details });
    }
});

module.exports = router;
