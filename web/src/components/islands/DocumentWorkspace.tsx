import { useEffect, useMemo, useState } from 'react';
import DocumentReader from './DocumentReader';
import DocumentChat from './DocumentChat';
import { getWorkspaceDocumentById, getWorkspaceDocuments, type StoredDocument } from '@/lib/workspace';

function statusClass(status: StoredDocument['status']) {
  return status === 'processed'
    ? 'badge badge--processed'
    : status === 'processing'
      ? 'badge badge--processing'
      : status === 'failed'
        ? 'badge badge--failed'
        : 'badge badge--queued';
}

type Props = {
  documentId: string;
};

export default function DocumentWorkspace({ documentId }: Props) {
  const [document, setDocument] = useState<StoredDocument | null>(null);

  const workspaceDocuments = useMemo(
    () => getWorkspaceDocuments().filter((entry) => entry.id !== documentId).slice(0, 3),
    [documentId, document]
  );

  const highlights = useMemo(
    () =>
      document ? (document.highlights.length > 0 ? document.highlights : document.readerSections.slice(0, 3)) : [],
    [document]
  );

  useEffect(() => {
    const loadDocument = () => setDocument(getWorkspaceDocumentById(documentId) ?? null);
    loadDocument();
    window.addEventListener('storage', loadDocument);
    return () => window.removeEventListener('storage', loadDocument);
  }, [documentId]);

  if (!document) {
    return (
      <section className="document-shell">
        <header className="document-topbar panel panel--solid">
          <a className="document-brand" href="/" aria-label="DocTalk home">
            <div className="workspace-mark">D</div>
            <div>
              <p className="document-brand__eyebrow">DocTalk</p>
              <h1 className="document-brand__title">Document workspace</h1>
            </div>
          </a>
        </header>
        <div className="workspace-loading panel panel--solid">
          <div className="panel__inner stack">
            <p className="kicker">Loading</p>
            <h2 className="h2">Opening document</h2>
            <p className="muted" style={{ lineHeight: 1.8, margin: 0 }}>
              If this is a document you just uploaded, the page will render once the browser store is ready.
            </p>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="document-shell">
      <header className="document-topbar panel panel--solid">
        <a className="document-brand" href="/" aria-label="DocTalk home">
          <div className="workspace-mark">D</div>
          <div>
            <p className="document-brand__eyebrow">DocTalk</p>
            <h1 className="document-brand__title">Workspace</h1>
          </div>
        </a>
      </header>

      <section className="document-room" id="workspace">
        <aside className="document-sidebar panel panel--solid">
          <div className="panel__inner document-sidebar__inner">
            <div className="document-sidebar__hero">
              <div>
                <p className="kicker">Document intake</p>
                <h2 className="workspace-title">{document.title}</h2>
                <p className="workspace-copy workspace-copy--tight">{document.summary}</p>
              </div>
              <span className={statusClass(document.status)}>{document.status}</span>
            </div>

            <a className="workspace-action workspace-action--primary document-sidebar__button" href="/app/upload">
              Upload Document
            </a>

            <section className="document-nav" aria-label="Workspace sections">
              <a className="document-nav__item document-nav__item--active" href="#workspace">
                Library
              </a>
              <a className="document-nav__item" href="#assistant">
                Recent
              </a>
              <a className="document-nav__item" href="#assistant">
                Starred
              </a>
              <a className="document-nav__item" href="#assistant">
                Settings
              </a>
            </section>

            <section className="document-sidebar__sources" id="sources">
              <p className="kicker">Active Sources</p>
              <div className="document-source-list">
                {[document, ...workspaceDocuments].slice(0, 3).map((entry, index) => (
                  <article className="document-source-card" key={`${entry.id}-${index}`}>
                    <div>
                      <strong>{entry.fileName}</strong>
                      <div className="muted">
                        {entry.pages} pages · {entry.status}
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </div>
        </aside>

        <section className="document-main panel panel--solid">
          <DocumentReader document={document} />
        </section>

        <aside className="document-assistant panel panel--solid" id="assistant">
          <DocumentChat
            documentId={document.id}
            documentTitle={document.title}
            documentText={document.sourceText}
            documentFileName={document.fileName}
          />
        </aside>
      </section>
    </section>
  );
}
