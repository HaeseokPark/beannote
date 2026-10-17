import { useQuery } from '@tanstack/react-query'
import type { CSSProperties } from 'react'
import { Link, useNavigate } from 'react-router'
import { tasteApi } from '../api/endpoints.ts'
import type { AttributeRating, TasteProfile } from '../api/types.ts'
import { endSession, useSession } from '../auth/session.tsx'
import { Icon } from '../components/Icon.tsx'
import { TasteRadar } from '../components/TasteRadar.tsx'
import { ConfidenceBadge, Empty, ErrorState, FlavorChip, Loading } from '../components/ui.tsx'
import { formatRating } from '../lib/format.ts'

/** 내 취향 (T-1 ~ T-4) */
export function TastePage() {
  const session = useSession()
  const navigate = useNavigate()
  const profile = useQuery({ queryKey: ['taste-profile'], queryFn: tasteApi.profile })

  const logout = async () => {
    await endSession()
    navigate('/welcome', { replace: true })
  }

  return (
    <main className="page with-tabs taste">
      <header className="topbar left">
        <h1>내 취향</h1>
        <button className="icon-btn" aria-label="로그아웃" onClick={logout}>
          <Icon name="logout" size={20} />
        </button>
      </header>

      {profile.isPending && <Loading />}
      {profile.isError && <ErrorState error={profile.error} onRetry={() => profile.refetch()} />}
      {profile.data && <ProfileView profile={profile.data} nickname={session?.user.nickname ?? ''} />}
    </main>
  )
}

function ProfileView({ profile, nickname }: { profile: TasteProfile; nickname: string }) {
  const progress = Math.min(1, profile.noteCount / (profile.noteCount + profile.notesUntilStable || 1))

  return (
    <>
      <section className="card taste-hero">
        <div className="row between">
          <div>
            <p className="eyebrow">{nickname}님의 커피 취향</p>
            <h2 style={{ marginTop: 2 }}>노트 {profile.noteCount}개</h2>
          </div>
          <ConfidenceBadge confidence={profile.confidence} />
        </div>
        <div className="stable-meter" style={{ '--p': progress } as CSSProperties}>
          <span />
        </div>
        <p className="small muted">
          {profile.notesUntilStable > 0
            ? `노트 ${profile.notesUntilStable}개 더 쓰면 취향이 안정돼요`
            : '충분한 기록으로 취향이 안정됐어요'}
        </p>

        {profile.radar ? (
          <>
            <TasteRadar value={profile.radar} />
            <p className="tiny muted center">
              {profile.radarSource === 'NOTES' ? '높게 평가한 커피들의 맛 경향이에요' : '설문에서 고른 맛이에요'}
            </p>
          </>
        ) : (
          <Empty title="아직 취향을 몰라요" action={<Link className="btn btn-primary btn-sm" to="/onboarding">30초 취향 설문</Link>}>
            설문을 하거나 노트를 3개 쓰면 취향 레이더가 그려져요.
          </Empty>
        )}
      </section>

      {(profile.likedFlavors.length > 0 || profile.likedCategories.length > 0) && (
        <section className="section">
          <div className="section-title">
            <h2>좋아하는 향</h2>
          </div>
          <div className="chips">
            {profile.likedCategories.map((c) => (
              <FlavorChip key={c.code} name={`${c.name}${c.fromSurvey ? ' · 설문' : ''}`} category={c.code} selected size="lg" />
            ))}
          </div>
          {profile.likedFlavors.length > 0 && (
            <div className="chips" style={{ marginTop: 10 }}>
              {profile.likedFlavors.map((f) => (
                <FlavorChip key={f.code} name={f.name} category={f.categoryCode} />
              ))}
            </div>
          )}
        </section>
      )}

      {(profile.dislikedFlavors.length > 0 || profile.dislikedCategories.length > 0) && (
        <section className="section">
          <div className="section-title">
            <h2>덜 좋아하는 향</h2>
          </div>
          <div className="chips">
            {profile.dislikedCategories.map((c) => (
              <FlavorChip key={c.code} name={`${c.name}${c.fromSurvey ? ' · 설문' : ''}`} category={c.code} />
            ))}
            {profile.dislikedFlavors.map((f) => (
              <FlavorChip key={f.code} name={f.name} category={f.categoryCode} />
            ))}
          </div>
        </section>
      )}

      <AttributeSection title="가공 방식" items={profile.processes} />
      <AttributeSection title="원산지" items={profile.origins} />
      <AttributeSection title="로스팅" items={profile.roastLevels} />

      <section className="section stack">
        <Link to="/onboarding" className="btn btn-outline btn-block">
          <Icon name="refresh" size={18} /> 취향 설문 다시 하기
        </Link>
      </section>
    </>
  )
}

/** T-3: 평균 별점 막대 */
function AttributeSection({ title, items }: { title: string; items: AttributeRating[] }) {
  if (items.length === 0) return null
  return (
    <section className="section">
      <div className="section-title">
        <h2>{title}</h2>
        <span className="small muted">평균 별점</span>
      </div>
      <div className="card flat bars">
        {items.slice(0, 5).map((item) => (
          <div key={item.code} className="bar attr">
            <span>{item.name}</span>
            <div className="bar-track">
              <div className="bar-fill" style={{ width: `${(item.avgRating / 5) * 100}%` }} />
            </div>
            <span className="num">
              {formatRating(item.avgRating)}
              <span className="tiny muted"> ({item.noteCount})</span>
            </span>
          </div>
        ))}
      </div>
    </section>
  )
}
