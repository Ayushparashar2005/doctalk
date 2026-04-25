import { analyzeDocument, buildRagAnswer, chunkText, normalizeText, tokenize, type RagAnalysis, type RagAnswer } from '@/lib/rag';

const GROQ_MODEL = 'llama-3.3-70b-versatile';
const GROQ_API_URL = 'https://api.groq.com/openai/v1/chat/completions';

export type GroqAnalysisResult = RagAnalysis & {
  modelUsed: string;
  fallbackUsed: boolean;
};

export type GroqAnswerResult = RagAnswer & {
  fallbackUsed: boolean;
};

function getGroqApiKey() {
  return import.meta.env.GROQ_API_KEY ?? '';
}

function extractJsonObject(content: string) {
  const trimmed = content.trim();
  const fenced = trimmed.match(/```(?:json)?\s*([\s\S]*?)```/i);
  const candidate = (fenced?.[1] ?? trimmed).trim();
  const start = candidate.indexOf('{');
  const end = candidate.lastIndexOf('}');
  const jsonText = start >= 0 && end > start ? candidate.slice(start, end + 1) : candidate;
  return JSON.parse(jsonText) as Record<string, unknown>;
}

function cleanStringArray(value: unknown, fallback: string[], limit = fallback.length) {
  if (!Array.isArray(value)) {
    return fallback;
  }

  const cleaned = value
    .map((entry) => (typeof entry === 'string' ? normalizeText(entry) : ''))
    .filter(Boolean)
    .slice(0, limit);

  return cleaned.length > 0 ? cleaned : fallback;
}

function rankRelevantChunks(sourceText: string, query: string) {
  const chunks = chunkText(sourceText, 900, 120);
  const queryTokens = tokenize(query);

  return chunks
    .map((chunk, index) => {
      const chunkTokens = tokenize(chunk);
      const chunkSet = new Set(chunkTokens);
      const overlap = queryTokens.reduce((score, token) => score + (chunkSet.has(token) ? 2 : 0), 0);
      const repeatedOverlap = chunkTokens.reduce(
        (score, token) => score + (queryTokens.includes(token) ? 0.25 : 0),
        0
      );

      return {
        index,
        chunk,
        score: overlap + repeatedOverlap
      };
    })
    .sort((left, right) => right.score - left.score);
}

async function callGroq(messages: Array<{ role: 'system' | 'user' | 'assistant'; content: string }>, maxTokens: number) {
  const apiKey = getGroqApiKey();
  if (!apiKey) {
    throw new Error('Groq API key is not configured.');
  }

  const response = await fetch(GROQ_API_URL, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      model: GROQ_MODEL,
      messages,
      temperature: 0.2,
      max_tokens: maxTokens,
      response_format: { type: 'json_object' }
    })
  });

  if (!response.ok) {
    const detail = await response.text();
    throw new Error(`Groq request failed (${response.status}): ${detail}`);
  }

  const payload = (await response.json()) as {
    choices?: Array<{
      message?: {
        content?: string;
      };
    }>;
  };

  const content = payload.choices?.[0]?.message?.content?.trim();
  if (!content) {
    throw new Error('Groq returned an empty response.');
  }

  return content;
}

export async function analyzeDocumentWithGroq(input: {
  title: string;
  fileName: string;
  sourceText: string;
}): Promise<GroqAnalysisResult> {
  const fallback = analyzeDocument(input.sourceText);
  const clippedText = normalizeText(input.sourceText).slice(0, 28000);

  try {
    const content = await callGroq(
      [
        {
          role: 'system',
          content:
            'You analyze documents for a reading workspace. Return only JSON with summary, highlights, and tags. Summary must be 2 to 3 concise sentences. Highlights must be 3 to 5 short bullet-style strings. Tags must be 4 to 6 lowercase topical keywords.'
        },
        {
          role: 'user',
          content: [
            `Title: ${input.title}`,
            `File name: ${input.fileName}`,
            'Document text:',
            clippedText
          ].join('\n')
        }
      ],
      350
    );

    const parsed = extractJsonObject(content);

    return {
      ...fallback,
      summary: typeof parsed.summary === 'string' && parsed.summary.trim() ? normalizeText(parsed.summary) : fallback.summary,
      highlights: cleanStringArray(parsed.highlights, fallback.highlights, 5),
      tags: cleanStringArray(parsed.tags, fallback.tags, 6),
      modelUsed: `groq/${GROQ_MODEL}`,
      fallbackUsed: false
    };
  } catch {
    return {
      ...fallback,
      modelUsed: 'local-fallback',
      fallbackUsed: true
    };
  }
}

export async function answerDocumentQuestionWithGroq(input: {
  documentTitle: string;
  documentFileName: string;
  sourceText: string;
  query: string;
}): Promise<GroqAnswerResult> {
  const fallback = buildRagAnswer(input.documentTitle, input.documentFileName, input.sourceText, input.query);
  const startedAt = Date.now();
  const rankedChunks = rankRelevantChunks(input.sourceText, input.query);
  const context = rankedChunks.slice(0, 4).map(({ chunk }) => chunk);
  const clippedContext = context.length > 0 ? context : [fallback.answer];
  const queryTokens = tokenize(input.query);
  const sourceTokens = tokenize(input.sourceText);

  try {
    const content = await callGroq(
      [
        {
          role: 'system',
          content:
            'You answer questions using only the provided document context. Return only JSON with answer, confidence, and sources. Answer should be direct, specific, and grounded in the context. If the context is incomplete, say what is missing rather than guessing.'
        },
        {
          role: 'user',
          content: [
            `Document title: ${input.documentTitle}`,
            `File name: ${input.documentFileName}`,
            `Question: ${input.query}`,
            'Context excerpts:',
            ...clippedContext.map((chunk, index) => `${index + 1}. ${chunk}`)
          ].join('\n')
        }
      ],
      650
    );

    const parsed = extractJsonObject(content);
    const answer = typeof parsed.answer === 'string' && parsed.answer.trim() ? normalizeText(parsed.answer) : fallback.answer;
    const confidence = typeof parsed.confidence === 'number' ? parsed.confidence : fallback.confidence;

    return {
      answer,
      context,
      sources: [input.documentFileName],
      modelUsed: `groq/${GROQ_MODEL}`,
      responseTime: Date.now() - startedAt,
      tokensUsed: queryTokens.length + sourceTokens.length,
      confidence,
      fallbackUsed: false
    };
  } catch {
    return {
      ...fallback,
      fallbackUsed: true
    };
  }
}