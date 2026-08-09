/**
 * @file notification.routes.js
 * @description Express router for user notification endpoints.
 */

const express = require('express');
const notificationController = require('./notification.controller');
const { authenticateToken } = require('../../middleware/auth.middleware');

const router = express.Router();

router.get('/', authenticateToken, notificationController.getUserNotifications);

module.exports = router;
