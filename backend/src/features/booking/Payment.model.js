/**
 * @file Payment.model.js
 * @description Sequelize model definition for Payment entity (Transaction logs for bookings).
 */

const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db.config');

const Payment = sequelize.define('Payment', {
  id: {
    type: DataTypes.INTEGER,
    primaryKey: true,
    autoIncrement: true,
  },
  bookingId: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
  transactionId: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true,
  },
  amount: {
    type: DataTypes.DECIMAL(10, 2),
    allowNull: false,
  },
  status: {
    type: DataTypes.ENUM('completed', 'failed', 'refunded'),
    defaultValue: 'completed',
  },
  paymentMethod: {
    type: DataTypes.STRING,
    allowNull: false,
  },
});

module.exports = Payment;
