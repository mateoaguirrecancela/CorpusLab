import { createBrowserRouter, Navigate } from 'react-router'
import RootLayout from '@/layouts/RootLayout'
import AuthLayout from '@/modules/auth/layouts/AuthLayout'
import ForgotPasswordPage from '@/modules/auth/pages/ForgotPasswordPage'
import LogInPage from '@/modules/auth/pages/LogInPage'
import ResetPasswordPage from '@/modules/auth/pages/ResetPasswordPage'
import SignUpPage from '@/modules/auth/pages/SignUpPage'
import HomeLayout from '@/modules/home/layouts/HomeLayout'
import HomePage from '@/modules/home/pages/HomePage'
import { PublicOnly, RequireSession } from '@/routes/guards'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      {
        index: true,
        element: <Navigate replace to="/auth/login" />,
      },
      {
        path: 'auth',
        element: (
          <PublicOnly>
            <AuthLayout />
          </PublicOnly>
        ),
        children: [
          {
            path: 'login',
            element: <LogInPage />,
          },
          {
            path: 'signup',
            element: <SignUpPage />,
          },
          {
            path: 'forgot-password',
            element: <ForgotPasswordPage />,
          },
          {
            path: 'reset-password',
            element: <ResetPasswordPage />,
          },
        ],
      },
      {
        path: 'home',
        element: (
          <RequireSession>
            <HomeLayout />
          </RequireSession>
        ),
        children: [
          {
            index: true,
            element: <HomePage />,
          },
          {
            path: 'experiments',
            element: <HomePage />,
          },
          {
            path: 'laboratories',
            element: <HomePage />,
          },
        ],
      },
    ],
  },
])
