/**
 * @file Schedule.model.js
 * @description Sequelize model definition for Train Schedule entity (Timetables, departure/arrival times).
 */

const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db.config');

const Schedule = sequelize.define('Schedule', {
  id: {
    type: DataTypes.INTEGER,
    primaryKey: true,
    autoIncrement: true,
  },
  trainId: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
  routeId: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
  departureTime: {
    type: DataTypes.DATE,
    allowNull: false,
  },
  arrivalTime: {
    type: DataTypes.DATE,
    allowNull: false,
  },
});

module.exports = Schedule;
