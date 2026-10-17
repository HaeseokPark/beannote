import type { CSSProperties } from 'react'

type Props = {
  name: string
  levels: readonly string[]
  value: number
  onChange: (value: number) => void
  /** 설문처럼 "원하는 정도"를 물을 때 앞에 붙는 말 */
  prefix?: string
}

/** 맛 축 1~5 슬라이더. 점수 아래 쉬운 설명을 보여 준다 (3-1) */
export function TasteSlider({ name, levels, value, onChange, prefix }: Props) {
  const fill = `${((value - 1) / 4) * 100}%`
  return (
    <div className="taste-slider">
      <header>
        <strong>{name}</strong>
        <span className="value">{value}</span>
      </header>
      <input
        className="range"
        type="range"
        min={1}
        max={5}
        step={1}
        value={value}
        aria-label={name}
        aria-valuetext={`${value}점, ${levels[value - 1]}`}
        style={{ '--fill': fill } as CSSProperties}
        onChange={(e) => onChange(Number(e.target.value))}
      />
      <div className="range-scale" aria-hidden="true">
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n}>{n}</span>
        ))}
      </div>
      <p className="desc">
        {prefix}
        {levels[value - 1]}
      </p>
    </div>
  )
}
