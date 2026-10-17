import { useQuery } from '@tanstack/react-query'
import { useMemo, useState, type CSSProperties } from 'react'
import { flavorApi } from '../api/endpoints.ts'
import type { CategoryCode, FlavorCategory, FlavorMatch } from '../api/types.ts'
import { useDebounced } from '../lib/hooks.ts'
import { CATEGORY_HINTS, flavorColor } from '../lib/labels.ts'
import { Icon } from './Icon.tsx'
import { FlavorChip } from './ui.tsx'

const MAX_TAGS = 5
const MAX_FREE = 5

type Props = {
  categories: FlavorCategory[]
  selected: number[]
  onSelectedChange: (ids: number[]) => void
  freeFlavors: string[]
  onFreeFlavorsChange: (texts: string[]) => void
}

/**
 * 향미 고르기: 대분류 → 소분류 (최대 5개). 자유 입력은 표준화 API 로 태그 후보를 제안한다 (4-6).
 */
export function FlavorPicker({ categories, selected, onSelectedChange, freeFlavors, onFreeFlavorsChange }: Props) {
  const [active, setActive] = useState<CategoryCode>(categories[0]?.code ?? 'FLORAL')
  const [text, setText] = useState('')
  const debounced = useDebounced(text.trim(), 350)

  const tagIndex = useMemo(() => {
    const map = new Map<number, { name: string; category: CategoryCode }>()
    categories.forEach((c) => c.tags.forEach((t) => map.set(t.id, { name: t.name, category: c.code })))
    return map
  }, [categories])

  const suggestions = useQuery({
    queryKey: ['normalize', debounced],
    queryFn: () => flavorApi.normalize(debounced),
    enabled: debounced.length > 0,
    staleTime: 60_000,
  })

  const full = selected.length >= MAX_TAGS
  const toggle = (id: number) => {
    if (selected.includes(id)) onSelectedChange(selected.filter((x) => x !== id))
    else if (!full) onSelectedChange([...selected, id])
  }

  const addFree = () => {
    const value = text.trim()
    if (!value || freeFlavors.includes(value) || freeFlavors.length >= MAX_FREE) return
    onFreeFlavorsChange([...freeFlavors, value])
    setText('')
  }

  const activeCategory = categories.find((c) => c.code === active)
  const tagSuggestions = (suggestions.data ?? []).filter(
    (m): m is FlavorMatch & { tag: NonNullable<FlavorMatch['tag']> } =>
      m.tag !== null && m.result !== 'UNMAPPED' && !selected.includes(m.tag.id),
  )

  return (
    <div className="flavor-picker stack" style={{ '--gap': '16px' } as CSSProperties}>
      <div className="picked">
        <div className="row between">
          <span className="label">고른 향</span>
          <span className={`small ${full ? 'rating-number' : 'muted'}`}>
            {selected.length} / {MAX_TAGS}
          </span>
        </div>
        {selected.length === 0 ? (
          <p className="small muted">아래에서 느껴진 향을 골라 주세요. 잘 모르겠으면 건너뛰어도 괜찮아요.</p>
        ) : (
          <div className="chips">
            {selected.map((id) => {
              const tag = tagIndex.get(id)
              return tag ? (
                <FlavorChip key={id} name={`${tag.name} ✕`} category={tag.category} selected onClick={() => toggle(id)} />
              ) : null
            })}
          </div>
        )}
      </div>

      <div className="flavor-wheel" role="tablist" aria-label="향미 대분류">
        {categories.map((c) => (
          <button
            key={c.code}
            role="tab"
            aria-selected={c.code === active}
            className={`wheel-item ${c.code === active ? 'active' : ''}`}
            style={{ '--c': flavorColor(c.code) } as CSSProperties}
            onClick={() => setActive(c.code)}
          >
            <span className="wheel-dot" />
            {c.name}
          </button>
        ))}
      </div>

      {activeCategory && (
        <div className="card flat tag-panel" style={{ '--c': flavorColor(activeCategory.code) } as CSSProperties}>
          <p className="small muted" style={{ marginBottom: 10 }}>
            {CATEGORY_HINTS[activeCategory.code]}
          </p>
          <div className="chips">
            {activeCategory.tags.map((tag) => (
              <FlavorChip
                key={tag.id}
                name={tag.name}
                category={activeCategory.code}
                size="lg"
                selected={selected.includes(tag.id)}
                onClick={() => toggle(tag.id)}
              />
            ))}
          </div>
          {full && <p className="tiny muted" style={{ marginTop: 10 }}>최대 5개까지 고를 수 있어요.</p>}
        </div>
      )}

      <div className="field">
        <span>직접 표현하기</span>
        <div className="row" style={{ '--gap': '8px' } as CSSProperties}>
          <input
            className="input"
            placeholder="예: 살짝 자두 같은 느낌"
            value={text}
            maxLength={50}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault()
                addFree()
              }
            }}
          />
          <button type="button" className="btn btn-secondary" style={{ flex: 'none' }} onClick={addFree} disabled={!text.trim()}>
            추가
          </button>
        </div>
        {tagSuggestions.length > 0 && (
          <div className="suggest">
            <Icon name="sparkle" size={15} />
            <span className="small muted">이 향을 고를까요?</span>
            <div className="chips">
              {tagSuggestions.map((m) => (
                <FlavorChip
                  key={m.tag.id}
                  name={`${m.tag.name}${m.result === 'REVIEW' ? '?' : ''}`}
                  category={m.tag.categoryCode}
                  onClick={() => toggle(m.tag.id)}
                />
              ))}
            </div>
          </div>
        )}
        {freeFlavors.length > 0 && (
          <div className="chips" style={{ marginTop: 4 }}>
            {freeFlavors.map((f) => (
              <button
                key={f}
                type="button"
                className="chip neutral"
                onClick={() => onFreeFlavorsChange(freeFlavors.filter((x) => x !== f))}
              >
                “{f}” ✕
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
