import type { ReactNode } from 'react'

export type IconName =
  | 'cup'
  | 'plus'
  | 'notebook'
  | 'search'
  | 'taste'
  | 'back'
  | 'close'
  | 'check'
  | 'edit'
  | 'trash'
  | 'lock'
  | 'globe'
  | 'chevron'
  | 'sparkle'
  | 'compass'
  | 'bean'
  | 'leaf'
  | 'logout'
  | 'info'
  | 'drop'
  | 'thermo'
  | 'timer'
  | 'scale'
  | 'users'
  | 'pour'
  | 'espresso'
  | 'latte'
  | 'americano'
  | 'refresh'

const PATHS: Record<IconName, ReactNode> = {
  cup: (
    <>
      <path d="M4 9h12v5a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5V9Z" />
      <path d="M16 10h1.5a2.5 2.5 0 0 1 0 5H16" />
      <path d="M8 3c-.6.8-.6 1.7 0 2.5M12 3c-.6.8-.6 1.7 0 2.5" />
    </>
  ),
  plus: <path d="M12 5v14M5 12h14" />,
  notebook: (
    <>
      <path d="M6 3h11a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6V3Z" />
      <path d="M6 3v18M4 7h4M4 12h4M4 17h4M10 8h6M10 12h4" />
    </>
  ),
  search: (
    <>
      <circle cx="11" cy="11" r="6.5" />
      <path d="m20 20-4.2-4.2" />
    </>
  ),
  taste: (
    <>
      <path d="M12 3 20.5 9.2 17.3 19H6.7L3.5 9.2 12 3Z" />
      <path d="M12 8 15.7 10.7 14.3 15H9.7L8.3 10.7 12 8Z" />
    </>
  ),
  back: <path d="M15 5 8 12l7 7" />,
  close: <path d="M6 6l12 12M18 6 6 18" />,
  check: <path d="m5 12.5 4.5 4.5L19 7.5" />,
  edit: (
    <>
      <path d="M4 20h4L19 9l-4-4L4 16v4Z" />
      <path d="m13.5 6.5 4 4" />
    </>
  ),
  trash: (
    <>
      <path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13" />
      <path d="M10 11v6M14 11v6" />
    </>
  ),
  lock: (
    <>
      <rect x="5" y="10" width="14" height="10" rx="2" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3" />
    </>
  ),
  globe: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M3.5 12h17M12 3.5c2.5 2.6 2.5 14.4 0 17M12 3.5c-2.5 2.6-2.5 14.4 0 17" />
    </>
  ),
  chevron: <path d="m9 5 7 7-7 7" />,
  sparkle: (
    <>
      <path d="M12 3.5 13.6 10.4 20.5 12l-6.9 1.6L12 20.5l-1.6-6.9L3.5 12l6.9-1.6L12 3.5Z" />
    </>
  ),
  compass: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <path d="m15.5 8.5-2 5-5 2 2-5 5-2Z" />
    </>
  ),
  bean: (
    <>
      <ellipse cx="12" cy="12" rx="6" ry="8.5" transform="rotate(-30 12 12)" />
      <path d="M8.6 17.7c2.4-1.7 1-4.3 3.2-6.2 1.9-1.6 1.5-4.2 3.6-5.2" />
    </>
  ),
  leaf: (
    <>
      <path d="M5 19c0-8 5-13 14-14-1 9-6 14-14 14Z" />
      <path d="M5 19 13 11" />
    </>
  ),
  logout: (
    <>
      <path d="M10 4H5v16h5" />
      <path d="M15 8l4 4-4 4M19 12H9" />
    </>
  ),
  info: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 11v5M12 8h.01" />
    </>
  ),
  drop: <path d="M12 3.5s6 6.4 6 10.5a6 6 0 0 1-12 0c0-4.1 6-10.5 6-10.5Z" />,
  thermo: (
    <>
      <path d="M10 14V5a2 2 0 0 1 4 0v9a4 4 0 1 1-4 0Z" />
      <path d="M12 9v7" />
    </>
  ),
  timer: (
    <>
      <circle cx="12" cy="13" r="7.5" />
      <path d="M12 9v4l2.5 2M10 3h4" />
    </>
  ),
  scale: (
    <>
      <rect x="4" y="9" width="16" height="11" rx="2" />
      <path d="M9 9V6h6v3M12 13v3" />
    </>
  ),
  users: (
    <>
      <circle cx="9" cy="9" r="3.2" />
      <path d="M3.5 19c.6-3 2.8-4.6 5.5-4.6s4.9 1.6 5.5 4.6" />
      <path d="M15.5 6.2a3 3 0 0 1 0 5.6M17 14.6c1.8.5 3 1.9 3.5 4.4" />
    </>
  ),
  pour: (
    <>
      <path d="M5 6h14l-5 7v5h-4v-5L5 6Z" />
      <path d="M8 21h8" />
    </>
  ),
  espresso: (
    <>
      <path d="M6 10h10v3a5 5 0 0 1-10 0v-3Z" />
      <path d="M16 11h1.5a2 2 0 0 1 0 4H15" />
      <path d="M4 20h14" />
    </>
  ),
  latte: (
    <>
      <path d="M7 5h10l-1.2 14.2a2 2 0 0 1-2 1.8h-3.6a2 2 0 0 1-2-1.8L7 5Z" />
      <path d="M7.6 11h8.8" />
      <path d="M10.5 14.5c1-.8 2-.8 3 0" />
    </>
  ),
  americano: (
    <>
      <path d="M6 7h12l-1.5 12.5a2 2 0 0 1-2 1.5h-5a2 2 0 0 1-2-1.5L6 7Z" />
      <path d="M5 7h14M9 3.5h6l1 3.5H8l1-3.5Z" />
    </>
  ),
  refresh: (
    <>
      <path d="M19 12a7 7 0 1 1-2.1-5" />
      <path d="M19 4v4h-4" />
    </>
  ),
}

export function Icon({ name, size = 22, stroke = 1.8 }: { name: IconName; size?: number; stroke?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={stroke}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {PATHS[name]}
    </svg>
  )
}
