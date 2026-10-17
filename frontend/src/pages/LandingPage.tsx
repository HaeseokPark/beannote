import { Link, Navigate } from 'react-router'
import { useSession } from '../auth/session.tsx'
import { BeanMark, CoffeeCupArt } from '../components/CoffeeArt.tsx'
import { Icon, type IconName } from '../components/Icon.tsx'
import { TasteRadar } from '../components/TasteRadar.tsx'
import { FlavorChip } from '../components/ui.tsx'

const FEATURES: { icon: IconName; title: string; text: string }[] = [
  { icon: 'notebook', title: '1분 시음 노트', text: '맛 5가지와 향만 고르면 끝. 어려운 용어는 쉬운 말로 알려 드려요.' },
  { icon: 'taste', title: '숫자로 보는 내 취향', text: '노트가 쌓일수록 산미·단맛·바디 취향이 또렷해져요.' },
  { icon: 'sparkle', title: '이유가 있는 추천', text: '"좋아하시는 밝은 산미가 특징이에요"처럼 왜 추천했는지 알려 드려요.' },
]

/** 랜딩: 서비스 소개, 예시 취향 프로필, 시작하기 (8-2) */
export function LandingPage() {
  const session = useSession()
  if (session) return <Navigate to="/home" replace />

  return (
    <main className="page landing">
      <header className="row between">
        <span className="brand">
          <BeanMark size={24} /> Beanote
        </span>
        <Link to="/login" className="btn btn-ghost btn-sm">
          로그인
        </Link>
      </header>

      <section className="landing-hero">
        <CoffeeCupArt size={190} />
        <p className="eyebrow">원두 취향 노트</p>
        <h1 className="landing-title">
          마신 커피가
          <br />
          나의 취향이 되는 곳
        </h1>
        <p className="muted">
          오늘 마신 커피를 기록하면, 내가 좋아하는 맛을 알려 주고
          <br />
          다음에 마실 원두를 이유와 함께 골라 드려요.
        </p>
        <div className="stack" style={{ width: '100%', marginTop: 8 }}>
          <Link to="/signup" className="btn btn-primary btn-block">
            시작하기
          </Link>
          <Link to="/login" className="btn btn-ghost btn-block">
            이미 계정이 있어요
          </Link>
        </div>
      </section>

      <section className="section stack">
        {FEATURES.map((f) => (
          <div key={f.title} className="feature card flat">
            <span className="option-icon">
              <Icon name={f.icon} />
            </span>
            <div>
              <h3>{f.title}</h3>
              <p className="small muted">{f.text}</p>
            </div>
          </div>
        ))}
      </section>

      <section className="section">
        <div className="card example-card">
          <p className="eyebrow">예시 · 노트 12개를 쓴 민수 님</p>
          <h2 style={{ marginTop: 4 }}>“밝은 산미와 꽃향을 좋아하는 라이트 로스팅파”</h2>
          <TasteRadar value={{ acidity: 4.4, sweetness: 3.6, body: 2.3, bitterness: 1.8, aftertaste: 3.4 }} height={220} />
          <div className="chips">
            <FlavorChip name="자스민" category="FLORAL" />
            <FlavorChip name="블루베리" category="BERRY" />
            <FlavorChip name="복숭아" category="STONE_TROPICAL" />
            <FlavorChip name="꿀" category="SWEET" />
          </div>
        </div>
      </section>

      <p className="tiny muted center section">
        시드 원두 정보는 예시 데이터로, 실제 판매 정보와 다를 수 있어요.
      </p>
    </main>
  )
}
