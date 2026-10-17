import { useEffect, useState } from 'react'

/** 입력이 멈춘 뒤 delay ms 가 지나면 값을 반영한다 (검색어·표준화 요청 줄이기) */
export function useDebounced<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay)
    return () => window.clearTimeout(timer)
  }, [value, delay])
  return debounced
}
