import type { APIRoute } from 'astro';
import { answerDocumentQuestionWithGroq } from '@/lib/server/groq';

export const prerender = false;

export const POST: APIRoute = async ({ request }) => {
  const body = (await request.json().catch(() => null)) as
    | {
        documentTitle?: string;
        documentFileName?: string;
        sourceText?: string;
        query?: string;
      }
    | null;

  if (!body?.sourceText || !body.sourceText.trim()) {
    return new Response(JSON.stringify({ error: 'sourceText is required.' }), {
      status: 400,
      headers: { 'content-type': 'application/json; charset=utf-8' }
    });
  }

  if (!body?.query || !body.query.trim()) {
    return new Response(JSON.stringify({ error: 'query is required.' }), {
      status: 400,
      headers: { 'content-type': 'application/json; charset=utf-8' }
    });
  }

  const answer = await answerDocumentQuestionWithGroq({
    documentTitle: body.documentTitle?.trim() || 'Untitled document',
    documentFileName: body.documentFileName?.trim() || 'document',
    sourceText: body.sourceText,
    query: body.query
  });

  return new Response(JSON.stringify(answer), {
    headers: { 'content-type': 'application/json; charset=utf-8' }
  });
};