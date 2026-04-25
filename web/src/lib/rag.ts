const STOP_WORDS = new Set([
  'a',
  'an',
  'and',
  'are',
  'as',
  'at',
  'be',
  'but',
  'by',
  'for',
  'from',
  'has',
  'have',
  'he',
  'her',
  'his',
  'i',
  'in',
  'into',
  'is',
  'it',
  'its',
  'me',
  'my',
  'of',
  'on',
  'or',
  'our',
  'she',
  'that',
  'the',
  'their',
  'them',
  'they',
  'this',
  'to',
  'was',
  'we',
  'were',
  'with',
  'you',
  'your'
]);

export type RagAnalysis = {
  summary: string;
  highlights: string[];
  tags: string[];
  wordCount: number;
  chunkCount: number;
  pageCount: number;
  sourceText: string;
  readerSections: string[];
};

export type RagAnswer = {
  answer: string;
  context: string[];
  sources: string[];
  modelUsed: string;
  responseTime: number;
  tokensUsed: number;
  confidence: number;
};

export function normalizeText(text: string) {
  return text.replace(/\s+/g, ' ').trim();
}

export function countWords(text: string) {
  return normalizeText(text)
    .split(' ')
    .filter(Boolean).length;
}

export function estimatePageCount(text: string) {
  return Math.max(1, Math.ceil(countWords(text) / 450));
}

export function splitIntoSentences(text: string) {
  return normalizeText(text)
    .split(/(?<=[.!?])\s+/)
    .map((sentence) => sentence.trim())
    .filter((sentence) => sentence.length > 0);
}

export function splitIntoParagraphs(text: string) {
  return text
    .split(/\n{2,}/)
    .map((paragraph) => paragraph.replace(/\s+/g, ' ').trim())
    .filter((paragraph) => paragraph.length > 0);
}

export function buildReaderSections(text: string, maxSections = 12) {
  const trimmedText = text.trim();
  if (!trimmedText) {
    return [];
  }

  const paragraphs = splitIntoParagraphs(text);
  if (paragraphs.length > 1) {
    return paragraphs.slice(0, maxSections);
  }

  const sentences = splitIntoSentences(text);
  if (sentences.length > 1) {
    const groupedSections: string[] = [];
    for (let index = 0; index < sentences.length && groupedSections.length < maxSections; index += 3) {
      groupedSections.push(sentences.slice(index, index + 3).join(' '));
    }
    return groupedSections.filter(Boolean);
  }

  return chunkText(text, 900, 0).slice(0, maxSections);
}

export function chunkText(text: string, chunkSize = 700, overlap = 120) {
  const normalized = normalizeText(text);
  if (!normalized) {
    return [];
  }

  const chunks: string[] = [];
  let cursor = 0;

  while (cursor < normalized.length) {
    const end = Math.min(cursor + chunkSize, normalized.length);
    chunks.push(normalized.slice(cursor, end).trim());
    if (end === normalized.length) {
      break;
    }
    cursor = Math.max(end - overlap, cursor + 1);
  }

  return chunks.filter(Boolean);
}

export function tokenize(text: string) {
  return normalizeText(text)
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .filter((token) => token.length > 2 && !STOP_WORDS.has(token));
}

function frequencyMap(tokens: string[]) {
  return tokens.reduce<Map<string, number>>((map, token) => {
    map.set(token, (map.get(token) ?? 0) + 1);
    return map;
  }, new Map());
}

export function extractKeywords(text: string, limit = 5) {
  const tokens = tokenize(text);
  const counts = frequencyMap(tokens);

  return [...counts.entries()]
    .sort((left, right) => right[1] - left[1])
    .slice(0, limit)
    .map(([token]) => token);
}

export function summarizeText(text: string, limit = 3) {
  const sentences = splitIntoSentences(text);
  if (sentences.length === 0) {
    return normalizeText(text).slice(0, 240);
  }

  const tokens = tokenize(text);
  const counts = frequencyMap(tokens);

  const ranked = sentences
    .map((sentence) => {
      const score = tokenize(sentence).reduce((total, token) => total + (counts.get(token) ?? 0), 0);
      return { sentence, score };
    })
    .sort((left, right) => right.score - left.score);

  return ranked
    .slice(0, limit)
    .map(({ sentence }) => sentence)
    .join(' ');
}

export function extractHighlights(text: string, limit = 3) {
  const sentences = splitIntoSentences(text);
  if (sentences.length === 0) {
    return [normalizeText(text).slice(0, 120)].filter(Boolean);
  }

  const tokens = tokenize(text);
  const counts = frequencyMap(tokens);

  return sentences
    .map((sentence) => ({
      sentence,
      score: tokenize(sentence).reduce((total, token) => total + (counts.get(token) ?? 0), 0)
    }))
    .filter(({ sentence }) => sentence.length > 25)
    .sort((left, right) => right.score - left.score)
    .slice(0, limit)
    .map(({ sentence }) => sentence);
}

export function analyzeDocument(sourceText: string): RagAnalysis {
  const normalizedText = normalizeText(sourceText);
  const wordCount = countWords(normalizedText);
  const chunkCount = chunkText(normalizedText).length;
  const pageCount = estimatePageCount(normalizedText);
  const summary = summarizeText(normalizedText);
  const highlights = extractHighlights(normalizedText);
  const tags = extractKeywords(normalizedText, 4);
  const readerSections = buildReaderSections(sourceText);

  return {
    summary,
    highlights,
    tags,
    wordCount,
    chunkCount,
    pageCount,
    sourceText: normalizedText,
    readerSections
  };
}

function scoreChunk(queryTokens: string[], chunk: string) {
  const chunkTokens = tokenize(chunk);
  const chunkSet = new Set(chunkTokens);
  let score = 0;

  queryTokens.forEach((token) => {
    if (chunkSet.has(token)) {
      score += 2;
    }
  });

  const overlap = chunkTokens.filter((token) => queryTokens.includes(token)).length;
  score += overlap * 0.25;
  return score;
}

export function buildRagAnswer(documentTitle: string, fileName: string, sourceText: string, query: string): RagAnswer {
  const startedAt = Date.now();
  const chunks = chunkText(sourceText);
  const queryTokens = tokenize(query);

  const rankedChunks = chunks
    .map((chunk, index) => ({
      index,
      chunk,
      score: scoreChunk(queryTokens, chunk)
    }))
    .sort((left, right) => right.score - left.score);

  const context = rankedChunks.slice(0, 3).map(({ chunk }) => chunk);
  const snippets = context.length > 0 ? context : [summarizeText(sourceText)];
  const answer =
    `Based on ${documentTitle}, the most relevant parts are:\n` +
    snippets
      .map((snippet) => `- ${snippet.slice(0, 240)}`)
      .join('\n') +
    `\n\nIn short, this document suggests: ${summarizeText(sourceText, 1).slice(0, 260)}`;

  const bestScore = rankedChunks[0]?.score ?? 0;
  const confidence = Math.max(0.4, Math.min(0.98, bestScore / Math.max(queryTokens.length, 1) / 2 + 0.35));

  return {
    answer,
    context: snippets,
    sources: [fileName],
    modelUsed: 'local-rag',
    responseTime: Date.now() - startedAt,
    tokensUsed: queryTokens.length + tokenize(sourceText).length,
    confidence
  };
}
