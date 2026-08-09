/**
 * Notification Service
 * Manages user notifications and real-time train status alerts.
 */
import apiClient from './api';

export const notificationService = {
  getNotifications: async () => apiClient('/notifications'),
  markAsRead: async (id) => apiClient(`/notifications/${id}/read`, { method: 'PATCH' }),
  getTrainStatus: async (trainId) => apiClient(`/status/train/${trainId}`),
};

export default notificationService;
