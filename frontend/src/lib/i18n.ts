import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';
import commonEn from '@/common/locales/en.json';
import commonEs from '@/common/locales/es.json';
import commonGl from '@/common/locales/gl.json';
import en from '@/modules/auth/locales/en.json';
import es from '@/modules/auth/locales/es.json';
import gl from '@/modules/auth/locales/gl.json';
import homeEn from '@/modules/home/locales/en.json';
import homeEs from '@/modules/home/locales/es.json';
import homeGl from '@/modules/home/locales/gl.json';

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: { ...commonEn, ...en, ...homeEn } },
      es: { translation: { ...commonEs, ...es, ...homeEs } },
      gl: { translation: { ...commonGl, ...gl, ...homeGl } },
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

export default i18n;
