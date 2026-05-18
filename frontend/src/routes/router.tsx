import { createBrowserRouter, Navigate } from 'react-router';
import RootLayout from '@/layouts/RootLayout';
import AuthLayout from '@/modules/auth/layouts/AuthLayout';
import ForgotPasswordPage from '@/modules/auth/pages/ForgotPasswordPage';
import LogInPage from '@/modules/auth/pages/LogInPage';
import OAuthRedirectPage from '@/modules/auth/pages/OAuthRedirectPage';
import ResetPasswordPage from '@/modules/auth/pages/ResetPasswordPage';
import SignUpPage from '@/modules/auth/pages/SignUpPage';
import HomeLayout from '@/modules/home/layouts/HomeLayout';
import HomePage from '@/modules/home/pages/HomePage';
import ProfilePage from '@/modules/home/pages/ProfilePage';
import ProjectCreatePage from '@/modules/project/pages/ProjectCreatePage';
import ResearchGroupDetailPage from '@/modules/researchgroup/pages/ResearchGroupDetailPage';
import ResearchGroupsPage from '@/modules/researchgroup/pages/ResearchGroupsPage';
import { PublicOnly, RequireSession } from '@/routes/guards';
import ProjectsPage from '@/modules/project/pages/ProjectsPage';
import ProjectDetailPage from '@/modules/project/pages/ProjectDetailPage';
import ProjectAnnotationPage from '@/modules/project/pages/ProjectAnnotationPage';
import NotFoundPage from '@/common/pages/NotFoundPage';

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
            <HomeLayout />
          </RequireSession>
        ),
        children: [
          {
            index: true,
            element: <HomePage />,
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
            path: 'laboratories',
            element: <HomePage />,
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
