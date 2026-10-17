/**
 * 김이 오르는 커피잔 일러스트 (장식용). 색은 디자인 토큰을 따라 다크 모드에서도 어울린다.
 */
export function CoffeeCupArt({ size = 160 }: { size?: number }) {
  return (
    <svg className="cup-art" width={size} height={size} viewBox="0 0 200 200" aria-hidden="true">
      <defs>
        <radialGradient id="crema" cx="45%" cy="40%" r="65%">
          <stop offset="0%" stopColor="#e9b979" />
          <stop offset="55%" stopColor="#b9773f" />
          <stop offset="100%" stopColor="#6e3b20" />
        </radialGradient>
        <linearGradient id="cupBody" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stopColor="var(--card)" />
          <stop offset="100%" stopColor="var(--paper-2)" />
        </linearGradient>
      </defs>

      {/* 받침 */}
      <ellipse cx="100" cy="164" rx="78" ry="16" fill="var(--paper-2)" stroke="var(--line-strong)" strokeWidth="2" />
      <ellipse cx="100" cy="160" rx="50" ry="8" fill="var(--line)" opacity="0.7" />

      {/* 잔 */}
      <path
        d="M42 86h116l-8 52a28 28 0 0 1-27.7 24H77.7A28 28 0 0 1 50 138L42 86Z"
        fill="url(#cupBody)"
        stroke="var(--line-strong)"
        strokeWidth="2.5"
      />
      <path
        d="M156 98h8a18 18 0 0 1 0 36h-12"
        fill="none"
        stroke="var(--line-strong)"
        strokeWidth="7"
        strokeLinecap="round"
      />

      {/* 커피 표면과 라떼 아트 */}
      <ellipse cx="100" cy="86" rx="58" ry="13" fill="url(#crema)" />
      <path
        d="M100 78c-9 0-14 4-14 8 0 3 4 5 8 5-3 1-5 2-5 3 3 1 7 1 11 1s8 0 11-1c0-1-2-2-5-3 4 0 8-2 8-5 0-4-5-8-14-8Z"
        fill="#f6e3c6"
        opacity="0.9"
      />

      {/* 김 */}
      <g fill="none" stroke="var(--ink-3)" strokeWidth="3" strokeLinecap="round" opacity="0.55">
        <path className="steam s1" d="M78 64c-8-10 8-14 0-26" />
        <path className="steam s2" d="M100 60c-8-10 8-14 0-28" />
        <path className="steam s3" d="M122 64c-8-10 8-14 0-26" />
      </g>
    </svg>
  )
}

/** 작은 원두 장식 */
export function BeanMark({ size = 28, color = 'var(--roast)' }: { size?: number; color?: string }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <ellipse cx="16" cy="16" rx="9" ry="12.5" transform="rotate(-30 16 16)" fill={color} />
      <path
        d="M10.6 24.4c3.4-2.4 1.4-6.2 4.6-8.9 2.7-2.3 2.2-6 5.2-7.4"
        fill="none"
        stroke="var(--paper)"
        strokeWidth="1.8"
        strokeLinecap="round"
        opacity="0.85"
      />
    </svg>
  )
}
