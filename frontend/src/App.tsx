import { RouterProvider } from 'react-router';
import { Toaster } from '@/components/ui/sonner';
import { router } from '@/app/router/router';

export default function App() {
  return (
    <>
      <RouterProvider router={router} />
      <Toaster position="bottom-right" richColors />
    </>
  );
}
