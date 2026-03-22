import { createBrowserRouter } from 'react-router'
import RootLayout from '@/layouts/RootLayout'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      {
        index: true,
        element: (
          <div className="flex items-center justify-center min-h-screen">
            <h1 className="text-4xl font-bold">CorpusLab</h1>
          </div>
        ),
      },
    ],
  },
])
