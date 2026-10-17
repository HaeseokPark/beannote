import { useEffect } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router'
import { Icon, type IconName } from '../components/Icon.tsx'

/** 화면이 바뀌면 맨 위에서 시작한다 */
function ScrollToTop() {
  const { pathname } = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return null
}

const TABS: { to: string; label: string; icon: IconName; end?: boolean }[] = [
  { to: '/home', label: '홈', icon: 'cup' },
  { to: '/notes', label: '내 노트', icon: 'notebook', end: true },
  { to: '/notes/new', label: '기록하기', icon: 'plus' },
  { to: '/beans', label: '원두 찾기', icon: 'search' },
  { to: '/taste', label: '내 취향', icon: 'taste' },
]

/** 8-1 정보 구조: 하단 탭 (가운데 기록하기 버튼) */
export function AppLayout() {
  return (
    <div className="shell">
      <ScrollToTop />
      <Outlet />
      <nav className="tabbar" aria-label="주요 메뉴">
        {TABS.map((tab) =>
          tab.to === '/notes/new' ? (
            <NavLink key={tab.to} to={tab.to} className="tab-record" aria-label="기록하기">
              <Icon name="plus" size={26} stroke={2.2} />
            </NavLink>
          ) : (
            <NavLink key={tab.to} to={tab.to} end={tab.end} className={({ isActive }) => `tab ${isActive ? 'active' : ''}`}>
              <Icon name={tab.icon} />
              <span>{tab.label}</span>
            </NavLink>
          ),
        )}
      </nav>
    </div>
  )
}

/** 탭 없는 화면 (랜딩·로그인·설문) */
export function PlainLayout() {
  return (
    <div className="shell">
      <ScrollToTop />
      <Outlet />
    </div>
  )
}
