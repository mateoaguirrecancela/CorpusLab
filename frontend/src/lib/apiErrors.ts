import i18n from '@/lib/i18n';

export function extractApiErrorMessage(error: unknown, fallbackMessage: string): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } })
      .response;
    return response?.data?.message ?? response?.data?.error ?? fallbackMessage;
  }

  return fallbackMessage;
}

export function extractTranslatedApiErrorMessage(error: unknown, fallbackKey: string): string {
  return extractApiErrorMessage(error, i18n.t(fallbackKey));
}
