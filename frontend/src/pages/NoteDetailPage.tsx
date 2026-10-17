import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type CSSProperties } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { beanApi, noteApi } from '../api/endpoints.ts'
import type { NoteDetail } from '../api/types.ts'
import { Icon } from '../components/Icon.tsx'
import { Stars } from '../components/Stars.tsx'
import { TasteRadar } from '../components/TasteRadar.tsx'
import { useToast } from '../components/Toast.tsx'
import { ConfirmDialog, ErrorState, FlavorChip, FlavorChips, Loading, TopBar } from '../components/ui.tsx'
import { brewRatio, formatDate, formatSeconds } from '../lib/format.ts'
import { BREW_NAMES } from '../lib/labels.ts'

/** 노트 상세·수정·삭제 (N-3) */
export function NoteDetailPage() {
  const id = Number(useParams().id)
  const note = useQuery({ queryKey: ['note', id], queryFn: () => noteApi.detail(id) })

  return (
    <main className="page with-tabs">
      <TopBar title="시음 노트" />
      {note.isPending && <Loading />}
      {note.isError && <ErrorState error={note.error} />}
      {note.data && <NoteView note={note.data} />}
    </main>
  )
}

function NoteView({ note }: { note: NoteDetail }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [confirming, setConfirming] = useState(false)
  // 원두 평균과 비교해 보여 준다
  const bean = useQuery({ queryKey: ['bean', note.bean.id], queryFn: () => beanApi.detail(note.bean.id) })
  const average = bean.data?.stats.axes && bean.data.stats.noteCount > 1 ? bean.data.stats.axes : null

  const remove = useMutation({
    mutationFn: () => noteApi.remove(note.id),
    onSuccess: () => {
      queryClient.invalidateQueries()
      toast('노트를 지웠어요')
      navigate('/notes', { replace: true })
    },
  })

  const r = note.recipe
  const recipeItems = [
    r?.doseG && { label: '원두', value: `${r.doseG}g` },
    r?.waterG && { label: '물', value: `${r.waterG}g` },
    brewRatio(r?.doseG, r?.waterG) && { label: '비율', value: brewRatio(r?.doseG, r?.waterG) },
    r?.waterTempC && { label: '물 온도', value: `${r.waterTempC}℃` },
    r?.brewTimeSec && { label: '추출 시간', value: formatSeconds(r.brewTimeSec) },
    r?.grindText && { label: '분쇄도', value: r.grindText },
  ].filter(Boolean) as { label: string; value: string }[]

  const confidentFree = note.freeFlavors.filter((f) => f.tag && (f.result === 'EXACT' || f.result === 'AUTO'))

  return (
    <>
      <section className="card note-hero">
        <p className="tiny muted">
          {formatDate(note.tastedOn)} · {BREW_NAMES[note.brewMethod]}
          {!note.mine && ` · ${note.author.nickname}님의 노트`}
        </p>
        <Link to={`/beans/${note.bean.id}`} className="note-bean">
          <h1>{note.bean.name}</h1>
          <Icon name="chevron" size={18} />
        </Link>
        <p className="small muted">{note.bean.roasteryName}</p>
        <div className="row" style={{ marginTop: 14, '--gap': '10px' } as CSSProperties}>
          <Stars value={Number(note.rating)} size={22} />
          <span className="rating-big serif">{Number(note.rating).toFixed(1)}</span>
          {note.wouldBuyAgain !== null && (
            <span className={`badge ${note.wouldBuyAgain ? 'good' : 'ink'}`}>
              {note.wouldBuyAgain ? '또 마실래요' : '한 번이면 충분'}
            </span>
          )}
        </div>
      </section>

      <section className="section card flat">
        <div className="section-title">
          <h2>맛</h2>
          {note.visibility === 'PRIVATE' ? (
            <span className="badge ink"><Icon name="lock" size={12} /> 나만 보기</span>
          ) : (
            <span className="badge"><Icon name="globe" size={12} /> 공개</span>
          )}
        </div>
        <TasteRadar value={note.scores} compare={average} compareLabel="이 원두 평균" />
      </section>

      {(note.flavors.length > 0 || note.freeFlavors.length > 0) && (
        <section className="section">
          <div className="section-title">
            <h2>향</h2>
          </div>
          <FlavorChips flavors={note.flavors} />
          {note.freeFlavors.length > 0 && (
            <div className="free-flavors">
              {note.freeFlavors.map((f) => (
                <span key={f.raw} className="free-flavor">
                  “{f.raw}”
                  {f.tag && f.result !== 'UNMAPPED' && (
                    <>
                      <Icon name="chevron" size={12} />
                      <FlavorChip name={`${f.tag.name}${f.result === 'REVIEW' ? '?' : ''}`} category={f.tag.categoryCode} />
                    </>
                  )}
                </span>
              ))}
            </div>
          )}
          {confidentFree.length > 0 && (
            <p className="tiny muted" style={{ marginTop: 8 }}>
              직접 쓴 표현은 표준 향미로 바꿔서 취향 분석에 참고해요.
            </p>
          )}
        </section>
      )}

      {recipeItems.length > 0 && (
        <section className="section">
          <div className="section-title">
            <h2>레시피</h2>
          </div>
          <dl className="card flat kv">
            {recipeItems.map((item) => (
              <div key={item.label}>
                <dt>{item.label}</dt>
                <dd>{item.value}</dd>
              </div>
            ))}
          </dl>
        </section>
      )}

      {note.memo && (
        <section className="section">
          <div className="section-title">
            <h2>메모</h2>
          </div>
          <p className="card flat memo">{note.memo}</p>
        </section>
      )}

      {note.mine && (
        <section className="section row" style={{ '--gap': '10px' } as CSSProperties}>
          <Link to={`/notes/${note.id}/edit`} className="btn btn-outline" style={{ flex: 1 }}>
            <Icon name="edit" size={18} /> 고치기
          </Link>
          <button className="btn btn-danger" style={{ flex: 1 }} onClick={() => setConfirming(true)}>
            <Icon name="trash" size={18} /> 지우기
          </button>
        </section>
      )}

      {confirming && (
        <ConfirmDialog
          title="이 노트를 지울까요?"
          message="지운 노트는 되돌릴 수 없고, 취향 분석에서도 빠져요."
          confirmLabel={remove.isPending ? '지우는 중…' : '지우기'}
          danger
          onConfirm={() => remove.mutate()}
          onCancel={() => setConfirming(false)}
        />
      )}
    </>
  )
}
