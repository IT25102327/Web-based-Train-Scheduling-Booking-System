/**
 * @file index.js
 * @description Central router aggregator that mounts feature route modules.
 */

const express = require('express');

const authRoutes = require('./auth/auth.routes');
const passengerRoutes = require('./passenger/passenger.routes');
const trainRoutes = require('./trains/train.routes');
const bookingRoutes = require('./booking/booking.routes');
const notificationRoutes = require('./notifications/notification.routes');
const dashboardRoutes = require('./dashboard/dashboard.routes');

const router = express.Router();

router.use('/auth', authRoutes);
router.use('/passengers', passengerRoutes);
router.use('/trains', trainRoutes);
router.use('/bookings', bookingRoutes);
router.use('/notifications', notificationRoutes);
router.use('/dashboard', dashboardRoutes);

module.exports = router;
