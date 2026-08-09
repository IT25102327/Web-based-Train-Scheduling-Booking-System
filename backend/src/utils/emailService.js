/**
 * @file emailService.js
 * @description Utility service for sending transactional emails (notifications, booking confirmations) using Nodemailer.
 */

/**
 * Sends an email with given recipient, subject, and body content.
 * @param {string} to - Recipient email address.
 * @param {string} subject - Email subject line.
 * @param {string} body - Email body content (HTML/Text).
 * @returns {Promise<boolean>} Success indicator.
 */
const sendEmail = async (to, subject, body) => {
  // TODO: Implement email sending logic using nodemailer
  return true;
};

module.exports = {
  sendEmail,
};
