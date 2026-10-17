import { createBrowserRouter } from 'react-router'
import AppLayout from './layout/AppLayout.tsx'
import HomePage from './pages/HomePage.tsx'
import PlaceholderPage from './pages/PlaceholderPage.tsx'

// 8-1 정보 구조: 홈(추천) · 기록하기 · 내 노트 · 원두 찾기 · 내 취향
export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'notes/new', element: <PlaceholderPage title="기록하기" phase="Phase 2" /> },
      { path: 'notes', element: <PlaceholderPage title="내 노트" phase="Phase 2" /> },
      { path: 'beans', element: <PlaceholderPage title="원두 찾기" phase="Phase 1" /> },
      { path: 'taste', element: <PlaceholderPage title="내 취향" phase="Phase 4" /> },
      { path: '*', element: <PlaceholderPage title="페이지를 찾을 수 없어요" /> },
    ],
  },
])
