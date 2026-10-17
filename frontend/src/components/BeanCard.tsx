import type { CSSProperties } from 'react'
import { Link } from 'react-router'
import type { BeanSummary } from '../api/types.ts'
import { formatRating } from '../lib/format.ts'
import { PROCESS_NAMES, ROAST_NAMES } from '../lib/labels.ts'
import { Icon } from './Icon.tsx'
import { FlavorChips } from './ui.tsx'

/** 원두 요약 카드 (원두 찾기·비슷한 원두) */
export function BeanCard({ bean, compact }: { bean: BeanSummary; compact?: boolean }) {
  const origin = bean.blend ? '블렌드' : bean.origins.map((o) => o.name).join(' · ')
  return (
    <Link to={`/beans/${bean.id}`} className={`card tappable bean-card ${compact ? 'compact' : ''}`}>
      <div className="bean-card-top">
        <span className="origin-tag">{origin}</span>
        {bean.status === 'DISCONTINUED' && <span className="badge ink">판매 종료</span>}
      </div>
      <h3 className="card-title">{bean.name}</h3>
      <p className="small muted">{bean.roastery.name}</p>
      <div className="bean-meta">
        <span>{PROCESS_NAMES[bean.process]}</span>
        <span className="sep" />
        <RoastDots level={bean.roastLevel} />
        <span>{ROAST_NAMES[bean.roastLevel]}</span>
      </div>
      {!compact && bean.flavors.length > 0 && <FlavorChips flavors={bean.flavors} max={3} />}
      <div className="bean-card-foot">
        {bean.noteCount > 0 ? (
          <span className="row small" style={{ '--gap': '4px' } as CSSProperties}>
            <span className="rating-number">★ {formatRating(bean.avgRating)}</span>
            <span className="muted">· 노트 {bean.noteCount}</span>
          </span>
        ) : (
          <span className="small muted">아직 노트가 없어요</span>
        )}
        <Icon name="chevron" size={18} />
      </div>
    </Link>
  )
}

/** 로스팅 정도를 원두 5개로 표시 */
export function RoastDots({ level }: { level: BeanSummary['roastLevel'] }) {
  const index = ['LIGHT', 'MEDIUM_LIGHT', 'MEDIUM', 'MEDIUM_DARK', 'DARK'].indexOf(level)
  return (
    <span className="roast-dots" aria-hidden="true">
      {[0, 1, 2, 3, 4].map((i) => (
        <i key={i} className={i <= index ? 'on' : ''} style={{ opacity: 0.45 + i * 0.13 }} />
      ))}
    </span>
  )
}
