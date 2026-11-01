const express = require('express');
const router = express.Router();
const rentalService = require('../../services/rentalService');

// POST /api/reservas (Alta de Reserva - Orquestada)
router.post('/', async (req, res) => {
    try {
        const clienteId = req.user.rol === 'CLIENTE' ? req.user.clienteId : req.body.cliente_id;
        const nuevaReserva = await rentalService.createReservation({ ...req.body, cliente_id: clienteId });
        res.status(201).json(nuevaReserva);
    } catch (error) {
        
        res.status(400).json({ error: error.message || error.details || "Error al crear reserva" });
    }
});

// PATCH /api/reservas/:id/cancelar (Cancelación de Reserva)
router.patch('/:id/cancelar', async (req, res) => {
    try {
        const reserva = await rentalService.getReservation(req.params.id);
        if (req.user.rol === 'CLIENTE' && Number(reserva.cliente_id) !== Number(req.user.clienteId)) {
            return res.status(403).json({ error: 'No tiene permisos para cancelar esta reserva.' });
        }
        const reservaCancelada = await rentalService.cancelReservation(req.params.id);
        res.json(reservaCancelada);
    } catch (error) {
        if (error.code === 9) return res.status(409).json({ error: 'La reserva no se puede cancelar en este estado' });
        res.status(400).json({ error: error.details });
    }
});

module.exports = router;
