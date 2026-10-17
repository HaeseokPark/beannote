import type { Axes, BrewMethod, Visibility } from '../api/types.ts'
import { todayIso } from './format.ts'

/** 노트 작성 폼 상태. 작성 중에는 기기에 임시 저장해 두었다가 이어 쓸 수 있다 (8-2 임시 저장) */
export type NoteForm = {
  bean: { id: number; name: string; roasteryName: string } | null
  tastedOn: string
  brewMethod: BrewMethod
  recipe: { doseG: string; waterG: string; waterTempC: string; grindText: string; brewTimeSec: string }
  scores: Axes
  flavorTagIds: number[]
  freeFlavors: string[]
  rating: number
  wouldBuyAgain: boolean | null
  memo: string
  visibility: Visibility
}

export function emptyForm(): NoteForm {
  return {
    bean: null,
    tastedOn: todayIso(),
    brewMethod: 'POUR_OVER',
    recipe: { doseG: '', waterG: '', waterTempC: '', grindText: '', brewTimeSec: '' },
    scores: { acidity: 3, sweetness: 3, body: 3, bitterness: 3, aftertaste: 3 },
    flavorTagIds: [],
    freeFlavors: [],
    rating: 0,
    wouldBuyAgain: null,
    memo: '',
    visibility: 'PRIVATE',
  }
}

const KEY = 'beanote.noteDraft'

export const noteDraft = {
  load(): { form: NoteForm; step: number } | null {
    try {
      const raw = localStorage.getItem(KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  },
  save(form: NoteForm, step: number) {
    try {
      localStorage.setItem(KEY, JSON.stringify({ form, step }))
    } catch {
      // 저장소를 못 쓰면 임시 저장 없이 진행
    }
  },
  clear() {
    try {
      localStorage.removeItem(KEY)
    } catch {
      // 무시
    }
  },
}
