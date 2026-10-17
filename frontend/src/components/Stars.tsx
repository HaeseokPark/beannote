import { useId, type CSSProperties } from 'react'

function StarShape({ fill }: { fill: 0 | 0.5 | 1 }) {
  const id = `half${useId().replaceAll(':', '')}`
  return (
    <svg viewBox="0 0 24 24" width="100%" height="100%" aria-hidden="true">
      <defs>
        <linearGradient id={id}>
          <stop offset="50%" stopColor="currentColor" />
          <stop offset="50%" stopColor="transparent" />
        </linearGradient>
      </defs>
      <path
        d="M12 2.8l2.8 5.8 6.3.9-4.6 4.4 1.1 6.3L12 17.2l-5.6 3 1.1-6.3-4.6-4.4 6.3-.9L12 2.8Z"
        fill={fill === 1 ? 'currentColor' : fill === 0.5 ? `url(#${id})` : 'transparent'}
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function fillOf(value: number, index: number): 0 | 0.5 | 1 {
  if (value >= index + 1) return 1
  if (value >= index + 0.5) return 0.5
  return 0
}

/** 별점 표시 (0.5 단위) */
export function Stars({ value, size = 15 }: { value: number; size?: number }) {
  return (
    <span className="stars" style={{ '--size': `${size}px` } as CSSProperties} aria-label={`별점 ${value}점`}>
      {[0, 1, 2, 3, 4].map((i) => (
        <span className="star" key={i}>
          <StarShape fill={fillOf(value, i)} />
        </span>
      ))}
    </span>
  )
}

/** 별점 입력. 별의 왼쪽 절반을 누르면 .5 점 */
export function StarInput({ value, onChange }: { value: number; onChange: (value: number) => void }) {
  return (
    <div className="stars stars-input" style={{ '--size': '42px' } as CSSProperties} role="radiogroup" aria-label="총평 별점">
      {[0, 1, 2, 3, 4].map((i) => (
        <span className="star" key={i}>
          <StarShape fill={fillOf(value, i)} />
          <button
            type="button"
            className="half"
            aria-label={`${i + 0.5}점`}
            onClick={() => onChange(i + 0.5)}
          />
          <button
            type="button"
            className="half right"
            aria-label={`${i + 1}점`}
            onClick={() => onChange(i + 1)}
          />
        </span>
      ))}
    </div>
  )
}
