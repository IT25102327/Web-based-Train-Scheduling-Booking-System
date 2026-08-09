/**
 * @file auth.validator.js
 * @description Request validation middleware rules for authentication endpoints.
 */

const validateRegister = (req, res, next) => {
  // TODO: Validate registration request body
  next();
};

const validateLogin = (req, res, next) => {
  // TODO: Validate login request body
  next();
};

module.exports = {
  validateRegister,
  validateLogin,
};
