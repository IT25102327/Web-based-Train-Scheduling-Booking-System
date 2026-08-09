/**
 * @file app.config.js
 * @description General application configuration constants sourced from environment variables.
 */

require('dotenv').config();

module.exports = {
  PORT: process.env.PORT || 5000,
  NODE_ENV: process.env.NODE_ENV || 'development',
  JWT_SECRET: process.env.JWT_SECRET || 'default_secret',
  JWT_EXPIRES_IN: process.env.JWT_EXPIRES_IN || '1d',
  UPLOAD_DIR: process.env.UPLOAD_DIR || 'uploads/',
};
