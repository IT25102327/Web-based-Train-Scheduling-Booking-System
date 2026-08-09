/**
 * @file role.middleware.js
 * @description Role-based authorization middleware to restrict endpoint access by user roles.
 */

/**
 * Middleware factory to authorize requests based on allowed user roles.
 * @param {...string} allowedRoles - List of roles permitted to access the route.
 * @returns {Function} Express middleware function.
 */
const authorizeRoles = (...allowedRoles) => {
  return (req, res, next) => {
    // TODO: Implement role validation logic against authenticated user
    next();
  };
};

module.exports = { authorizeRoles };
