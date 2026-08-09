/**
 * Application Constants
 * Holds global configuration values, role definitions, and status codes.
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000/api/v1';

export const ROLES = {
  PASSENGER: 'PASSENGER',
  ADMIN: 'ADMIN',
  CONDUCTOR: 'CONDUCTOR',
};

export const TICKET_STATUS = {
  BOOKED: 'BOOKED',
  CANCELLED: 'CANCELLED',
  USED: 'USED',
  EXPIRED: 'EXPIRED',
};

export const TRAIN_STATUS = {
  ON_TIME: 'ON_TIME',
  DELAYED: 'DELAYED',
  CANCELLED: 'CANCELLED',
};
