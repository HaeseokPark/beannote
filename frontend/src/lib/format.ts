/** 오늘 날짜 (사용자 기기 시간대) YYYY-MM-DD */
export function todayIso(): string {
  const now = new Date()
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 10)
}

/** 2026-10-17 → 10월 17일 (올해가 아니면 연도 포함) */
export function formatDate(iso: string): string {
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number)
  const thisYear = new Date().getFullYear()
  return y === thisYear ? `${m}월 ${d}일` : `${y}년 ${m}월 ${d}일`
}

export function formatRating(value: number | null | undefined): string {
  return value == null ? '-' : Number(value).toFixed(1)
}

/** 원두량·물량 비율. 15g / 240g → 1:16 */
export function brewRatio(doseG?: number | null, waterG?: number | null): string | null {
  if (!doseG || !waterG) return null
  return `1:${(waterG / doseG).toFixed(1).replace(/\.0$/, '')}`
}

export function formatSeconds(seconds?: number | null): string | null {
  if (!seconds) return null
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m ? `${m}분${s ? ` ${s}초` : ''}` : `${s}초`
}

export function greeting(): string {
  const hour = new Date().getHours()
  if (hour < 11) return '좋은 아침이에요'
  if (hour < 17) return '향긋한 오후예요'
  return '편안한 저녁이에요'
}
