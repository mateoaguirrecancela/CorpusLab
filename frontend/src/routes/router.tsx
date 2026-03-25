import { createBrowserRouter, Navigate } from 'react-router'
import RootLayout from '@/layouts/RootLayout'
import AuthLayout from '@/modules/auth/layouts/AuthLayout'
import SignUpPage from '@/modules/auth/pages/SignUpPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      {
        index: true,
        element: <Navigate replace to="/auth/signup" />,
      },
      {
        path: 'auth',
        element: <AuthLayout />,
        children: [
          {
            path: 'signup',
            element: <SignUpPage />,
          },
        ],
      },
    ],
  },
])
