/**
 * @file helpers.js
 * @description General utility and helper functions for common tasks across the application.
 */

/**
 * Formats a given date to standard ISO format string.
 * @param {Date|string} date 
 * @returns {string} Formatted date string
 */
const formatDate = (date) => {
  return new Date(date).toISOString();
};

module.exports = {
  formatDate,
};
