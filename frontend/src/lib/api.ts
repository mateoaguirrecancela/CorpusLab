import axios from 'axios';
import i18n from '@/lib/i18n';
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
  const language = i18n.resolvedLanguage ?? i18n.language ?? 'en';

  config.headers = config.headers ?? {};
  config.headers['Accept-Language'] = language;

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});
