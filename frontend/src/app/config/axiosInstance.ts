import axios from 'axios';
import i18n, { getResolvedLanguage } from '@/app/config/i18n';
import { SESSION_AUTH_TOKEN_STORAGE_KEY } from '@/modules/auth/constants/session';
import { getSessionToken } from '@/modules/auth/services/sessionService';

export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = getSessionToken();
  const language = getResolvedLanguage(i18n);

  config.headers = config.headers ?? {};
  config.headers['Accept-Language'] = language;

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    const status =
      typeof error === 'object' && error !== null && 'response' in error
        ? (error as { response?: { status?: number } }).response?.status
        : undefined;

    if (status === 401) {
      localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY);

      if (globalThis.location.pathname !== '/auth/login') {
        globalThis.location.assign('/auth/login');
      }
    }

    throw error;
  },
);
