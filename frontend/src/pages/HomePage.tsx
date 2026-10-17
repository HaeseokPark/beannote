import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router'
import { recommendationApi } from '../api/endpoints.ts'
import type { Recommendation, ReasonType } from '../api/types.ts'
import { useSession } from '../auth/session.tsx'
import { RoastDots } from '../components/BeanCard.tsx'
import { BeanMark, CoffeeCupArt } from '../components/CoffeeArt.tsx'
import { Icon, type IconName } from '../components/Icon.tsx'
import { useToast } from '../components/Toast.tsx'
import { ConfidenceBadge, ErrorState, FlavorChips, Loading, Notice } from '../components/ui.tsx'
import { formatRating, greeting } from '../lib/format.ts'
import { PROCESS_NAMES, ROAST_NAMES } from '../lib/labels.ts'

const REASON_ICONS: Record<ReasonType, IconName> = {
  FLAVOR: 'leaf',
  TASTE_AXIS: 'drop',
  ROAST: 'bean',
  PROCESS: 'drop',
  EXPLORE: 'compass',
  COLLAB: 'users',
  POPULAR: 'sparkle',
  PROFILE: 'sparkle',
}

/** 홈: 취향 신뢰도 + 추천 카드 10개 (R-1 ~ R-3) */
export function HomePage() {
  const session = useSession()
  const recs = useQuery({ queryKey: ['recommendations'], queryFn: recommendationApi.list })
  const data = recs.data

  return (
    <main className="page with-tabs home">
      <header className="row between">
        <span className="brand">
          <BeanMark size={22} /> Beanote
        </span>
        {data && <ConfidenceBadge confidence={data.confidence} />}
      </header>

      <section className="home-hero">
        <div>
          <p className="eyebrow">{greeting()}</p>
          <h1>
            {session?.user.nickname}님,
            <br />
            오늘의 원두를 골라 봤어요
          </h1>
        </div>
        <CoffeeCupArt size={96} />
      </section>

      {data && <ConfidenceNotice confidence={data.confidence} notes={data.basedOnNotes} popular={data.popular} />}

      <section className="section">
        <div className="section-title">
          <h2>{data?.popular ? '많이 기록된 원두' : '취향 맞춤 추천'}</h2>
          {data && <span className="small muted">{data.items.length}개</span>}
        </div>
        {recs.isPending && <Loading lines={4} />}
        {recs.isError && <ErrorState error={recs.error} onRetry={() => recs.refetch()} />}
        {data && (
          <ol className="rec-list">
            {data.items.map((item, index) => (
              <RecommendationCard key={item.bean.id} item={item} rank={index + 1} />
            ))}
          </ol>
        )}
      </section>
    </main>
  )
}

function ConfidenceNotice({ confidence, notes, popular }: { confidence: string; notes: number; popular: boolean }) {
  if (popular) {
    return (
      <Notice icon="sparkle">
        아직 취향 정보가 없어서 많이 기록된 원두를 보여 드려요.{' '}
        <Link className="text-link" to="/onboarding">
          30초 취향 설문
        </Link>
        을 하면 바로 맞춤 추천을 받을 수 있어요.
      </Notice>
    )
  }
  if (confidence === 'SURVEY') {
    return <Notice>설문을 바탕으로 추천했어요. 노트를 {Math.max(0, 3 - notes)}개 더 쓰면 실제 입맛을 반영하기 시작해요.</Notice>
  }
  if (confidence === 'FORMING') {
    return <Notice>노트 {notes}개로 취향이 만들어지는 중이에요. {10 - notes}개 더 쓰면 추천이 더 정확해져요.</Notice>
  }
  return null
}

function RecommendationCard({ item, rank }: { item: Recommendation; rank: number }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const toast = useToast()
  const { bean } = item

  const feedback = useMutation({
    mutationFn: (type: 'NOT_INTERESTED' | 'ALREADY_TRIED') => recommendationApi.feedback(bean.id, type),
    onSuccess: (_, type) => {
      queryClient.invalidateQueries({ queryKey: ['recommendations'] })
      if (type === 'ALREADY_TRIED') navigate(`/notes/new?beanId=${bean.id}`)
      else toast('이 원두는 다시 추천하지 않을게요')
    },
  })

  const origin = bean.blend ? '블렌드' : bean.origins.map((o) => o.name).join(' · ')

  return (
    <li className="card rec-card">
      {item.isExploration && (
        <span className="explore-ribbon">
          <Icon name="compass" size={14} /> 새로운 경험
        </span>
      )}
      <Link to={`/beans/${bean.id}`} className="rec-main">
        <span className="rec-rank serif">{String(rank).padStart(2, '0')}</span>
        <div className="rec-body">
          <span className="origin-tag">{origin}</span>
          <h3 className="card-title">{bean.name}</h3>
          <p className="small muted">{bean.roastery.name}</p>
          <div className="bean-meta">
            <span>{PROCESS_NAMES[bean.process]}</span>
            <span className="sep" />
            <RoastDots level={bean.roastLevel} />
            <span>{ROAST_NAMES[bean.roastLevel]}</span>
            {bean.noteCount > 0 && (
              <>
                <span className="sep" />
                <span className="rating-number">★ {formatRating(bean.avgRating)}</span>
              </>
            )}
          </div>
        </div>
      </Link>

      {bean.flavors.length > 0 && <FlavorChips flavors={bean.flavors} max={3} />}

      <ul className="reasons">
        {item.reasons.map((reason) => (
          <li key={reason.text} className="reason">
            <Icon name={REASON_ICONS[reason.type]} size={16} />
            <span>{reason.text}</span>
          </li>
        ))}
      </ul>

      <div className="rec-actions">
        <button className="btn btn-ghost btn-sm" disabled={feedback.isPending} onClick={() => feedback.mutate('NOT_INTERESTED')}>
          <Icon name="close" size={16} /> 관심 없음
        </button>
        <button className="btn btn-secondary btn-sm" disabled={feedback.isPending} onClick={() => feedback.mutate('ALREADY_TRIED')}>
          <Icon name="check" size={16} /> 이미 마셔봤어요
        </button>
      </div>
    </li>
  )
}
