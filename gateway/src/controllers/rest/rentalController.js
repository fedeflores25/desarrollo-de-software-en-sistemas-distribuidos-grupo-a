const express = require('express');
const router = express.Router();
const rentalService = require('../../services/rentalService');

// POST /api/reservas (Alta de Reserva - Orquestada)
router.post('/', async (req, res) => {
    try {
        const nuevaReserva = await rentalService.createReservation(req.body);
        res.status(201).json(nuevaReserva);
    } catch (error) {
        
        res.status(400).json({ error: error.message || error.details || "Error al crear reserva" });
    }
});

// PATCH /api/reservas/:id/cancelar (Cancelación de Reserva)
router.patch('/:id/cancelar', async (req, res) => {
    try {
        const reservaCancelada = await rentalService.cancelReservation(req.params.id);
        res.json(reservaCancelada);
    } catch (error) {
        if (error.code === 9) return res.status(409).json({ error: 'La reserva no se puede cancelar en este estado' });
        res.status(400).json({ error: error.details });
    }
});

module.exports = router;