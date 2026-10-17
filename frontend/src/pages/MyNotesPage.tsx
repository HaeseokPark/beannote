import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { useState, type CSSProperties } from 'react'
import { Link } from 'react-router'
import { flavorApi, noteApi } from '../api/endpoints.ts'
import type { CategoryCode, NoteSummary } from '../api/types.ts'
import { Icon } from '../components/Icon.tsx'
import { Stars } from '../components/Stars.tsx'
import { Empty, ErrorState, FlavorChip, FlavorChips, Loading, LoadMore } from '../components/ui.tsx'
import { formatDate } from '../lib/format.ts'
import { AXES, BREW_NAMES, flavorColor } from '../lib/labels.ts'

/** 내 노트 목록: 최신순, 별점·향미 필터, 무한 스크롤 (N-2) */
export function MyNotesPage() {
  const [category, setCategory] = useState<CategoryCode | null>(null)
  const [highRated, setHighRated] = useState(false)
  const categories = useQuery({ queryKey: ['flavors'], queryFn: flavorApi.all, staleTime: Infinity })

  const notes = useInfiniteQuery({
    queryKey: ['my-notes', category, highRated],
    queryFn: ({ pageParam }) =>
      noteApi.mine({ flavorCategory: category ?? undefined, minRating: highRated ? 4 : undefined, cursor: pageParam }),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  })
  const items = notes.data?.pages.flatMap((p) => p.items) ?? []
  const filtered = category !== null || highRated

  return (
    <main className="page with-tabs">
      <header className="topbar left">
        <h1>내 노트</h1>
      </header>

      <div className="chips scroll" style={{ marginBottom: 16 }}>
        <button className={`chip lg neutral ${highRated ? 'selected' : ''}`} onClick={() => setHighRated(!highRated)}
          style={{ '--c': 'var(--crema)' } as CSSProperties}>
          ★ 4점 이상
        </button>
        {categories.data?.map((c) => (
          <FlavorChip
            key={c.code}
            name={c.name}
            category={c.code}
            size="lg"
            selected={category === c.code}
            onClick={() => setCategory(category === c.code ? null : c.code)}
          />
        ))}
      </div>

      {notes.isPending && <Loading />}
      {notes.isError && <ErrorState error={notes.error} onRetry={() => notes.refetch()} />}
      {notes.isSuccess && items.length === 0 && (
        filtered ? (
          <Empty title="조건에 맞는 노트가 없어요">필터를 바꿔 보세요.</Empty>
        ) : (
          <Empty title="첫 번째 커피를 기록해 볼까요?" action={<Link className="btn btn-primary" to="/notes/new">기록하기</Link>}>
            노트가 3개 모이면 취향이 보이기 시작해요.
          </Empty>
        )
      )}

      <ul className="stack note-list">
        {items.map((note) => (
          <li key={note.id}>
            <NoteCard note={note} />
          </li>
        ))}
      </ul>
      <LoadMore onVisible={() => notes.fetchNextPage()} loading={notes.isFetchingNextPage} done={!notes.hasNextPage} />
    </main>
  )
}

function NoteCard({ note }: { note: NoteSummary }) {
  const accent = note.flavors[0]?.categoryCode
  return (
    <Link
      to={`/notes/${note.id}`}
      className="card tappable note-card"
      style={{ '--accent': accent ? flavorColor(accent) : 'var(--crema)' } as CSSProperties}
    >
      <div className="row between">
        <span className="tiny muted">
          {formatDate(note.tastedOn)} · {BREW_NAMES[note.brewMethod]}
        </span>
        {note.visibility === 'PRIVATE' ? (
          <span className="tiny muted row" style={{ '--gap': '3px' } as CSSProperties}>
            <Icon name="lock" size={13} /> 나만
          </span>
        ) : (
          <span className="tiny muted row" style={{ '--gap': '3px' } as CSSProperties}>
            <Icon name="globe" size={13} /> 공개
          </span>
        )}
      </div>
      <h3 className="card-title">{note.bean.name}</h3>
      <div className="row between">
        <span className="small muted">{note.bean.roasteryName}</span>
        <span className="row" style={{ '--gap': '6px' } as CSSProperties}>
          <Stars value={Number(note.rating)} />
          <span className="rating-number small">{Number(note.rating).toFixed(1)}</span>
        </span>
      </div>
      <div className="mini-axes">
        {AXES.map((axis) => (
          <div key={axis.key} className="mini-axis">
            <span>{axis.name}</span>
            <div className="mini-dots">
              {[1, 2, 3, 4, 5].map((n) => (
                <i key={n} className={n <= note.scores[axis.key] ? 'on' : ''} />
              ))}
            </div>
          </div>
        ))}
      </div>
      {note.flavors.length > 0 && <FlavorChips flavors={note.flavors} max={4} />}
    </Link>
  )
}
