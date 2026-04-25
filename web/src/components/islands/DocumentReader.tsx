import type { StoredDocument } from '@/lib/workspace';

type Props = {
  document: StoredDocument;
  searchQuery?: string;
};

function sectionLabel(document: StoredDocument) {
  return document.fileType === 'PDF' ? 'Page' : 'Section';
}

export default function DocumentReader({ document, searchQuery = '' }: Props) {
  const sections = document.readerSections.length > 0 ? document.readerSections : [document.sourceText];
  const label = sectionLabel(document);
  const normalizedQuery = searchQuery.trim().toLowerCase();
  const filteredSections =
    normalizedQuery.length > 0
      ? sections.filter((section) => section.toLowerCase().includes(normalizedQuery))
      : sections;

  return (
    <section className="document-canvas">
      <div className="document-canvas__header">
        <div>
          <div className="document-canvas__kicker-row">
            <span className="document-chip">Document</span>
            <span className="document-canvas__meta">Modified {new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(document.updatedAt))}</span>
          </div>
          <h2 className="document-canvas__title">{document.title}</h2>
          <div className="document-canvas__contributors">
            <span className="document-avatar">{document.owner.slice(0, 1)}</span>
            <span className="document-canvas__contributors-text">Owner: {document.owner}</span>
          </div>
        </div>
      </div>

      <p className="document-canvas__intro">{document.summary}</p>

      <div className="document-insight-grid">
        {document.tags.slice(0, 2).map((tag, index) => (
          <article className="document-insight-card" key={tag}>
            <div className="document-insight-card__icon">{index === 0 ? '↗' : '◌'}</div>
            <h3>{tag === 'layout' ? 'Workspace Layout' : tag === 'rag' ? 'Grounded Retrieval' : tag}</h3>
            <p>{document.highlights[index] ?? document.summary}</p>
          </article>
        ))}
      </div>

      <article className="document-article">
        <h3>Analysis of Core Risks</h3>
        <p>
          {filteredSections[0] ?? document.summary}
        </p>

        <div className="document-figure">
          <div className="document-figure__overlay" />
          <div className="document-figure__caption">
            <strong>Section preview</strong>
            <span>Document section view</span>
          </div>
        </div>

        <p>
          {filteredSections[1] ?? document.highlights[0] ?? document.sourceText}
        </p>
      </article>

      <div className="document-pages">
        {filteredSections.length === 0 ? (
          <div className="workspace-empty metric metric--workspace">
            <span className="metric__label">No matches</span>
            <span className="metric__hint">Try a different search term or clear the search field.</span>
          </div>
        ) : (
          filteredSections.map((section, index) => (
            <article className="reader-page" key={`${index}-${section.slice(0, 40)}`}>
              <div className="reader-page__header">
                <span className="reader-page__label">
                  {label} {index + 1}
                </span>
                <span className="reader-page__meta">{Math.max(1, Math.ceil(section.length / 1200))} min read</span>
              </div>
              <div className="reader-page__body">
                {section
                  .split(/\n+/)
                  .map((paragraph) => paragraph.replace(/\s+/g, ' ').trim())
                  .filter(Boolean)
                  .map((paragraph) => (
                    <p key={paragraph}>{paragraph}</p>
                  ))}
              </div>
            </article>
          ))
        )}
      </div>
    </section>
  );
}
