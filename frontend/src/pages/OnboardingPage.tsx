import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type CSSProperties } from 'react'
import { useNavigate } from 'react-router'
import { errorMessage } from '../api/client.ts'
import { flavorApi, surveyApi } from '../api/endpoints.ts'
import type { Axes, CategoryCode, CoffeeHabit } from '../api/types.ts'
import { Icon, type IconName } from '../components/Icon.tsx'
import { TasteSlider } from '../components/TasteSlider.tsx'
import { AXES, CATEGORY_HINTS, HABITS, flavorColor } from '../lib/labels.ts'

const HABIT_ICONS: Record<CoffeeHabit, IconName> = {
  AMERICANO: 'americano',
  LATTE: 'latte',
  POUR_OVER: 'pour',
  ESPRESSO: 'espresso',
}

const STEPS = ['평소 커피', '좋아하는 맛', '좋아하는 향']
const MAX_PICK = 3

/** 온보딩 취향 설문 3단계 (O-1 ~ O-4). 다시 하기(O-5)도 같은 화면 */
export function OnboardingPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [step, setStep] = useState(0)
  const [habit, setHabit] = useState<CoffeeHabit | null>(null)
  const [prefs, setPrefs] = useState<Axes>({ acidity: 3, sweetness: 3, body: 3, bitterness: 3, aftertaste: 3 })
  const [liked, setLiked] = useState<CategoryCode[]>([])
  const [disliked, setDisliked] = useState<CategoryCode[]>([])

  const categories = useQuery({ queryKey: ['flavors'], queryFn: flavorApi.all, staleTime: Infinity })
  const existing = useQuery({ queryKey: ['survey'], queryFn: surveyApi.get })

  // 다시 하기: 이전 답을 채워 둔다
  useEffect(() => {
    const s = existing.data
    if (!s) return
    setHabit(s.coffeeHabit)
    setPrefs(s.preferences)
    setLiked(s.likedCategories.map((c) => c.code))
    setDisliked(s.dislikedCategories.map((c) => c.code))
  }, [existing.data])

  const save = useMutation({
    mutationFn: () =>
      surveyApi.save({ coffeeHabit: habit!, preferences: prefs, likedCategories: liked, dislikedCategories: disliked }),
    onSuccess: () => {
      queryClient.invalidateQueries()
      navigate('/home', { replace: true })
    },
  })

  const toggle = (list: CategoryCode[], set: (v: CategoryCode[]) => void, other: CategoryCode[], code: CategoryCode) => {
    if (list.includes(code)) set(list.filter((c) => c !== code))
    else if (list.length < MAX_PICK && !other.includes(code)) set([...list, code])
  }

  const canNext = step === 0 ? habit !== null : true

  return (
    <main className="page onboarding">
      <header className="row between" style={{ minHeight: 44 }}>
        {step > 0 ? (
          <button className="icon-btn" aria-label="이전 단계" onClick={() => setStep(step - 1)}>
            <Icon name="back" />
          </button>
        ) : (
          <span />
        )}
        <button className="btn btn-ghost btn-sm" onClick={() => navigate('/home', { replace: true })}>
          건너뛰기
        </button>
      </header>

      <div className="progress" aria-label={`${STEPS.length}단계 중 ${step + 1}단계`}>
        {STEPS.map((s, i) => (
          <span key={s} className={i <= step ? 'done' : ''} />
        ))}
      </div>

      {step === 0 && (
        <section className="onboard-step">
          <p className="eyebrow">1 / 3 · {STEPS[0]}</p>
          <h1>평소에 어떤 커피를 즐겨 마시나요?</h1>
          <p className="muted">가장 가까운 것 하나만 골라 주세요.</p>
          <div className="option-grid">
            {HABITS.map((h) => (
              <button
                key={h.value}
                className={`option ${habit === h.value ? 'selected' : ''}`}
                onClick={() => setHabit(h.value)}
                aria-pressed={habit === h.value}
              >
                <span className="option-icon">
                  <Icon name={HABIT_ICONS[h.value]} />
                </span>
                <strong>{h.name}</strong>
                <span className="muted">{h.hint}</span>
              </button>
            ))}
          </div>
        </section>
      )}

      {step === 1 && (
        <section className="onboard-step">
          <p className="eyebrow">2 / 3 · {STEPS[1]}</p>
          <h1>어떤 맛을 좋아하세요?</h1>
          <p className="muted">정답은 없어요. 끌리는 정도를 골라 주세요.</p>
          <div className="stack">
            {AXES.map((axis) => (
              <TasteSlider
                key={axis.key}
                name={axis.name}
                levels={axis.levels}
                value={prefs[axis.key]}
                prefix="이런 커피가 좋아요 · "
                onChange={(v) => setPrefs({ ...prefs, [axis.key]: v })}
              />
            ))}
          </div>
        </section>
      )}

      {step === 2 && (
        <section className="onboard-step">
          <p className="eyebrow">3 / 3 · {STEPS[2]}</p>
          <h1>좋아하는 향과 싫어하는 향</h1>
          <p className="muted">각각 최대 3개까지 고를 수 있어요.</p>

          <h3 className="pick-title">
            <span className="badge good">좋아요</span> {liked.length} / {MAX_PICK}
          </h3>
          <CategoryGrid
            categories={categories.data ?? []}
            selected={liked}
            disabled={disliked}
            onToggle={(code) => toggle(liked, setLiked, disliked, code)}
          />

          <h3 className="pick-title">
            <span className="badge ink">별로예요</span> {disliked.length} / {MAX_PICK}
          </h3>
          <CategoryGrid
            categories={categories.data ?? []}
            selected={disliked}
            disabled={liked}
            negative
            onToggle={(code) => toggle(disliked, setDisliked, liked, code)}
          />
        </section>
      )}

      {save.isError && <p className="error-text">{errorMessage(save.error)}</p>}

      <div className="action-bar no-tabs">
        {step < STEPS.length - 1 ? (
          <button className="btn btn-primary btn-block" disabled={!canNext} onClick={() => setStep(step + 1)}>
            다음
          </button>
        ) : (
          <button className="btn btn-primary btn-block" disabled={save.isPending || !habit} onClick={() => save.mutate()}>
            {save.isPending ? '저장 중…' : '내 취향으로 추천 받기'}
          </button>
        )}
      </div>
    </main>
  )
}

function CategoryGrid({
  categories,
  selected,
  disabled,
  negative,
  onToggle,
}: {
  categories: { code: CategoryCode; name: string }[]
  selected: CategoryCode[]
  disabled: CategoryCode[]
  negative?: boolean
  onToggle: (code: CategoryCode) => void
}) {
  return (
    <div className="cat-grid">
      {categories.map((c) => {
        const isSelected = selected.includes(c.code)
        return (
          <button
            key={c.code}
            className={`cat-option ${isSelected ? 'selected' : ''} ${negative ? 'negative' : ''}`}
            style={{ '--c': flavorColor(c.code) } as CSSProperties}
            disabled={disabled.includes(c.code)}
            aria-pressed={isSelected}
            onClick={() => onToggle(c.code)}
          >
            <span className="wheel-dot" />
            <strong>{c.name}</strong>
            <span className="tiny muted">{CATEGORY_HINTS[c.code]}</span>
          </button>
        )
      })}
    </div>
  )
}
