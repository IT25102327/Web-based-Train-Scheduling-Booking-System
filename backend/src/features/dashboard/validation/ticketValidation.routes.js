/**
 * @file ticketValidation.routes.js
 * @description Express router for ticket validation endpoints used by conductors.
 */

const express = require('express');
const ticketValidationController = require('./ticketValidation.controller');
const { authenticateToken } = require('../../../middleware/auth.middleware');
const { authorizeRoles } = require('../../../middleware/role.middleware');

const router = express.Router();

router.post('/verify', authenticateToken, authorizeRoles('conductor', 'admin'), ticketValidationController.validateTicket);

module.exports = router;
