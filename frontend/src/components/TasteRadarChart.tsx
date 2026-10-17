import { PolarAngleAxis, PolarGrid, PolarRadiusAxis, Radar, RadarChart, ResponsiveContainer } from 'recharts'
import type { Axes } from '../api/types.ts'
import { AXES } from '../lib/labels.ts'

export type Props = {
  value: Axes
  /** 비교용 두 번째 값 (예: 원두 평균 vs 내 노트) */
  compare?: Axes | null
  compareLabel?: string
  height?: number
}

/**
 * 맛 축 레이더. 색만으로 구분하지 않도록 축 이름 옆에 수치를 함께 표시한다 (8-3 접근성).
 */
export default function TasteRadarChart({ value, compare, compareLabel, height = 240 }: Props) {
  const data = AXES.map((axis) => ({
    axis: `${axis.name} ${Number(value[axis.key]).toFixed(1)}`,
    value: Number(value[axis.key]),
    compare: compare ? Number(compare[axis.key]) : undefined,
  }))

  return (
    <figure className="radar" aria-label={AXES.map((a) => `${a.name} ${value[a.key]}`).join(', ')}>
      <ResponsiveContainer width="100%" height={height}>
        <RadarChart data={data} outerRadius="70%" margin={{ top: 8, right: 24, bottom: 8, left: 24 }}>
          <PolarGrid stroke="var(--line-strong)" />
          <PolarAngleAxis
            dataKey="axis"
            tick={{ fill: 'var(--ink-2)', fontSize: 12, fontWeight: 600 }}
            tickLine={false}
          />
          <PolarRadiusAxis domain={[0, 5]} tickCount={6} tick={false} axisLine={false} />
          {compare && (
            <Radar
              dataKey="compare"
              stroke="var(--ink-3)"
              strokeDasharray="4 4"
              fill="var(--ink-3)"
              fillOpacity={0.08}
              isAnimationActive={false}
            />
          )}
          <Radar
            dataKey="value"
            stroke="var(--roast)"
            strokeWidth={2}
            fill="var(--crema)"
            fillOpacity={0.35}
            dot={{ r: 3, fill: 'var(--roast)', strokeWidth: 0 }}
          />
        </RadarChart>
      </ResponsiveContainer>
      {compare && compareLabel && (
        <figcaption className="radar-legend">
          <span className="swatch solid" /> 이 노트
          <span className="swatch dashed" /> {compareLabel}
        </figcaption>
      )}
    </figure>
  )
}
