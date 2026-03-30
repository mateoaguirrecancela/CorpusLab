import axios from 'axios';
import i18n from '@/lib/i18n';
import { SESSION_AUTH_TOKEN_STORAGE_KEY } from '@/modules/auth/constants/session';

export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
  const language = i18n.resolvedLanguage ?? i18n.language ?? 'en';

  config.headers = config.headers ?? {};
  config.headers['Accept-Language'] = language;

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});
