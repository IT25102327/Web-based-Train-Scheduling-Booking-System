/**
 * @file auth.routes.js
 * @description Express router for authentication endpoints.
 */

const express = require('express');
const authController = require('./auth.controller');
const authValidator = require('./auth.validator');

const router = express.Router();

router.post('/register', authValidator.validateRegister, authController.register);
router.post('/login', authValidator.validateLogin, authController.login);

module.exports = router;
