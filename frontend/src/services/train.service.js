/**
 * Train Service
 * Handles train, route, and schedule API integrations.
 */
import apiClient from './api';

export const trainService = {
  searchTrains: async (params) => apiClient(`/trains/search?${new URLSearchParams(params)}`),
  getSchedules: async () => apiClient('/schedules'),
  getTrainById: async (id) => apiClient(`/trains/${id}`),
};

export default trainService;
