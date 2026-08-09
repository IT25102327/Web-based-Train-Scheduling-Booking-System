/**
 * @file Route.model.js
 * @description Sequelize model definition for Route entity (Train stations, distances, and paths).
 */

const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db.config');

const Route = sequelize.define('Route', {
  id: {
    type: DataTypes.INTEGER,
    primaryKey: true,
    autoIncrement: true,
  },
  startStation: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  endStation: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  distanceKm: {
    type: DataTypes.FLOAT,
    allowNull: false,
  },
});

module.exports = Route;
