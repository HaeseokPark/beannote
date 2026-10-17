import { useMutation } from '@tanstack/react-query'
import { useState, type CSSProperties, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { errorMessage } from '../api/client.ts'
import { authApi } from '../api/endpoints.ts'
import { startSession } from '../auth/session.tsx'
import { BeanMark } from '../components/CoffeeArt.tsx'
import { TopBar } from '../components/ui.tsx'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/home'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const login = useMutation({
    mutationFn: () => authApi.login({ email, password }),
    onSuccess: (tokens) => {
      startSession(tokens)
      navigate(from, { replace: true })
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    login.mutate()
  }

  return (
    <main className="page auth-page">
      <TopBar />
      <AuthHead title="다시 만나서 반가워요" text="오늘은 어떤 커피를 마셨나요?" />
      <form className="stack" onSubmit={submit} style={{ '--gap': '14px' } as CSSProperties}>
        <label className="field">
          <span>이메일</span>
          <input
            className="input"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <label className="field">
          <span>비밀번호</span>
          <input
            className="input"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {login.isError && <p className="error-text">{errorMessage(login.error)}</p>}
        <button className="btn btn-primary btn-block" disabled={login.isPending}>
          {login.isPending ? '로그인 중…' : '로그인'}
        </button>
      </form>
      <p className="center small muted section">
        처음이신가요?{' '}
        <Link className="text-link" to="/signup">
          가입하기
        </Link>
      </p>
    </main>
  )
}

export function SignupPage() {
  const navigate = useNavigate()
  const [nickname, setNickname] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const passwordOk = password.length >= 8 && /[A-Za-z]/.test(password) && /\d/.test(password)

  const signup = useMutation({
    mutationFn: () => authApi.signup({ email, password, nickname: nickname.trim() }),
    onSuccess: (tokens) => {
      startSession(tokens)
      navigate('/onboarding', { replace: true })
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (passwordOk) signup.mutate()
  }

  return (
    <main className="page auth-page">
      <TopBar />
      <AuthHead title="커피 노트를 시작해요" text="가입하고 간단한 취향 설문을 하면 바로 추천을 받아요." />
      <form className="stack" onSubmit={submit} style={{ '--gap': '14px' } as CSSProperties}>
        <label className="field">
          <span>닉네임</span>
          <input
            className="input"
            required
            minLength={2}
            maxLength={20}
            placeholder="2~20자"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
          />
        </label>
        <label className="field">
          <span>이메일</span>
          <input
            className="input"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <label className="field">
          <span>비밀번호</span>
          <input
            className="input"
            type="password"
            autoComplete="new-password"
            required
            placeholder="영문과 숫자를 섞어 8자 이상"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {password && !passwordOk && <span className="error-text">영문과 숫자를 섞어 8자 이상 입력해 주세요.</span>}
        </label>
        {signup.isError && <p className="error-text">{errorMessage(signup.error)}</p>}
        <button className="btn btn-primary btn-block" disabled={signup.isPending || !passwordOk}>
          {signup.isPending ? '가입 중…' : '가입하기'}
        </button>
      </form>
      <p className="center small muted section">
        이미 계정이 있나요?{' '}
        <Link className="text-link" to="/login">
          로그인
        </Link>
      </p>
    </main>
  )
}

function AuthHead({ title, text }: { title: string; text: string }) {
  return (
    <div className="auth-head">
      <BeanMark size={40} />
      <h1>{title}</h1>
      <p className="muted">{text}</p>
    </div>
  )
}
