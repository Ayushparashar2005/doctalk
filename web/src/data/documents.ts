export type DocumentStatus = 'processed' | 'processing' | 'failed' | 'queued';

export type DocumentRecord = {
  id: string;
  title: string;
  fileName: string;
  fileType: 'PDF' | 'TXT' | 'DOCX';
  status: DocumentStatus;
  pages: number;
  words: number;
  updatedAt: string;
  owner: string;
  summary: string;
  highlights: string[];
  tags: string[];
  sourceText?: string;
};

export type ChatSeed = {
  role: 'user' | 'assistant';
  content: string;
};

export const documents: DocumentRecord[] = [
  {
    id: 'board-brief-q2',
    title: 'Board Brief Q2',
    fileName: 'board-brief-q2.pdf',
    fileType: 'PDF',
    status: 'processed',
    pages: 18,
    words: 6240,
    updatedAt: '2026-04-18T16:20:00Z',
    owner: 'Ayesha Khan',
    summary:
      'Quarterly board brief with finance, operations, and delivery metrics. The strongest themes are margin improvement, faster onboarding, and a tighter product scope for the next release.',
    sourceText:
      'Quarterly board brief with finance, operations, and delivery metrics. Revenue grew 18 percent quarter over quarter. Churn dropped after the onboarding rewrite. The launch sequence for Q3 was narrowed to three priorities. The strongest themes are margin improvement, faster onboarding, and a tighter product scope for the next release.',
    highlights: [
      'Revenue grew 18 percent quarter over quarter.',
      'Churn dropped after the onboarding rewrite.',
      'The launch sequence for Q3 was narrowed to three priorities.'
    ],
    tags: ['finance', 'strategy', 'q2']
  },
  {
    id: 'patient-intake-playbook',
    title: 'Patient Intake Playbook',
    fileName: 'patient-intake-playbook.pdf',
    fileType: 'PDF',
    status: 'processing',
    pages: 42,
    words: 11820,
    updatedAt: '2026-04-21T08:44:00Z',
    owner: 'Dr. Samir Patel',
    summary:
      'Operational playbook for patient intake, triage, and document handling. The current processing step is extracting forms and standardizing field names for downstream review.',
    sourceText:
      'Operational playbook for patient intake, triage, and document handling. Uses a three-step triage intake flow. Contains a checklist for insurance and consent forms. Requires additional OCR validation before chat is enabled. The current processing step is extracting forms and standardizing field names for downstream review.',
    highlights: [
      'Uses a three-step triage intake flow.',
      'Contains a checklist for insurance and consent forms.',
      'Requires additional OCR validation before chat is enabled.'
    ],
    tags: ['healthcare', 'operations', 'forms']
  },
  {
    id: 'product-roadmap-2026',
    title: 'Product Roadmap 2026',
    fileName: 'product-roadmap-2026.pdf',
    fileType: 'PDF',
    status: 'processed',
    pages: 24,
    words: 8450,
    updatedAt: '2026-04-20T12:10:00Z',
    owner: 'Mina Ortiz',
    summary:
      'A roadmap document centered on document intelligence, faster processing, and better operational visibility. The next milestone is a unified workspace for uploads, summaries, and chats.',
    sourceText:
      'A roadmap document centered on document intelligence, faster processing, and better operational visibility. Dynamic document pages will be SSR-backed. Cloudflare Pages and Workers are the target deployment path. The chat experience will support streamed answers in later phases. The next milestone is a unified workspace for uploads, summaries, and chats.',
    highlights: [
      'Dynamic document pages will be SSR-backed.',
      'Cloudflare Pages and Workers are the target deployment path.',
      'The chat experience will support streamed answers in later phases.'
    ],
    tags: ['product', 'roadmap', 'frontend']
  },
  {
    id: '3ea22c62-5e93-45e5-8e7c-c5a53390bb8a',
    title: 'Workspace Overview',
    fileName: 'notebooklm-style-brief.pdf',
    fileType: 'PDF',
    status: 'processed',
    pages: 28,
    words: 9720,
    updatedAt: '2026-04-21T10:30:00Z',
    owner: 'DocTalk Team',
    summary:
      'An overview of the redesigned document workspace. It explains the three-pane layout, how the reader should feel, and how Groq-backed answers are grounded in the extracted document text.',
    sourceText:
      'An overview of the redesigned document workspace. The left column shows stats, summary, highlights, and metadata. The middle column is the conversational window where questions are asked against the document. The right column is the document reader with PDF or DOCX sections rendered in a structured layout. The workspace is self-contained and the answers come from Groq-backed retrieval over the extracted document text. The goal is to create a focused reading and chat experience.',
    highlights: [
      'The left column keeps the document facts visible at a glance.',
      'The middle column behaves like a dedicated AI chat workspace.',
      'The right column provides a reader for PDF or DOCX content.'
    ],
    tags: ['layout', 'rag', 'workspace']
  },
  {
    id: 'incident-response-notes',
    title: 'Incident Response Notes',
    fileName: 'incident-response-notes.txt',
    fileType: 'TXT',
    status: 'failed',
    pages: 6,
    words: 1840,
    updatedAt: '2026-04-17T21:05:00Z',
    owner: 'Infra Team',
    summary:
      'A short incident log that failed validation because the source text was incomplete. The failure state is useful in the UI because it mirrors the Android app behavior for broken processing jobs.',
    sourceText:
      'A short incident log that failed validation because the source text was incomplete. Upload status should surface the error clearly. Failed docs still keep their metadata for troubleshooting. The retry path should be explicit instead of hidden.',
    highlights: [
      'Upload status should surface the error clearly.',
      'Failed docs still keep their metadata for troubleshooting.',
      'The retry path should be explicit instead of hidden.'
    ],
    tags: ['ops', 'incident', 'txt']
  }
];

export const chatSeeds: Record<string, ChatSeed[]> = {
  'board-brief-q2': [
    { role: 'user', content: 'What are the biggest takeaways from this brief?' },
    {
      role: 'assistant',
      content:
        'The biggest takeaways are revenue growth, better retention after onboarding changes, and a narrower launch plan for the next quarter.'
    }
  ],
  'patient-intake-playbook': [
    { role: 'user', content: 'Where does the workflow still slow down?' },
    {
      role: 'assistant',
      content:
        'The slowest step is OCR validation and form normalization. That is why the document remains in processing until the data is cleaned.'
    }
  ],
  'product-roadmap-2026': [
    { role: 'user', content: 'What will this app be built with?' },
    {
      role: 'assistant',
      content:
        'The web rebuild uses Astro with Cloudflare deployment, static first pages, and SSR-backed document islands for dynamic interactions.'
    }
  ],
  '3ea22c62-5e93-45e5-8e7c-c5a53390bb8a': [
    { role: 'user', content: 'What should the new layout emphasize?' },
    {
      role: 'assistant',
      content:
        'The page should emphasize the document facts on the left, the AI conversation in the center, and the reader on the right, with NotebookLM-style focus.'
    }
  ],
  'incident-response-notes': [
    { role: 'user', content: 'Why did processing fail?' },
    {
      role: 'assistant',
      content:
        'The source text was incomplete, so the document processor could not extract a reliable corpus for indexing.'
    }
  ]
};

export function getDocumentById(id: string) {
  return documents.find((document) => document.id === id);
}

export function getDocumentStats() {
  const totalDocuments = documents.length;
  const processedDocuments = documents.filter((document) => document.status === 'processed').length;
  const processingDocuments = documents.filter((document) => document.status === 'processing').length;
  const failedDocuments = documents.filter((document) => document.status === 'failed').length;
  const totalPages = documents.reduce((sum, document) => sum + document.pages, 0);
  const totalWords = documents.reduce((sum, document) => sum + document.words, 0);

  return {
    totalDocuments,
    processedDocuments,
    processingDocuments,
    failedDocuments,
    totalPages,
    totalWords
  };
}

export function formatDocumentDate(isoDate: string) {
  return new Intl.DateTimeFormat('en', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  }).format(new Date(isoDate));
}
