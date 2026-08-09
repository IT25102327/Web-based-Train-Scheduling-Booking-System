/**
 * @file booking.routes.js
 * @description Express router for train booking and payment endpoints.
 */

const express = require('express');
const bookingController = require('./booking.controller');
const bookingValidator = require('./booking.validator');
const { authenticateToken } = require('../../middleware/auth.middleware');

const router = express.Router();

router.post('/', authenticateToken, bookingValidator.validateCreateBooking, bookingController.createBooking);
router.get('/:id', authenticateToken, bookingController.getBookingById);

module.exports = router;
