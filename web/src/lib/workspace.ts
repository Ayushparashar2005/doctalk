import { documents as seededDocuments, type DocumentRecord } from '@/data/documents';
import { analyzeDocument } from '@/lib/rag';

export type StoredDocument = DocumentRecord & {
  sourceText: string;
  readerSections: string[];
  chunkCount: number;
  source: 'seed' | 'local';
};

export type ChatMessage = {
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
};

const DOCUMENT_STORAGE_KEY = 'doctalk:web:documents:v1';
const CHAT_STORAGE_PREFIX = 'doctalk:web:chat:v1:';

function isBrowser() {
  return typeof window !== 'undefined';
}

function readJson<T>(key: string, fallback: T): T {
  if (!isBrowser()) {
    return fallback;
  }

  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) {
      return fallback;
    }
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

function writeJson<T>(key: string, value: T) {
  if (!isBrowser()) {
    return;
  }

  window.localStorage.setItem(key, JSON.stringify(value));
}

export function getSeedDocuments(): StoredDocument[] {
  return seededDocuments.map((document) => {
    const sourceText = document.sourceText ?? `${document.title}. ${document.summary}`;
    const analysis = analyzeDocument(sourceText);

    return {
      ...document,
      sourceText,
      readerSections: analysis.readerSections,
      chunkCount: analysis.chunkCount,
      source: 'seed'
    };
  });
}

export function getLocalDocuments(): StoredDocument[] {
  return readJson<StoredDocument[]>(DOCUMENT_STORAGE_KEY, []).map((document) => {
    const sourceText = document.sourceText ?? `${document.title}. ${document.summary}`;
    const analysis = analyzeDocument(sourceText);

    return {
      ...document,
      sourceText,
      readerSections: document.readerSections?.length ? document.readerSections : analysis.readerSections,
      chunkCount: document.chunkCount ?? analysis.chunkCount,
      source: 'local'
    };
  });
}

export function getWorkspaceDocuments(): StoredDocument[] {
  const localDocuments = getLocalDocuments();
  const documentMap = new Map<string, StoredDocument>();

  [...getSeedDocuments(), ...localDocuments].forEach((document) => {
    documentMap.set(document.id, document);
  });

  return [...documentMap.values()].sort((left, right) => {
    return new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime();
  });
}

export function getWorkspaceDocumentById(id: string) {
  return getWorkspaceDocuments().find((document) => document.id === id);
}

export function saveWorkspaceDocument(document: StoredDocument) {
  const localDocuments = getLocalDocuments().filter((entry) => entry.id !== document.id);
  writeJson(DOCUMENT_STORAGE_KEY, [...localDocuments, document]);
}

export function deleteWorkspaceDocument(id: string) {
  const localDocuments = getLocalDocuments().filter((entry) => entry.id !== id);
  writeJson(DOCUMENT_STORAGE_KEY, localDocuments);
  if (isBrowser()) {
    window.localStorage.removeItem(`${CHAT_STORAGE_PREFIX}${id}`);
  }
}

export function clearWorkspaceDocuments() {
  if (!isBrowser()) {
    return;
  }

  window.localStorage.removeItem(DOCUMENT_STORAGE_KEY);
  seededDocuments.forEach((document) => {
    window.localStorage.removeItem(`${CHAT_STORAGE_PREFIX}${document.id}`);
  });
}

export function getWorkspaceChat(documentId: string) {
  return readJson<ChatMessage[]>(`${CHAT_STORAGE_PREFIX}${documentId}`, []);
}

export function saveWorkspaceChat(documentId: string, messages: ChatMessage[]) {
  writeJson(`${CHAT_STORAGE_PREFIX}${documentId}`, messages);
}
