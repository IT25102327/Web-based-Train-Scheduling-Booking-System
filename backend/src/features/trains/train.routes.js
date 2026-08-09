/**
 * @file train.routes.js
 * @description Express router for train management, route, and schedule endpoints.
 */

const express = require('express');
const trainController = require('./train.controller');
const trainValidator = require('./train.validator');

const router = express.Router();

router.get('/', trainController.getAllTrains);
router.get('/schedules', trainController.getSchedules);

module.exports = router;
