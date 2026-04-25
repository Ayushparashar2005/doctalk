import type { APIRoute } from 'astro';
import { analyzeDocumentWithGroq } from '@/lib/server/groq';

export const prerender = false;

export const POST: APIRoute = async ({ request }) => {
  const body = (await request.json().catch(() => null)) as
    | {
        title?: string;
        fileName?: string;
        sourceText?: string;
      }
    | null;

  if (!body?.sourceText || !body.sourceText.trim()) {
    return new Response(JSON.stringify({ error: 'sourceText is required.' }), {
      status: 400,
      headers: { 'content-type': 'application/json; charset=utf-8' }
    });
  }

  const analysis = await analyzeDocumentWithGroq({
    title: body.title?.trim() || body.fileName?.replace(/\.[^.]+$/, '') || 'Untitled document',
    fileName: body.fileName?.trim() || 'document',
    sourceText: body.sourceText
  });

  return new Response(JSON.stringify(analysis), {
    headers: { 'content-type': 'application/json; charset=utf-8' }
  });
};