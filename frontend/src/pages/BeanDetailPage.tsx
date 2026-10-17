import { useQuery } from '@tanstack/react-query'
import type { CSSProperties } from 'react'
import { Link, useParams } from 'react-router'
import { beanApi } from '../api/endpoints.ts'
import type { BeanDetail } from '../api/types.ts'
import { BeanCard, RoastDots } from '../components/BeanCard.tsx'
import { Icon } from '../components/Icon.tsx'
import { Stars } from '../components/Stars.tsx'
import { TasteRadar } from '../components/TasteRadar.tsx'
import { ErrorState, FlavorChips, Loading, Notice, TopBar } from '../components/ui.tsx'
import { formatRating } from '../lib/format.ts'
import { PROCESS_HINTS, PROCESS_NAMES, ROAST_NAMES } from '../lib/labels.ts'

/** 원두 상세: 속성, 공식 향미, 사용자 평균 레이더, 비슷한 원두 (B-2, R-4) */
export function BeanDetailPage() {
  const id = Number(useParams().id)
  const bean = useQuery({ queryKey: ['bean', id], queryFn: () => beanApi.detail(id) })

  return (
    <main className="page with-action">
      <TopBar title="원두" />
      {bean.isPending && <Loading />}
      {bean.isError && <ErrorState error={bean.error} />}
      {bean.data && <BeanView bean={bean.data} />}
    </main>
  )
}

function BeanView({ bean }: { bean: BeanDetail }) {
  const similar = useQuery({ queryKey: ['similar', bean.id], queryFn: () => beanApi.similar(bean.id) })
  const origin = bean.blend ? '블렌드' : bean.origins.map((o) => o.name).join(' · ')

  const attributes = [
    { label: '원산지', value: bean.origins.map((o) => o.name).join(', ') },
    bean.region && { label: '지역', value: bean.region },
    bean.farm && { label: '농장', value: bean.farm },
    bean.varieties.length > 0 && { label: '품종', value: bean.varieties.join(', ') },
    { label: '가공', value: PROCESS_NAMES[bean.process], hint: PROCESS_HINTS[bean.process] },
    { label: '로스팅', value: ROAST_NAMES[bean.roastLevel] },
  ].filter(Boolean) as { label: string; value: string; hint?: string }[]

  return (
    <>
      <section className="bean-hero card">
        <div className="row between">
          <span className="origin-tag">{origin}</span>
          {bean.status === 'DISCONTINUED' && <span className="badge ink">판매 종료</span>}
        </div>
        <h1>{bean.name}</h1>
        <p className="muted">{bean.roastery.name}</p>
        <div className="bean-meta" style={{ marginTop: 6 }}>
          <RoastDots level={bean.roastLevel} />
          <span>{ROAST_NAMES[bean.roastLevel]} 로스팅</span>
        </div>
        {bean.stats.noteCount > 0 ? (
          <div className="row" style={{ marginTop: 14, '--gap': '8px' } as CSSProperties}>
            <Stars value={Number(bean.stats.avgRating)} size={18} />
            <span className="rating-number">{formatRating(bean.stats.avgRating)}</span>
            <span className="small muted">· 노트 {bean.stats.noteCount}개</span>
          </div>
        ) : (
          <p className="small muted" style={{ marginTop: 12 }}>아직 노트가 없어요. 첫 기록을 남겨 보세요!</p>
        )}
      </section>

      {bean.flavors.length > 0 && (
        <section className="section">
          <div className="section-title">
            <h2>로스터리가 말하는 향</h2>
          </div>
          <FlavorChips flavors={bean.flavors} />
          {bean.officialFlavorText && <p className="small muted" style={{ marginTop: 8 }}>“{bean.officialFlavorText}”</p>}
        </section>
      )}

      {bean.stats.axes && (
        <section className="section card flat">
          <div className="section-title">
            <h2>마신 사람들의 평균</h2>
            <span className="small muted">노트 {bean.stats.noteCount}개</span>
          </div>
          <TasteRadar value={bean.stats.axes} />
        </section>
      )}

      <section className="section">
        <div className="section-title">
          <h2>원두 정보</h2>
        </div>
        <dl className="card flat kv">
          {attributes.map((a) => (
            <div key={a.label}>
              <dt>{a.label}</dt>
              <dd>
                {a.value}
                {a.hint && <span className="tiny muted" style={{ display: 'block', fontWeight: 400 }}>{a.hint}</span>}
              </dd>
            </div>
          ))}
        </dl>
      </section>

      {similar.data && similar.data.length > 0 && (
        <section className="section">
          <div className="section-title">
            <h2>이 원두와 비슷한 원두</h2>
          </div>
          <ul className="h-scroll">
            {similar.data.map((s) => (
              <li key={s.bean.id}>
                <BeanCard bean={s.bean} compact />
              </li>
            ))}
          </ul>
        </section>
      )}

      {bean.seed && (
        <section className="section">
          <Notice>예시로 만든 원두 정보예요. 실제 판매 정보와 다를 수 있어요.</Notice>
        </section>
      )}

      <div className="action-bar">
        <Link to={`/notes/new?beanId=${bean.id}`} className="btn btn-primary btn-block">
          <Icon name="edit" size={18} /> 이 원두 기록하기
        </Link>
      </div>
    </>
  )
}
