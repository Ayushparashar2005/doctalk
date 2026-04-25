# DocTalk Web

Standalone Astro recreation of DocTalk with browser-based uploads, local document storage, and in-project RAG chat.

## What The App Does

The flow is intentionally simple:

- upload a PDF or TXT file
- extract and analyze the document in the browser
- store the document in localStorage
- open the document page and ask questions from that document

The app no longer depends on the Android or FastAPI folders in this repository.

## Architecture

- `src/pages/index.astro` is the landing page.
- `src/pages/app/index.astro` renders the workspace dashboard.
- `src/pages/app/upload.astro` hosts the browser upload flow.
- `src/pages/app/documents/[id].astro` hosts the document workspace and chat.
- `src/lib/rag.ts` contains the local RAG and summarization logic.
- `src/lib/workspace.ts` stores documents and chat history in the browser.

## Development

```bash
cd web
bun install
bun run dev
```

## Deployment

This project is ready for Cloudflare Pages and Workers through the Astro Cloudflare adapter.

- Build: `bun run build`
- Preview: `bun run preview`
- Pages deploy: `wrangler pages deploy dist`

No external backend service is required for the app to run.
