import axios from 'axios'
import { SESSION_AUTH_TOKEN_STORAGE_KEY } from '@/modules/auth/constants/session'


export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(SESSION_AUTH_TOKEN_STORAGE_KEY)

  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})
