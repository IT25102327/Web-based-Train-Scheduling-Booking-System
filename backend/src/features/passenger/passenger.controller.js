/**
 * @file passenger.controller.js
 * @description Controller handling passenger profile management and saved route preferences.
 */

const passengerService = require('./passenger.service');

const getProfile = async (req, res, next) => {
  try {
    // TODO: Get passenger profile logic
    res.status(200).json({ message: 'Passenger profile retrieved' });
  } catch (error) {
    next(error);
  }
};

const updateProfile = async (req, res, next) => {
  try {
    // TODO: Update passenger profile logic
    res.status(200).json({ message: 'Passenger profile updated' });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getProfile,
  updateProfile,
};
