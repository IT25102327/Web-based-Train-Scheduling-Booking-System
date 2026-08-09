/**
 * @file dashboard.routes.js
 * @description Express router for dashboard stats and ticket validation sub-routes.
 */

const express = require('express');
const dashboardController = require('./dashboard.controller');
const ticketValidationRoutes = require('./validation/ticketValidation.routes');
const { authenticateToken } = require('../../middleware/auth.middleware');
const { authorizeRoles } = require('../../middleware/role.middleware');

const router = express.Router();

router.get('/stats', authenticateToken, authorizeRoles('admin', 'conductor'), dashboardController.getDashboardStats);
router.use('/validation', ticketValidationRoutes);

module.exports = router;
