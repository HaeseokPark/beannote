import { NavLink, Outlet } from 'react-router'

const TABS = [
  { to: '/', label: '홈', icon: '☕', end: true },
  { to: '/notes/new', label: '기록하기', icon: '＋', end: false },
  { to: '/notes', label: '내 노트', icon: '📓', end: true },
  { to: '/beans', label: '원두 찾기', icon: '🔍', end: false },
  { to: '/taste', label: '내 취향', icon: '📊', end: false },
]

export default function AppLayout() {
  return (
    <div className="app">
      <header className="app-header">
        <span className="brand">Beanote</span>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
      <nav className="tab-bar" aria-label="주요 메뉴">
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            end={tab.end}
            className={({ isActive }) => (isActive ? 'tab active' : 'tab')}
          >
            <span className="tab-icon" aria-hidden="true">
              {tab.icon}
            </span>
            <span className="tab-label">{tab.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
