import { createBrowserRouter, Navigate } from 'react-router'
import { RequireAuth } from './auth/session.tsx'
import { tokenStore } from './auth/tokenStore.ts'
import { AppLayout, PlainLayout } from './layout/AppLayout.tsx'
import { LoginPage, SignupPage } from './pages/AuthPages.tsx'
import { BeanDetailPage } from './pages/BeanDetailPage.tsx'
import { BeanRegisterPage } from './pages/BeanRegisterPage.tsx'
import { BeansPage } from './pages/BeansPage.tsx'
import { HomePage } from './pages/HomePage.tsx'
import { LandingPage } from './pages/LandingPage.tsx'
import { MyNotesPage } from './pages/MyNotesPage.tsx'
import { NoteDetailPage } from './pages/NoteDetailPage.tsx'
import { NoteWritePage } from './pages/NoteWritePage.tsx'
import { OnboardingPage } from './pages/OnboardingPage.tsx'
import { TastePage } from './pages/TastePage.tsx'

function RootRedirect() {
  return <Navigate to={tokenStore.get() ? '/home' : '/welcome'} replace />
}

export const router = createBrowserRouter([
  { index: true, element: <RootRedirect /> },
  {
    element: <PlainLayout />,
    children: [
      { path: 'welcome', element: <LandingPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'signup', element: <SignupPage /> },
    ],
  },
  {
    element: <RequireAuth />,
    children: [
      { element: <PlainLayout />, children: [{ path: 'onboarding', element: <OnboardingPage /> }] },
      {
        element: <AppLayout />,
        children: [
          { path: 'home', element: <HomePage /> },
          { path: 'notes', element: <MyNotesPage /> },
          { path: 'notes/new', element: <NoteWritePage /> },
          { path: 'notes/:id', element: <NoteDetailPage /> },
          { path: 'notes/:id/edit', element: <NoteWritePage /> },
          { path: 'beans', element: <BeansPage /> },
          { path: 'beans/new', element: <BeanRegisterPage /> },
          { path: 'beans/:id', element: <BeanDetailPage /> },
          { path: 'taste', element: <TastePage /> },
        ],
      },
    ],
  },
  { path: '*', element: <RootRedirect /> },
])
