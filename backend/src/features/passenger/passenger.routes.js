/**
 * @file passenger.routes.js
 * @description Express router for passenger profile and favorite route endpoints.
 */

const express = require('express');
const passengerController = require('./passenger.controller');
const passengerValidator = require('./passenger.validator');
const { authenticateToken } = require('../../middleware/auth.middleware');

const router = express.Router();

router.get('/profile', authenticateToken, passengerController.getProfile);
router.put('/profile', authenticateToken, passengerValidator.validateUpdateProfile, passengerController.updateProfile);

module.exports = router;
