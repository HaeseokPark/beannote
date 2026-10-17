import { useQuery } from '@tanstack/react-query'
import { fetchHealth } from '../api/client.ts'

export default function HomePage() {
  const health = useQuery({ queryKey: ['health'], queryFn: fetchHealth })

  return (
    <section className="page">
      <h1 className="page-title">오늘은 어떤 커피를 마셨나요?</h1>
      <p className="muted">시음 노트를 쓰면 취향에 맞는 원두를 이유와 함께 추천해 드려요.</p>

      <div className="card">
        <div className="card-title">서버 상태</div>
        {health.isPending && <p className="muted">확인 중…</p>}
        {health.isError && <p className="status bad">연결 안 됨 — 백엔드를 실행해 주세요</p>}
        {health.isSuccess && (
          <p className={health.data === 'UP' ? 'status good' : 'status bad'}>{health.data}</p>
        )}
      </div>
    </section>
  )
}
