/**
 * Booking Service
 * Manages ticket reservations, payment processing, and e-tickets.
 */
import apiClient from './api';

export const bookingService = {
  createBooking: async (bookingData) => apiClient('/bookings', { method: 'POST', body: JSON.stringify(bookingData) }),
  getBookingHistory: async () => apiClient('/bookings/history'),
  processPayment: async (paymentData) => apiClient('/payments/process', { method: 'POST', body: JSON.stringify(paymentData) }),
  getTicket: async (id) => apiClient(`/tickets/${id}`),
};

export default bookingService;
