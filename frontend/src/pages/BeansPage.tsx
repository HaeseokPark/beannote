import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { useState, type CSSProperties } from 'react'
import { Link } from 'react-router'
import { beanApi, flavorApi } from '../api/endpoints.ts'
import type { BeanSort, CategoryCode, ProcessMethod, RoastLevel } from '../api/types.ts'
import { BeanCard } from '../components/BeanCard.tsx'
import { Icon } from '../components/Icon.tsx'
import { Empty, ErrorState, FlavorChip, Loading, LoadMore } from '../components/ui.tsx'
import { useDebounced } from '../lib/hooks.ts'
import { PROCESS_NAMES, ROAST_LEVELS, ROAST_NAMES } from '../lib/labels.ts'

const SORTS: { value: BeanSort; name: string }[] = [
  { value: 'POPULAR', name: '인기' },
  { value: 'RATING', name: '평점' },
  { value: 'LATEST', name: '최신' },
  { value: 'NAME', name: '이름' },
]

const PROCESSES: ProcessMethod[] = ['WASHED', 'NATURAL', 'HONEY', 'ANAEROBIC']

/** 원두 찾기: 검색, 필터 칩, 정렬, 무한 스크롤 (B-1) */
export function BeansPage() {
  const [q, setQ] = useState('')
  const [sort, setSort] = useState<BeanSort>('POPULAR')
  const [process, setProcess] = useState<ProcessMethod | null>(null)
  const [roast, setRoast] = useState<RoastLevel | null>(null)
  const [category, setCategory] = useState<CategoryCode | null>(null)
  const query = useDebounced(q.trim(), 300)
  const categories = useQuery({ queryKey: ['flavors'], queryFn: flavorApi.all, staleTime: Infinity })

  const beans = useInfiniteQuery({
    queryKey: ['beans', query, sort, process, roast, category],
    queryFn: ({ pageParam }) =>
      beanApi.search({
        q: query || undefined,
        sort,
        process: process ?? undefined,
        roastLevel: roast ?? undefined,
        flavorCategory: category ?? undefined,
        cursor: pageParam,
        size: 12,
      }),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  })
  const items = beans.data?.pages.flatMap((p) => p.items) ?? []
  const filtered = Boolean(query || process || roast || category)

  return (
    <main className="page with-tabs">
      <header className="topbar left">
        <h1>원두 찾기</h1>
        <Link to="/beans/new" className="icon-btn" aria-label="원두 등록">
          <Icon name="plus" />
        </Link>
      </header>

      <div className="search">
        <Icon name="search" size={20} />
        <input
          className="input"
          type="search"
          placeholder="원두, 로스터리, 원산지 (예: 에티오피아)"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      <div className="filters">
        <div className="chips scroll">
          {categories.data?.map((c) => (
            <FlavorChip
              key={c.code}
              name={c.name}
              category={c.code}
              selected={category === c.code}
              onClick={() => setCategory(category === c.code ? null : c.code)}
            />
          ))}
        </div>
        <div className="chips scroll">
          {PROCESSES.map((p) => (
            <button
              key={p}
              className={`chip neutral ${process === p ? 'selected' : ''}`}
              style={{ '--c': 'var(--roast)' } as CSSProperties}
              onClick={() => setProcess(process === p ? null : p)}
            >
              {PROCESS_NAMES[p]}
            </button>
          ))}
          <span className="chip-sep" />
          {ROAST_LEVELS.map((r) => (
            <button
              key={r}
              className={`chip neutral ${roast === r ? 'selected' : ''}`}
              style={{ '--c': 'var(--roast)' } as CSSProperties}
              onClick={() => setRoast(roast === r ? null : r)}
            >
              {ROAST_NAMES[r]}
            </button>
          ))}
        </div>
      </div>

      <div className="segmented" style={{ marginBottom: 16 }}>
        {SORTS.map((s) => (
          <button key={s.value} className={sort === s.value ? 'active' : ''} onClick={() => setSort(s.value)}>
            {s.name}
          </button>
        ))}
      </div>

      {beans.isPending && <Loading />}
      {beans.isError && <ErrorState error={beans.error} onRetry={() => beans.refetch()} />}
      {beans.isSuccess && items.length === 0 && (
        <Empty
          title={filtered ? '찾는 원두가 없어요' : '아직 원두가 없어요'}
          action={
            <Link className="btn btn-primary btn-sm" to="/beans/new">
              직접 등록하기
            </Link>
          }
        >
          카탈로그에 없는 원두는 직접 등록할 수 있어요.
        </Empty>
      )}

      <ul className="bean-grid">
        {items.map((bean) => (
          <li key={bean.id}>
            <BeanCard bean={bean} />
          </li>
        ))}
      </ul>
      <LoadMore onVisible={() => beans.fetchNextPage()} loading={beans.isFetchingNextPage} done={!beans.hasNextPage} />
    </main>
  )
}
