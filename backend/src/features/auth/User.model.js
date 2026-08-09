/**
 * @file User.model.js
 * @description Sequelize model definition for User entity (Authentication & Profile details).
 */

const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db.config');

const User = sequelize.define('User', {
  id: {
    type: DataTypes.INTEGER,
    primaryKey: true,
    autoIncrement: true,
  },
  name: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  email: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true,
  },
  password: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  role: {
    type: DataTypes.ENUM('passenger', 'admin', 'conductor'),
    defaultValue: 'passenger',
  },
});

module.exports = User;
