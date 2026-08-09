/**
 * @file dashboard.validator.js
 * @description Request validation rules for dashboard metrics endpoints.
 */

const validateDashboardQuery = (req, res, next) => {
  // TODO: Validate dashboard filter query options
  next();
};

module.exports = {
  validateDashboardQuery,
};
