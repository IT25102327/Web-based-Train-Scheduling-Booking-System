/**
 * Dashboard Service
 * Serves analytics, revenue figures, and ticket validation metrics for admins/conductors.
 */
import apiClient from './api';

export const dashboardService = {
  getAnalytics: async () => apiClient('/dashboard/analytics'),
  validateTicket: async (ticketCode) => apiClient('/dashboard/validate-ticket', { method: 'POST', body: JSON.stringify({ ticketCode }) }),
};

export default dashboardService;
