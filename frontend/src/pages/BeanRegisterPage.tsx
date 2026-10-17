import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type CSSProperties, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { ApiError, errorMessage } from '../api/client.ts'
import { beanApi } from '../api/endpoints.ts'
import type { BeanCreateRequest, ProcessMethod, RoastLevel } from '../api/types.ts'
import { RoastDots } from '../components/BeanCard.tsx'
import { Icon } from '../components/Icon.tsx'
import { Notice, TopBar } from '../components/ui.tsx'
import { useDebounced } from '../lib/hooks.ts'
import { PROCESS_HINTS, PROCESS_NAMES, ROAST_LEVELS, ROAST_NAMES } from '../lib/labels.ts'

const ORIGINS = [
  ['ET', '에티오피아'], ['KE', '케냐'], ['CO', '콜롬비아'], ['BR', '브라질'], ['GT', '과테말라'],
  ['CR', '코스타리카'], ['PA', '파나마'], ['HN', '온두라스'], ['SV', '엘살바도르'], ['NI', '니카라과'],
  ['MX', '멕시코'], ['PE', '페루'], ['BO', '볼리비아'], ['EC', '에콰도르'], ['RW', '르완다'],
  ['BI', '부룬디'], ['TZ', '탄자니아'], ['UG', '우간다'], ['YE', '예멘'], ['ID', '인도네시아'],
  ['IN', '인도'], ['VN', '베트남'], ['PG', '파푸아뉴기니'], ['CN', '중국'], ['TH', '태국'], ['JM', '자메이카'],
] as const

const PROCESSES: ProcessMethod[] = ['WASHED', 'NATURAL', 'HONEY', 'ANAEROBIC', 'OTHER']

/** 원두 등록 (B-3): 이름을 입력하면 같은 로스터리의 비슷한 원두를 먼저 보여 준다 */
export function BeanRegisterPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [params] = useSearchParams()
  const fromNote = params.get('from') === 'note'

  const [roasteryId, setRoasteryId] = useState<number | 'new' | ''>('')
  const [roasteryName, setRoasteryName] = useState('')
  const [name, setName] = useState('')
  const [blend, setBlend] = useState(false)
  const [origin, setOrigin] = useState('')
  const [blendOrigins, setBlendOrigins] = useState<string[]>([])
  const [region, setRegion] = useState('')
  const [varieties, setVarieties] = useState('')
  const [process, setProcess] = useState<ProcessMethod>('WASHED')
  const [roast, setRoast] = useState<RoastLevel>('MEDIUM_LIGHT')
  const [flavorText, setFlavorText] = useState('')
  const [duplicateBlocked, setDuplicateBlocked] = useState(false)

  const roasteries = useQuery({ queryKey: ['roasteries'], queryFn: () => beanApi.roasteries() })
  const debouncedName = useDebounced(name.trim(), 400)
  const newRoastery = roasteryId === 'new'
  const duplicates = useQuery({
    queryKey: ['duplicates', roasteryId, roasteryName, debouncedName],
    queryFn: () =>
      beanApi.duplicates({
        roasteryId: typeof roasteryId === 'number' ? roasteryId : undefined,
        roasteryName: newRoastery ? roasteryName.trim() : undefined,
        name: debouncedName,
      }),
    enabled: debouncedName.length >= 2 && (typeof roasteryId === 'number' || (newRoastery && roasteryName.trim().length > 0)),
  })

  const create = useMutation({
    mutationFn: (force: boolean) => {
      const body: BeanCreateRequest = {
        roasteryId: typeof roasteryId === 'number' ? roasteryId : undefined,
        roasteryName: newRoastery ? roasteryName.trim() : undefined,
        name: name.trim(),
        blend,
        originCountry: blend ? undefined : origin,
        blendOrigins: blend ? blendOrigins : undefined,
        region: region.trim() || undefined,
        varieties: varieties.split(',').map((v) => v.trim()).filter(Boolean).slice(0, 5),
        process,
        roastLevel: roast,
        officialFlavorText: flavorText.trim() || undefined,
        force,
      }
      return beanApi.create(body)
    },
    onSuccess: (bean) => {
      queryClient.invalidateQueries({ queryKey: ['beans'] })
      navigate(fromNote ? `/notes/new?beanId=${bean.id}` : `/beans/${bean.id}`, { replace: true })
    },
    onError: (error) => {
      if (error instanceof ApiError && error.code === 'BEAN_DUPLICATE_SUSPECTED') setDuplicateBlocked(true)
    },
  })

  const originOk = blend ? blendOrigins.length >= 2 : origin !== ''
  const roasteryOk = typeof roasteryId === 'number' || (newRoastery && roasteryName.trim().length > 0)
  const valid = roasteryOk && name.trim().length > 0 && originOk

  const submit = (e: FormEvent) => {
    e.preventDefault()
    setDuplicateBlocked(false)
    if (valid) create.mutate(false)
  }

  return (
    <main className="page with-action">
      <TopBar title="원두 등록" />
      <p className="muted small" style={{ marginBottom: 18 }}>
        카탈로그에 없는 원두를 등록해요. 카페에서 마신 커피는 로스터리 자리에 카페 이름, 원두 이름에 메뉴를 적어 주세요.
      </p>

      <form id="bean-form" className="stack" style={{ '--gap': '18px' } as CSSProperties} onSubmit={submit}>
        <label className="field">
          <span>로스터리 · 카페</span>
          <select
            className="select"
            value={roasteryId}
            onChange={(e) => setRoasteryId(e.target.value === 'new' ? 'new' : e.target.value === '' ? '' : Number(e.target.value))}
          >
            <option value="">선택해 주세요</option>
            {roasteries.data?.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
                {r.region ? ` · ${r.region}` : ''}
              </option>
            ))}
            <option value="new">+ 목록에 없어요 (직접 입력)</option>
          </select>
          {newRoastery && (
            <input className="input" placeholder="로스터리 또는 카페 이름" maxLength={100} value={roasteryName} onChange={(e) => setRoasteryName(e.target.value)} />
          )}
        </label>

        <label className="field">
          <span>원두 이름</span>
          <input className="input" placeholder="예: 에티오피아 구지 함벨라" maxLength={100} value={name} onChange={(e) => setName(e.target.value)} />
        </label>

        {duplicates.data && duplicates.data.length > 0 && (
          <div className="card flat dup-box">
            <p className="small">
              <Icon name="info" size={15} /> 혹시 이 원두인가요? 이미 있다면 골라 주세요.
            </p>
            <ul>
              {duplicates.data.map((d) => (
                <li key={d.id}>
                  <Link to={fromNote ? `/notes/new?beanId=${d.id}` : `/beans/${d.id}`} className="pick-row">
                    <span className="pick-text">
                      <strong>{d.name}</strong>
                      <span className="tiny muted">{d.roasteryName}</span>
                    </span>
                    <Icon name="chevron" size={16} />
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        )}

        <div className="field">
          <span>원산지</span>
          <div className="segmented">
            <button type="button" className={!blend ? 'active' : ''} onClick={() => setBlend(false)}>
              싱글오리진
            </button>
            <button type="button" className={blend ? 'active' : ''} onClick={() => setBlend(true)}>
              블렌드
            </button>
          </div>
          {!blend ? (
            <select className="select" value={origin} onChange={(e) => setOrigin(e.target.value)}>
              <option value="">나라를 골라 주세요</option>
              {ORIGINS.map(([code, label]) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </select>
          ) : (
            <div className="chips">
              {ORIGINS.slice(0, 15).map(([code, label]) => (
                <button
                  type="button"
                  key={code}
                  className={`chip neutral ${blendOrigins.includes(code) ? 'selected' : ''}`}
                  style={{ '--c': 'var(--roast)' } as CSSProperties}
                  onClick={() =>
                    setBlendOrigins(
                      blendOrigins.includes(code)
                        ? blendOrigins.filter((c) => c !== code)
                        : blendOrigins.length < 5
                          ? [...blendOrigins, code]
                          : blendOrigins,
                    )
                  }
                >
                  {label}
                </button>
              ))}
              <p className="tiny muted" style={{ width: '100%' }}>섞인 나라를 2개 이상 골라 주세요.</p>
            </div>
          )}
        </div>

        <label className="field">
          <span>지역 (선택)</span>
          <input className="input" placeholder="예: 예가체프, 우일라" maxLength={100} value={region} onChange={(e) => setRegion(e.target.value)} />
        </label>

        <div className="field">
          <span>가공 방식</span>
          <div className="option-grid">
            {PROCESSES.map((p) => (
              <button type="button" key={p} className={`option ${process === p ? 'selected' : ''}`} onClick={() => setProcess(p)}>
                <strong>{PROCESS_NAMES[p]}</strong>
                <span className="muted">{PROCESS_HINTS[p]}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="field">
          <span>로스팅</span>
          <div className="roast-scale">
            {ROAST_LEVELS.map((r) => (
              <button type="button" key={r} className={roast === r ? 'active' : ''} onClick={() => setRoast(r)}>
                <RoastDots level={r} />
                <span>{ROAST_NAMES[r]}</span>
              </button>
            ))}
          </div>
        </div>

        <label className="field">
          <span>품종 (선택, 쉼표로 구분)</span>
          <input className="input" placeholder="예: 게이샤, 버번" value={varieties} onChange={(e) => setVarieties(e.target.value)} />
        </label>

        <label className="field">
          <span>로스터리 향미 설명 (선택)</span>
          <input className="input" placeholder="예: 자스민, 복숭아, 꿀" maxLength={500} value={flavorText} onChange={(e) => setFlavorText(e.target.value)} />
          <span className="tiny muted">적어 주시면 표준 향미 태그로 자동 정리해요.</span>
        </label>

        {duplicateBlocked && (
          <Notice icon="info">
            비슷한 원두가 이미 있어요. 위 목록을 확인하고, 다른 원두가 맞다면 그대로 등록해 주세요.
            <button type="button" className="btn btn-secondary btn-sm" style={{ marginTop: 10 }} onClick={() => create.mutate(true)}>
              그래도 등록하기
            </button>
          </Notice>
        )}
        {create.isError && !duplicateBlocked && <p className="error-text">{errorMessage(create.error)}</p>}
      </form>

      <div className="action-bar">
        <button type="submit" form="bean-form" className="btn btn-primary btn-block" disabled={!valid || create.isPending}>
          {create.isPending ? '등록 중…' : fromNote ? '등록하고 기록 이어 쓰기' : '원두 등록'}
        </button>
      </div>
    </main>
  )
}
