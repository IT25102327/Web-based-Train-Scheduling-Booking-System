/**
 * Auth Service
 * Handles authentication requests including login, registration, and password recovery.
 */
import apiClient from './api';

export const authService = {
  login: async (credentials) => apiClient('/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),
  register: async (userData) => apiClient('/auth/register', { method: 'POST', body: JSON.stringify(userData) }),
  forgotPassword: async (email) => apiClient('/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
};

export default authService;
