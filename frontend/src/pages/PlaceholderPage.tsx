type Props = { title: string; phase?: string }

export default function PlaceholderPage({ title, phase }: Props) {
  return (
    <section className="page">
      <h1 className="page-title">{title}</h1>
      {phase && <p className="muted">{phase}에서 만들 화면이에요.</p>}
    </section>
  )
}
