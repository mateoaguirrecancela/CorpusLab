import { type ReactNode } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import '@/app/config/i18n';
import { queryClient } from '@/app/config/queryClient';

type AppProvidersProps = Readonly<{
  children: ReactNode;
}>;

export function AppProviders({ children }: AppProvidersProps) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
