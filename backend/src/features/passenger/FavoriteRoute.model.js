/**
 * @file FavoriteRoute.model.js
 * @description Sequelize model definition for FavoriteRoute entity (Passenger saved favorite routes).
 */

const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db.config');

const FavoriteRoute = sequelize.define('FavoriteRoute', {
  id: {
    type: DataTypes.INTEGER,
    primaryKey: true,
    autoIncrement: true,
  },
  userId: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
  routeId: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
});

module.exports = FavoriteRoute;
