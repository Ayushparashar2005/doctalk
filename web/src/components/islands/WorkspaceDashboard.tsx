import { useEffect, useState } from 'react';
import { getWorkspaceDocuments, clearWorkspaceDocuments, type StoredDocument } from '@/lib/workspace';

function statusClass(status: StoredDocument['status']) {
  return status === 'processed'
    ? 'badge badge--processed'
    : status === 'processing'
      ? 'badge badge--processing'
      : status === 'failed'
        ? 'badge badge--failed'
        : 'badge badge--queued';
}

function formatUpdatedAt(isoDate: string) {
  return new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric', year: 'numeric' }).format(
    new Date(isoDate)
  );
}

export default function WorkspaceDashboard() {
  const [documents, setDocuments] = useState<StoredDocument[]>([]);

  useEffect(() => {
    setDocuments(getWorkspaceDocuments());

    const handleStorage = () => setDocuments(getWorkspaceDocuments());
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  const totalDocuments = documents.length;
  const processedDocuments = documents.filter((document) => document.status === 'processed').length;
  const processingDocuments = documents.filter((document) => document.status === 'processing').length;
  const failedDocuments = documents.filter((document) => document.status === 'failed').length;

  return (
    <section className="stack">
      <section className="panel">
        <div className="panel__inner app-toolbar">
          <div>
            <p className="kicker">Workspace</p>
            <h1 className="h2">Your document command center</h1>
            <p className="muted" style={{ maxWidth: '68ch', lineHeight: 1.7, marginBottom: 0 }}>
              Everything here runs inside the `web` project. Uploaded documents live in your browser,
              and the app uses Groq through server routes instead of a separate backend folder.
            </p>
          </div>
          <div className="app-toolbar__group">
            <a className="button button--primary" href="/app/upload">
              Upload document
            </a>
            <a className="button button--secondary" href="/app/settings">
              Settings
            </a>
          </div>
        </div>
      </section>

      <section className="grid grid--4">
        <div className="metric">
          <span className="metric__label">Documents</span>
          <span className="metric__value">{totalDocuments}</span>
          <span className="metric__hint">Seeded and uploaded documents.</span>
        </div>
        <div className="metric">
          <span className="metric__label">Processed</span>
          <span className="metric__value">{processedDocuments}</span>
          <span className="metric__hint">Ready for chat.</span>
        </div>
        <div className="metric">
          <span className="metric__label">Processing</span>
          <span className="metric__value">{processingDocuments}</span>
          <span className="metric__hint">Queued while files are analyzed.</span>
        </div>
        <div className="metric">
          <span className="metric__label">Failed</span>
          <span className="metric__value">{failedDocuments}</span>
          <span className="metric__hint">Visible for troubleshooting.</span>
        </div>
      </section>

      <section className="grid grid--2">
        <article className="card">
          <div className="card__inner stack">
            <p className="kicker">AI mode</p>
            <h3 className="h3">The app is self-contained, with Groq handling the answers</h3>
            <div className="list">
              <div className="metric">
                <span className="metric__label">Upload</span>
                <span className="metric__hint">PDF and TXT files are parsed in the browser.</span>
              </div>
              <div className="metric">
                <span className="metric__label">RAG</span>
                <span className="metric__hint">Answers are generated from document chunks and matching snippets through Groq.</span>
              </div>
              <div className="metric">
                <span className="metric__label">Storage</span>
                <span className="metric__hint">Documents and chat history live in localStorage.</span>
              </div>
            </div>
            <div className="app-toolbar">
              <button className="button button--ghost" type="button" onClick={() => {
                clearWorkspaceDocuments();
                setDocuments(getWorkspaceDocuments());
              }}>
                Clear uploads
              </button>
            </div>
          </div>
        </article>

        <article className="card">
          <div className="card__inner stack">
            <p className="kicker">Product shape</p>
            <h3 className="h3">Three steps</h3>
            <div className="list">
              <div className="metric">
                <span className="metric__label">1. Upload</span>
                <span className="metric__hint">Send in a document from the upload page.</span>
              </div>
              <div className="metric">
                <span className="metric__label">2. Index</span>
                <span className="metric__hint">The browser extracts text, builds chunks, and saves the document.
                </span>
              </div>
              <div className="metric">
                <span className="metric__label">3. Ask</span>
                <span className="metric__hint">Open the document page and ask questions from that document.
                </span>
              </div>
            </div>
          </div>
        </article>
      </section>

      <section className="section">
        <div className="app-toolbar" style={{ marginBottom: 16 }}>
          <div>
            <p className="kicker">Recent documents</p>
            <h2 className="h2">Everything in the workspace</h2>
          </div>
          <a className="button button--ghost" href={`/app/documents/${documents[0]?.id ?? 'product-roadmap-2026'}`}>
            Open a document
          </a>
        </div>

        <div className="grid grid--2">
          {documents.map((document) => (
            <article className="card" key={document.id}>
              <div className="card__inner stack">
                <div className="card__header">
                  <div className="stack">
                    <p className="kicker">{document.fileType}</p>
                    <h3 className="h3">{document.title}</h3>
                  </div>
                  <span className={statusClass(document.status)}>{document.status}</span>
                </div>
                <p className="muted" style={{ lineHeight: 1.7, margin: 0 }}>
                  {document.summary}
                </p>
                <div className="pill-row">
                  {document.tags.map((tag) => (
                    <span className="pill" key={tag}>
                      {tag}
                    </span>
                  ))}
                </div>
                <div className="app-toolbar">
                  <div className="muted">Updated {formatUpdatedAt(document.updatedAt)}</div>
                  <a className="button button--secondary" href={`/app/documents/${document.id}`}>
                    Open document
                  </a>
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}
