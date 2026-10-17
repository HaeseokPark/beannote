import { useEffect, useRef, type CSSProperties, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import type { CategoryCode, Confidence, FlavorLabel } from '../api/types.ts'
import { CONFIDENCE, flavorColor } from '../lib/labels.ts'
import { Icon, type IconName } from './Icon.tsx'

/** 화면 상단: 뒤로 가기 + 제목 */
export function TopBar({ title, back = true, right }: { title?: string; back?: boolean; right?: ReactNode }) {
  const navigate = useNavigate()
  return (
    <header className={back ? 'topbar' : 'topbar left'}>
      {back && (
        <button className="icon-btn" aria-label="뒤로 가기" onClick={() => navigate(-1)}>
          <Icon name="back" />
        </button>
      )}
      {title && <h1>{title}</h1>}
      {right}
    </header>
  )
}

/** 향미 칩. 대분류마다 고유한 색 */
export function FlavorChip({
  name,
  category,
  selected,
  onClick,
  size,
}: {
  name: string
  category: CategoryCode | string
  selected?: boolean
  onClick?: () => void
  size?: 'lg'
}) {
  const className = ['chip', selected ? 'selected' : '', size ?? ''].join(' ').trim()
  const style = { '--c': flavorColor(category) } as CSSProperties
  if (onClick) {
    return (
      <button type="button" className={className} style={style} onClick={onClick} aria-pressed={selected}>
        <span className="dot" />
        {name}
      </button>
    )
  }
  return (
    <span className={className} style={style}>
      <span className="dot" />
      {name}
    </span>
  )
}

export function FlavorChips({ flavors, max }: { flavors: FlavorLabel[]; max?: number }) {
  const shown = max ? flavors.slice(0, max) : flavors
  return (
    <div className="chips">
      {shown.map((f) => (
        <FlavorChip key={f.code} name={f.name} category={f.categoryCode} />
      ))}
    </div>
  )
}

export function ConfidenceBadge({ confidence }: { confidence: Confidence }) {
  const { label, tone } = CONFIDENCE[confidence]
  return <span className={`badge ${tone === 'crema' ? '' : tone}`}>{label}</span>
}

export function Notice({ children, icon = 'info' }: { children: ReactNode; icon?: IconName }) {
  return (
    <div className="notice">
      <Icon name={icon} size={18} />
      <div>{children}</div>
    </div>
  )
}

export function Empty({ title, children, action }: { title: string; children?: ReactNode; action?: ReactNode }) {
  return (
    <div className="empty">
      <div className="empty-art">
        <Icon name="cup" size={36} stroke={1.4} />
      </div>
      <h3>{title}</h3>
      {children && <p className="small">{children}</p>}
      {action}
    </div>
  )
}

export function Loading({ lines = 3 }: { lines?: number }) {
  return (
    <div className="stack" aria-busy="true" aria-label="불러오는 중">
      {Array.from({ length: lines }, (_, i) => (
        <div key={i} className="skeleton" style={{ height: i === 0 ? 120 : 84 }} />
      ))}
    </div>
  )
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  return (
    <Empty
      title="불러오지 못했어요"
      action={
        onRetry && (
          <button className="btn btn-outline btn-sm" onClick={onRetry}>
            <Icon name="refresh" size={16} /> 다시 시도
          </button>
        )
      }
    >
      {error instanceof Error ? error.message : null}
    </Empty>
  )
}

/** 화면 끝에 닿으면 다음 페이지를 불러온다 (무한 스크롤) */
export function LoadMore({ onVisible, loading, done }: { onVisible: () => void; loading: boolean; done: boolean }) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const element = ref.current
    if (!element || done) return
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && !loading) onVisible()
    })
    observer.observe(element)
    return () => observer.disconnect()
  }, [onVisible, loading, done])
  if (done) return null
  return (
    <div ref={ref} className="row" style={{ justifyContent: 'center', padding: 20 }}>
      {loading && <div className="spinner" />}
    </div>
  )
}

export function ConfirmDialog({
  title,
  message,
  confirmLabel,
  danger,
  onConfirm,
  onCancel,
}: {
  title: string
  message?: string
  confirmLabel: string
  danger?: boolean
  onConfirm: () => void
  onCancel: () => void
}) {
  return (
    <div className="dialog-backdrop" onClick={onCancel}>
      <div className="dialog" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <h2>{title}</h2>
        {message && (
          <p className="muted" style={{ marginTop: 8 }}>
            {message}
          </p>
        )}
        <div className="row" style={{ marginTop: 22, '--gap': '10px' } as CSSProperties}>
          <button className="btn btn-outline" style={{ flex: 1 }} onClick={onCancel}>
            취소
          </button>
          <button className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} style={{ flex: 1 }} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
