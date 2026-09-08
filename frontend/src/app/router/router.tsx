import { createBrowserRouter, Navigate } from 'react-router';
import RootLayout from '@/components/layout/RootLayout';
import AuthLayout from '@/modules/auth/layouts/AuthLayout';
import ForgotPasswordPage from '@/modules/auth/pages/ForgotPasswordPage';
import LogInPage from '@/modules/auth/pages/LogInPage';
import OAuthRedirectPage from '@/modules/auth/pages/OAuthRedirectPage';
import ResetPasswordPage from '@/modules/auth/pages/ResetPasswordPage';
import SignUpPage from '@/modules/auth/pages/SignUpPage';
import AppShell from '@/components/layout/AppShell';
import DashboardPage from '@/modules/dashboard/pages/DashboardPage';
import ProfilePage from '@/modules/auth/profile/pages/ProfilePage';
import ProjectCreatePage from '@/modules/project/setup/pages/ProjectCreatePage';
import ResearchGroupDetailPage from '@/modules/researchgroup/pages/ResearchGroupDetailPage';
import ResearchGroupsPage from '@/modules/researchgroup/pages/ResearchGroupsPage';
import { PublicOnly, RequireSession } from '@/app/router/guards';
import ProjectsPage from '@/modules/project/core/pages/ProjectsPage';
import ProjectDetailPage from '@/modules/project/core/pages/ProjectDetailPage';
import ProjectAnnotationPage from '@/modules/project/annotation/pages/ProjectAnnotationPage';
import NotFoundPage from '@/app/router/NotFoundPage';

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
        path: 'oauth2/redirect',
        element: <OAuthRedirectPage />,
      },
      {
        path: 'home',
        element: (
          <RequireSession>
            <AppShell />
          </RequireSession>
        ),
        children: [
          {
            index: true,
            element: <DashboardPage />,
          },
          {
            path: 'research-groups',
            element: <ResearchGroupsPage />,
          },
          {
            path: 'research-groups/:id',
            element: <ResearchGroupDetailPage />,
          },
          {
            path: 'projects',
            element: <ProjectsPage />,
          },
          {
            path: 'projects/create',
            element: <ProjectCreatePage />,
          },
          {
            path: 'projects/:projectId',
            element: <ProjectDetailPage />,
          },
          {
            path: 'projects/:projectId/annotate',
            element: <ProjectAnnotationPage />,
          },
          {
            path: 'profile',
            element: <ProfilePage />,
          },
          {
            path: '*',
            element: <NotFoundPage embedded />,
          },
        ],
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]);
