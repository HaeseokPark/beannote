import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type CSSProperties } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { errorMessage } from '../api/client.ts'
import { beanApi, flavorApi, noteApi } from '../api/endpoints.ts'
import type { NoteDetail, NoteRequest } from '../api/types.ts'
import { Icon } from '../components/Icon.tsx'
import { FlavorPicker } from '../components/FlavorPicker.tsx'
import { StarInput } from '../components/Stars.tsx'
import { TasteSlider } from '../components/TasteSlider.tsx'
import { useToast } from '../components/Toast.tsx'
import { Loading, Notice } from '../components/ui.tsx'
import { brewRatio, todayIso } from '../lib/format.ts'
import { useDebounced } from '../lib/hooks.ts'
import { AXES, BREW_METHODS } from '../lib/labels.ts'
import { emptyForm, noteDraft, type NoteForm } from '../lib/noteDraft.ts'

const STEPS = ['원두', '추출', '맛', '향', '마무리']

/** 노트 작성·수정 (단계형, 8-2). 새 노트는 단계마다 기기에 임시 저장한다 */
export function NoteWritePage() {
  const { id } = useParams()
  const editingId = id ? Number(id) : null
  const [params] = useSearchParams()
  const presetBeanId = params.get('beanId') ? Number(params.get('beanId')) : null

  const existing = useQuery({
    queryKey: ['note', editingId],
    queryFn: () => noteApi.detail(editingId!),
    enabled: editingId !== null,
  })

  if (editingId !== null) {
    if (existing.isPending) return <main className="page with-tabs"><Loading /></main>
    if (existing.data) return <NoteWizard key={editingId} editing={existing.data} presetBeanId={null} />
  }
  return <NoteWizard key={presetBeanId ?? 'new'} editing={null} presetBeanId={presetBeanId} />
}

function fromNote(note: NoteDetail): NoteForm {
  const r = note.recipe
  return {
    bean: note.bean,
    tastedOn: note.tastedOn,
    brewMethod: note.brewMethod,
    recipe: {
      doseG: r?.doseG?.toString() ?? '',
      waterG: r?.waterG?.toString() ?? '',
      waterTempC: r?.waterTempC?.toString() ?? '',
      grindText: r?.grindText ?? '',
      brewTimeSec: r?.brewTimeSec?.toString() ?? '',
    },
    scores: note.scores,
    flavorTagIds: note.flavors.map((f) => f.id),
    freeFlavors: note.freeFlavors.map((f) => f.raw),
    rating: Number(note.rating),
    wouldBuyAgain: note.wouldBuyAgain,
    memo: note.memo ?? '',
    visibility: note.visibility,
  }
}

function toRequest(form: NoteForm): NoteRequest {
  const num = (v: string) => (v.trim() === '' ? null : Number(v))
  const r = form.recipe
  const recipe = {
    doseG: num(r.doseG),
    waterG: num(r.waterG),
    waterTempC: num(r.waterTempC),
    grindText: r.grindText.trim() || null,
    brewTimeSec: num(r.brewTimeSec),
  }
  return {
    beanId: form.bean!.id,
    tastedOn: form.tastedOn,
    brewMethod: form.brewMethod,
    recipe: Object.values(recipe).some((v) => v !== null) ? recipe : null,
    scores: form.scores,
    flavorTagIds: form.flavorTagIds,
    freeFlavors: form.freeFlavors,
    rating: form.rating,
    wouldBuyAgain: form.wouldBuyAgain,
    memo: form.memo.trim() || null,
    visibility: form.visibility,
  }
}

function NoteWizard({ editing, presetBeanId }: { editing: NoteDetail | null; presetBeanId: number | null }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [draft] = useState(() => (editing ? null : noteDraft.load()))
  const [form, setForm] = useState<NoteForm>(() => (editing ? fromNote(editing) : emptyForm()))
  const [step, setStep] = useState(0)
  const [showDraft, setShowDraft] = useState(draft !== null && presetBeanId === null)

  const flavors = useQuery({ queryKey: ['flavors'], queryFn: flavorApi.all, staleTime: Infinity })
  const presetBean = useQuery({
    queryKey: ['bean', presetBeanId],
    queryFn: () => beanApi.detail(presetBeanId!),
    enabled: presetBeanId !== null,
  })

  // 원두 상세·추천에서 "이 원두 기록하기"로 들어오면 원두를 미리 고르고 다음 단계로
  useEffect(() => {
    const bean = presetBean.data
    if (bean) {
      setForm((f) => ({ ...f, bean: { id: bean.id, name: bean.name, roasteryName: bean.roastery.name } }))
      setStep(1)
    }
  }, [presetBean.data])

  useEffect(() => {
    // 원두를 고른 뒤부터 임시 저장한다 (빈 폼은 저장하지 않음)
    if (!editing && !showDraft && form.bean) noteDraft.save(form, step)
  }, [form, step, editing, showDraft])

  const save = useMutation({
    mutationFn: () => (editing ? noteApi.update(editing.id, toRequest(form)) : noteApi.create(toRequest(form))),
    onSuccess: (note) => {
      if (!editing) noteDraft.clear()
      queryClient.invalidateQueries()
      toast(editing ? '노트를 고쳤어요' : '오늘의 커피를 기록했어요 ☕')
      navigate(`/notes/${note.id}`, { replace: true })
    },
  })

  const update = <K extends keyof NoteForm>(key: K, value: NoteForm[K]) => setForm((f) => ({ ...f, [key]: value }))
  const canNext = [form.bean !== null, Boolean(form.tastedOn), true, true, form.rating > 0][step]
  const last = step === STEPS.length - 1

  return (
    <main className="page with-action note-write">
      <header className="topbar">
        <button
          className="icon-btn"
          aria-label={step > 0 ? '이전 단계' : '닫기'}
          onClick={() => (step > 0 ? setStep(step - 1) : navigate(-1))}
        >
          <Icon name={step > 0 ? 'back' : 'close'} />
        </button>
        <h1>{editing ? '노트 고치기' : '오늘의 커피 기록'}</h1>
      </header>

      <div className="step-head">
        <div className="progress">
          {STEPS.map((s, i) => (
            <span key={s} className={i <= step ? 'done' : ''} />
          ))}
        </div>
        <div className="step-names">
          {STEPS.map((s, i) => (
            <button key={s} className={i === step ? 'active' : ''} disabled={i > step} onClick={() => setStep(i)}>
              {s}
            </button>
          ))}
        </div>
      </div>

      {showDraft && draft && (
        <div className="notice draft-notice">
          <Icon name="notebook" size={18} />
          <div style={{ flex: 1 }}>
            <strong>작성 중이던 노트가 있어요</strong>
            <p className="small">{draft.form.bean?.name ?? '원두 선택 전'}</p>
            <div className="row" style={{ marginTop: 10 }}>
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  setForm(draft!.form)
                  setStep(draft!.step)
                  setShowDraft(false)
                }}
              >
                이어 쓰기
              </button>
              <button
                className="btn btn-ghost btn-sm"
                onClick={() => {
                  noteDraft.clear()
                  setShowDraft(false)
                }}
              >
                새로 쓰기
              </button>
            </div>
          </div>
        </div>
      )}

      {form.bean && step > 0 && (
        <div className="picked-bean">
          <Icon name="bean" size={18} />
          <span className="serif">{form.bean.name}</span>
          <span className="tiny muted">{form.bean.roasteryName}</span>
        </div>
      )}

      {step === 0 && <BeanStep form={form} onPick={(bean) => { update('bean', bean); setStep(1) }} />}
      {step === 1 && <BrewStep form={form} update={update} />}
      {step === 2 && (
        <section className="stack">
          <StepTitle title="어떤 맛이었나요?" text="느낀 대로 골라 주세요. 점수마다 쉬운 설명이 나와요." />
          {AXES.map((axis) => (
            <TasteSlider
              key={axis.key}
              name={axis.name}
              levels={axis.levels}
              value={form.scores[axis.key]}
              onChange={(v) => update('scores', { ...form.scores, [axis.key]: v })}
            />
          ))}
        </section>
      )}
      {step === 3 && (
        <section className="stack">
          <StepTitle title="어떤 향이 느껴졌나요?" text="최대 5개. 표현이 떠오르지 않으면 직접 적어도 돼요." />
          {flavors.data ? (
            <FlavorPicker
              categories={flavors.data}
              selected={form.flavorTagIds}
              onSelectedChange={(ids) => update('flavorTagIds', ids)}
              freeFlavors={form.freeFlavors}
              onFreeFlavorsChange={(texts) => update('freeFlavors', texts)}
            />
          ) : (
            <Loading lines={2} />
          )}
        </section>
      )}
      {step === 4 && <FinishStep form={form} update={update} />}

      {save.isError && <p className="error-text" style={{ marginTop: 12 }}>{errorMessage(save.error)}</p>}

      <div className="action-bar">
        {last ? (
          <button className="btn btn-primary btn-block" disabled={!canNext || save.isPending} onClick={() => save.mutate()}>
            {save.isPending ? '저장 중…' : editing ? '고친 내용 저장' : '기록 완료'}
          </button>
        ) : (
          <button className="btn btn-primary btn-block" disabled={!canNext} onClick={() => setStep(step + 1)}>
            다음 · {STEPS[step + 1]}
          </button>
        )}
      </div>
    </main>
  )
}

function StepTitle({ title, text }: { title: string; text: string }) {
  return (
    <div className="step-title">
      <h2>{title}</h2>
      <p className="small muted">{text}</p>
    </div>
  )
}

function BeanStep({ form, onPick }: { form: NoteForm; onPick: (bean: NonNullable<NoteForm['bean']>) => void }) {
  const [q, setQ] = useState('')
  const query = useDebounced(q.trim(), 300)
  const results = useQuery({
    queryKey: ['bean-pick', query],
    queryFn: () => beanApi.search({ q: query || undefined, sort: query ? 'NAME' : 'POPULAR', size: 8 }),
  })

  return (
    <section className="stack">
      <StepTitle title="어떤 원두를 마셨나요?" text="이름·로스터리·원산지로 찾아보세요." />
      <div className="search">
        <Icon name="search" size={20} />
        <input className="input" placeholder="예: 예가체프, 케냐, 모래시계" value={q} onChange={(e) => setQ(e.target.value)} autoFocus />
      </div>
      {!query && <p className="tiny muted">자주 기록되는 원두</p>}
      <ul className="pick-list">
        {results.data?.items.map((bean) => (
          <li key={bean.id}>
            <button
              className={`pick-row ${form.bean?.id === bean.id ? 'selected' : ''}`}
              onClick={() => onPick({ id: bean.id, name: bean.name, roasteryName: bean.roastery.name })}
            >
              <span className="pick-icon">
                <Icon name="bean" size={18} />
              </span>
              <span className="pick-text">
                <strong>{bean.name}</strong>
                <span className="tiny muted">
                  {bean.roastery.name} · {bean.blend ? '블렌드' : bean.origins.map((o) => o.name).join(', ')}
                </span>
              </span>
              {form.bean?.id === bean.id ? <Icon name="check" size={18} /> : <Icon name="chevron" size={18} />}
            </button>
          </li>
        ))}
      </ul>
      {results.data && results.data.items.length === 0 && <p className="small muted center">찾는 원두가 없어요.</p>}
      <Link className="btn btn-outline btn-block" to="/beans/new?from=note">
        <Icon name="plus" size={18} /> 찾는 원두가 없나요? 직접 등록하기
      </Link>
    </section>
  )
}

type Update = <K extends keyof NoteForm>(key: K, value: NoteForm[K]) => void

function BrewStep({ form, update }: { form: NoteForm; update: Update }) {
  const [open, setOpen] = useState(Object.values(form.recipe).some((v) => v !== ''))
  const setRecipe = (key: keyof NoteForm['recipe'], value: string) => update('recipe', { ...form.recipe, [key]: value })
  const ratio = brewRatio(Number(form.recipe.doseG), Number(form.recipe.waterG))

  return (
    <section className="stack" style={{ '--gap': '18px' } as CSSProperties}>
      <StepTitle title="어떻게 마셨나요?" text="같은 원두라도 추출 방식에 따라 맛이 달라져요." />
      <label className="field">
        <span>마신 날</span>
        <input className="input" type="date" max={todayIso()} value={form.tastedOn} onChange={(e) => update('tastedOn', e.target.value)} />
      </label>
      <div className="field">
        <span>추출 방식</span>
        <div className="brew-grid">
          {BREW_METHODS.map((b) => (
            <button
              key={b.value}
              className={`chip lg neutral ${form.brewMethod === b.value ? 'selected' : ''}`}
              style={{ '--c': 'var(--roast)' } as CSSProperties}
              onClick={() => update('brewMethod', b.value)}
            >
              {b.name}
            </button>
          ))}
        </div>
      </div>

      <div className="card flat recipe">
        <button className="row between recipe-toggle" onClick={() => setOpen(!open)} aria-expanded={open}>
          <span className="label">레시피 기록 (선택)</span>
          <span className="small muted">{open ? '접기' : ratio ? `비율 ${ratio}` : '펼치기'}</span>
        </button>
        {open && (
          <div className="recipe-grid">
            <RecipeInput icon="scale" label="원두" unit="g" value={form.recipe.doseG} onChange={(v) => setRecipe('doseG', v)} />
            <RecipeInput icon="drop" label="물" unit="g" value={form.recipe.waterG} onChange={(v) => setRecipe('waterG', v)} />
            <RecipeInput icon="thermo" label="물 온도" unit="℃" value={form.recipe.waterTempC} onChange={(v) => setRecipe('waterTempC', v)} />
            <RecipeInput icon="timer" label="추출 시간" unit="초" value={form.recipe.brewTimeSec} onChange={(v) => setRecipe('brewTimeSec', v)} />
            <label className="field" style={{ gridColumn: '1 / -1' }}>
              <span>분쇄도</span>
              <input
                className="input"
                placeholder="예: 중간, 코만단테 24클릭"
                maxLength={50}
                value={form.recipe.grindText}
                onChange={(e) => setRecipe('grindText', e.target.value)}
              />
            </label>
            {ratio && <p className="small muted" style={{ gridColumn: '1 / -1' }}>원두와 물 비율 <strong>{ratio}</strong></p>}
          </div>
        )}
      </div>
    </section>
  )
}

function RecipeInput({
  icon,
  label,
  unit,
  value,
  onChange,
}: {
  icon: 'scale' | 'drop' | 'thermo' | 'timer'
  label: string
  unit: string
  value: string
  onChange: (value: string) => void
}) {
  return (
    <label className="field recipe-input">
      <span>
        <Icon name={icon} size={14} /> {label}
      </span>
      <div className="with-unit">
        <input className="input" inputMode="decimal" value={value} onChange={(e) => onChange(e.target.value.replace(/[^\d.]/g, ''))} />
        <em>{unit}</em>
      </div>
    </label>
  )
}

function FinishStep({ form, update }: { form: NoteForm; update: Update }) {
  return (
    <section className="stack" style={{ '--gap': '20px' } as CSSProperties}>
      <StepTitle title="한 잔을 마무리해요" text="별점은 꼭 남겨 주세요. 취향 계산의 기준이 돼요." />
      <div className="card flat center rating-card">
        <StarInput value={form.rating} onChange={(v) => update('rating', v)} />
        <p className="rating-big serif">{form.rating > 0 ? form.rating.toFixed(1) : '별을 눌러 주세요'}</p>
        <p className="tiny muted">별의 왼쪽을 누르면 반 개</p>
      </div>

      <div className="field">
        <span>다시 마실 의향</span>
        <div className="segmented">
          <button className={form.wouldBuyAgain === true ? 'active' : ''} onClick={() => update('wouldBuyAgain', form.wouldBuyAgain === true ? null : true)}>
            또 마실래요
          </button>
          <button className={form.wouldBuyAgain === false ? 'active' : ''} onClick={() => update('wouldBuyAgain', form.wouldBuyAgain === false ? null : false)}>
            한 번이면 충분해요
          </button>
        </div>
      </div>

      <label className="field">
        <span className="row between">
          메모 <em className="tiny muted" style={{ fontStyle: 'normal' }}>{form.memo.length} / 1000</em>
        </span>
        <textarea
          className="textarea"
          maxLength={1000}
          placeholder="같이 먹은 디저트, 분위기, 다음에 바꿔 볼 레시피…"
          value={form.memo}
          onChange={(e) => update('memo', e.target.value)}
        />
      </label>

      <div className="field">
        <span>공개 범위</span>
        <div className="segmented">
          <button className={form.visibility === 'PRIVATE' ? 'active' : ''} onClick={() => update('visibility', 'PRIVATE')}>
            <Icon name="lock" size={14} /> 나만 보기
          </button>
          <button className={form.visibility === 'PUBLIC' ? 'active' : ''} onClick={() => update('visibility', 'PUBLIC')}>
            <Icon name="globe" size={14} /> 공개
          </button>
        </div>
        <Notice>비공개 노트는 나만 볼 수 있어요. 원두 평균 맛에는 이름 없이 반영돼요.</Notice>
      </div>
    </section>
  )
}
