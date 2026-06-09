import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';
import commonEn from '@/shared/locales/en.json';
import commonEs from '@/shared/locales/es.json';
import commonGl from '@/shared/locales/gl.json';
import en from '@/modules/auth/locales/en.json';
import es from '@/modules/auth/locales/es.json';
import gl from '@/modules/auth/locales/gl.json';
import dashboardEn from '@/modules/dashboard/locales/en.json';
import dashboardEs from '@/modules/dashboard/locales/es.json';
import dashboardGl from '@/modules/dashboard/locales/gl.json';
import notificationEn from '@/modules/notification/locales/en.json';
import notificationEs from '@/modules/notification/locales/es.json';
import notificationGl from '@/modules/notification/locales/gl.json';
import projectEn from '@/modules/project/locales/en.json';
import projectEs from '@/modules/project/locales/es.json';
import projectGl from '@/modules/project/locales/gl.json';
import rgEn from '@/modules/researchgroup/locales/en.json';
import rgEs from '@/modules/researchgroup/locales/es.json';
import rgGl from '@/modules/researchgroup/locales/gl.json';

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: {
        translation: {
          ...commonEn,
          ...en,
          ...dashboardEn,
          ...notificationEn,
          ...projectEn,
          ...rgEn,
        },
      },
      es: {
        translation: {
          ...commonEs,
          ...es,
          ...dashboardEs,
          ...notificationEs,
          ...projectEs,
          ...rgEs,
        },
      },
      gl: {
        translation: {
          ...commonGl,
          ...gl,
          ...dashboardGl,
          ...notificationGl,
          ...projectGl,
          ...rgGl,
        },
      },
    },
    fallbackLng: 'en',
    supportedLngs: ['en', 'es', 'gl'],
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ['navigator', 'htmlTag'],
      caches: [],
    },
  });

type LanguageSource = Readonly<{
  language?: string;
  resolvedLanguage?: string;
}>;

export function getResolvedLanguage(languageSource: LanguageSource = i18n): string {
  return languageSource.resolvedLanguage ?? languageSource.language ?? 'en';
}

export default i18n;
