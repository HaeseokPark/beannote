import { lazy, Suspense } from 'react'
import type { Props } from './TasteRadarChart.tsx'

// 차트 라이브러리(recharts)는 크기가 커서 처음 화면에 필요할 때 따로 불러온다
const TasteRadarChart = lazy(() => import('./TasteRadarChart.tsx'))

export function TasteRadar(props: Props) {
  return (
    <Suspense fallback={<div className="skeleton" style={{ height: props.height ?? 240 }} />}>
      <TasteRadarChart {...props} />
    </Suspense>
  )
}
