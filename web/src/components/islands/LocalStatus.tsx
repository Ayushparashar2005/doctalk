export default function LocalStatus() {
  return (
    <div className="card">
      <div className="card__inner stack">
        <div className="card__header">
          <div>
            <p className="kicker">Self-contained</p>
            <h3 className="h3">No separate app backend required</h3>
          </div>
          <span className="badge badge--processed">Groq</span>
        </div>
        <p className="muted" style={{ lineHeight: 1.7, margin: 0 }}>
          Documents are uploaded in the browser, stored in localStorage, and asked against with the
          in-project RAG flow while Groq handles the AI responses through server routes.
        </p>
      </div>
    </div>
  );
}
