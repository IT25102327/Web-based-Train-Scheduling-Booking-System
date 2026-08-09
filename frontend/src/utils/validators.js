/**
 * Validation Helpers
 * Client-side validators for form fields (email, phone, passenger inputs).
 */
export const isValidEmail = (email) => {
  const emailRegex = /^[^s@]+@[^s@]+.[^s@]+$/;
  return emailRegex.test(email);
};

export const isValidPhone = (phone) => {
  const phoneRegex = /^\+?[0-9]{9,15}$/;
  return phoneRegex.test(phone);
};
