import { useMemo, useState } from 'react';
import { analyzeDocument } from '@/lib/rag';
import { saveWorkspaceDocument, type StoredDocument } from '@/lib/workspace';

type UploadJob = {
  name: string;
  size: string;
  status: 'queued' | 'processing' | 'processed' | 'failed';
  progress: number;
  documentId?: string;
};

type AnalysisResponse = {
  summary?: string;
  highlights?: string[];
  tags?: string[];
  modelUsed?: string;
  fallbackUsed?: boolean;
};

const supportedExtensions = ['pdf', 'txt', 'docx'];

function formatBytes(size: number) {
  if (size === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1);
  const value = size / Math.pow(1024, index);
  return `${value.toFixed(value >= 10 || index === 0 ? 0 : 1)} ${units[index]}`;
}

async function extractDocumentFromFile(file: File) {
  const extension = file.name.split('.').pop()?.toLowerCase();

  if (extension === 'txt') {
    const text = await file.text();
    return {
      sourceText: text,
      readerSections: text
        .split(/\n{2,}/)
        .map((paragraph) => paragraph.replace(/\s+/g, ' ').trim())
        .filter(Boolean)
    };
  }

  if (extension === 'docx') {
    const mammothModule = await import('mammoth/mammoth.browser');
    const mammoth = (mammothModule.default ?? mammothModule) as {
      extractRawText: (options: { arrayBuffer: ArrayBuffer }) => Promise<{ value: string }>;
    };
    const arrayBuffer = await file.arrayBuffer();
    const extraction = await mammoth.extractRawText({ arrayBuffer });
    const text = extraction.value;
    return {
      sourceText: text,
      readerSections: text
        .split(/\n{2,}/)
        .map((paragraph) => paragraph.replace(/\s+/g, ' ').trim())
        .filter(Boolean)
    };
  }

  const pdfjs = await import('pdfjs-dist');
  const workerModule = await import('pdfjs-dist/build/pdf.worker.min.mjs?url');
  const workerUrl = String(workerModule.default ?? workerModule);
  pdfjs.GlobalWorkerOptions.workerSrc = workerUrl;

  const arrayBuffer = await file.arrayBuffer();
  const loadingTask = pdfjs.getDocument({ data: new Uint8Array(arrayBuffer) });
  const pdf = await loadingTask.promise;
  const pageTexts: string[] = [];

  for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber += 1) {
    const page = await pdf.getPage(pageNumber);
    const content = await page.getTextContent();
    const text = content.items
      .map((item) => (typeof item === 'object' && item && 'str' in item ? String((item as { str: string }).str) : ''))
      .join(' ');
    pageTexts.push(text);
  }

  const sourceText = pageTexts.join('\n\n');
  return { sourceText, readerSections: pageTexts };
}

export default function UploadPanel() {
  const [file, setFile] = useState<File | null>(null);
  const [jobs, setJobs] = useState<UploadJob[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isRunning, setIsRunning] = useState(false);

  const canUpload = useMemo(() => file !== null && !isRunning, [file, isRunning]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (!file) {
      setError('Choose a PDF, TXT, or DOCX file first.');
      return;
    }

    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!extension || !supportedExtensions.includes(extension)) {
      setError('Only PDF, TXT, and DOCX files are supported.');
      return;
    }

    const job: UploadJob = {
      name: file.name,
      size: formatBytes(file.size),
      status: 'processing',
      progress: 8
    };

    setJobs((current) => [job, ...current]);
    setIsRunning(true);

    const timer = window.setInterval(() => {
      setJobs((current) =>
        current.map((item) => {
          if (item.name !== file.name) {
            return item;
          }

          const nextProgress = Math.min(item.progress + 18, 100);
          return {
            ...item,
            progress: nextProgress,
            status: nextProgress >= 100 ? 'processed' : 'processing'
          };
        })
      );
    }, 220);

    try {
      const extractedDocument = await extractDocumentFromFile(file);
      const extractedText = extractedDocument.sourceText.trim();

      if (!extractedText) {
        throw new Error('No readable text was found in this file.');
      }

      const localAnalysis = analyzeDocument(extractedText);
      let analysis = localAnalysis;

      try {
        const response = await fetch('/api/ai/analyze', {
          method: 'POST',
          headers: {
            'content-type': 'application/json'
          },
          body: JSON.stringify({
            title: file.name.replace(/\.[^.]+$/, ''),
            fileName: file.name,
            sourceText: extractedText
          })
        });

        if (response.ok) {
          const aiAnalysis = (await response.json()) as AnalysisResponse;
          analysis = {
            ...localAnalysis,
            summary: aiAnalysis.summary?.trim() || localAnalysis.summary,
            highlights:
              Array.isArray(aiAnalysis.highlights) && aiAnalysis.highlights.length > 0
                ? aiAnalysis.highlights.map((item) => item.trim()).filter(Boolean)
                : localAnalysis.highlights,
            tags:
              Array.isArray(aiAnalysis.tags) && aiAnalysis.tags.length > 0
                ? aiAnalysis.tags.map((item) => item.trim()).filter(Boolean)
                : localAnalysis.tags
          };
        }
      } catch {
        analysis = localAnalysis;
      }

      const documentId = crypto.randomUUID();
      const now = new Date().toISOString();
      const document: StoredDocument = {
        id: documentId,
        title: file.name.replace(/\.[^.]+$/, ''),
        fileName: file.name,
        fileType: file.name.toLowerCase().endsWith('.pdf')
          ? 'PDF'
          : file.name.toLowerCase().endsWith('.docx')
            ? 'DOCX'
            : 'TXT',
        status: 'processed',
        pages: analysis.pageCount,
        words: analysis.wordCount,
        updatedAt: now,
        owner: 'You',
        summary: analysis.summary,
        highlights: analysis.highlights,
        tags: analysis.tags,
        sourceText: analysis.sourceText,
        readerSections: extractedDocument.readerSections.length > 0 ? extractedDocument.readerSections : analysis.readerSections,
        chunkCount: analysis.chunkCount,
        source: 'local'
      };

      saveWorkspaceDocument(document);

      window.clearInterval(timer);
      setJobs((current) =>
        current.map((item) =>
          item.name === file.name
            ? { ...item, progress: 100, status: 'processed', documentId }
            : item
        )
      );
      setIsRunning(false);

      window.setTimeout(() => {
        window.location.assign(`/app/documents/${documentId}`);
      }, 450);
    } catch (submitError) {
      window.clearInterval(timer);
      setJobs((current) =>
        current.map((item) =>
          item.name === file.name ? { ...item, progress: 100, status: 'failed' } : item
        )
      );
      setError(submitError instanceof Error ? submitError.message : 'Upload failed.');
      setIsRunning(false);
    }
  }

  return (
    <section className="card">
      <div className="card__inner upload">
        <div className="card__header">
          <div>
            <p className="kicker">Document intake</p>
            <h3 className="h3">Upload and analyze</h3>
          </div>
          <span className="badge badge--processing">Groq</span>
        </div>

        <div className="upload__dropzone">
          <strong>Drop a PDF, TXT, or DOCX file here</strong>
          <div className="muted">This island parses the file in the browser, then sends the extracted text to Groq for document analysis before storing it in your browser.</div>
          <form onSubmit={handleSubmit} className="upload__progress" style={{ width: '100%' }}>
            <input
              type="file"
              accept=".pdf,.txt,.docx"
              onChange={(event) => {
                setError(null);
                setFile(event.target.files?.[0] ?? null);
              }}
            />
            <button className="button button--primary" disabled={!canUpload} type="submit">
              Analyze document
            </button>
          </form>
          {error ? <div className="badge badge--failed" style={{ textTransform: 'none' }}>{error}</div> : null}
        </div>

        <div className="list">
          {jobs.length === 0 ? (
            <div className="metric">
              <span className="metric__label">No uploads yet</span>
              <span className="metric__hint">Select a file to see the staged processing feedback.</span>
            </div>
          ) : (
            jobs.map((job) => (
              <div key={job.name} className="metric">
                <div className="app-toolbar">
                  <div>
                    <strong>{job.name}</strong>
                    <div className="muted">{job.size}</div>
                  </div>
                  <span className={`badge badge--${job.status}`}>{job.status}</span>
                </div>
                <div className="upload__bar">
                  <span style={{ width: `${job.progress}%` }} />
                </div>
                <div className="muted">{job.progress}% complete</div>
                {job.documentId ? (
                  <a className="button button--secondary" href={`/app/documents/${job.documentId}`}>
                    Open document
                  </a>
                ) : null}
              </div>
            ))
          )}
        </div>
      </div>
    </section>
  );
}
