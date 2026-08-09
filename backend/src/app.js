/**
 * @file app.js
 * @description Express application setup. Configures middleware (CORS, body parser), mounts feature routes, and attaches error handling middleware.
 */

const express = require('express');
const cors = require('cors');
const routes = require('./features');
const { errorMiddleware } = require('./middleware/error.middleware');

const app = express();

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.use('/api', routes);

app.use(errorMiddleware);

module.exports = app;
