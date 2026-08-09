/**
 * @file smsService.js
 * @description Utility service for sending SMS notifications via Twilio API.
 */

/**
 * Sends an SMS message to a mobile number.
 * @param {string} to - Recipient phone number.
 * @param {string} message - Message text.
 * @returns {Promise<boolean>} Success indicator.
 */
const sendSMS = async (to, message) => {
  // TODO: Implement SMS sending logic using Twilio client
  return true;
};

module.exports = {
  sendSMS,
};
