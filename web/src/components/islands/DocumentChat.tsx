import { useEffect, useMemo, useRef, useState } from 'react';
import { buildRagAnswer } from '@/lib/rag';
import { getWorkspaceChat, saveWorkspaceChat } from '@/lib/workspace';

type ChatRole = 'user' | 'assistant';

type ChatLine = {
  role: ChatRole;
  content: string;
};

type ChatResponse = {
  answer: string;
  modelUsed?: string;
};

type Props = {
  documentId: string;
  documentTitle: string;
  documentText: string;
  documentFileName: string;
  seedMessages?: ChatLine[];
};

export default function DocumentChat({
  documentId,
  documentTitle,
  documentText,
  documentFileName,
  seedMessages = []
}: Props) {
  const [messages, setMessages] = useState<ChatLine[]>(seedMessages);
  const [input, setInput] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [modelUsed, setModelUsed] = useState('Groq ready');
  const [hydrated, setHydrated] = useState(false);
  const historyRef = useRef<HTMLDivElement | null>(null);

  const promptChips = useMemo(
    () => [
      `Summarize ${documentTitle}`,
      'What are the key takeaways?',
      'List the main action items',
      'What should I pay attention to next?'
    ],
    [documentTitle]
  );

  const canSend = useMemo(() => input.trim().length > 0 && !isSending, [input, isSending]);

  useEffect(() => {
    historyRef.current?.scrollTo({ top: historyRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, isSending]);

  useEffect(() => {
    const storedMessages = getWorkspaceChat(documentId);
    if (storedMessages.length > 0) {
      setMessages(storedMessages.map((message) => ({ role: message.role, content: message.content })));
    }
    setHydrated(true);
  }, [documentId]);

  useEffect(() => {
    if (!hydrated) {
      return;
    }

    saveWorkspaceChat(
      documentId,
      messages.map((message) => ({
        ...message,
        timestamp: Date.now()
      }))
    );
  }, [documentId, hydrated, messages]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const query = input.trim();
    if (!query || isSending) {
      return;
    }

    setInput('');
    setIsSending(true);
    setMessages((current) => [...current, { role: 'user', content: query }]);

    try {
      const response = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: {
          'content-type': 'application/json'
        },
        body: JSON.stringify({
          documentTitle,
          documentFileName,
          sourceText: documentText,
          query
        })
      });

      if (!response.ok) {
        throw new Error('Groq chat request failed.');
      }

      const payload = (await response.json()) as ChatResponse;

      setMessages((current) => [...current, { role: 'assistant', content: payload.answer }]);
      setModelUsed(payload.modelUsed ?? 'Groq');
    } catch {
      const payload = buildRagAnswer(documentTitle, documentFileName, documentText, query);

      setMessages((current) => [
        ...current,
        {
          role: 'assistant',
          content: payload.answer
        }
      ]);
      setModelUsed(payload.modelUsed ?? 'local-rag');
    } finally {
      setIsSending(false);
    }
  }

  return (
    <section className="document-chat" id="chat" aria-label="Conversational RAG">
      <header className="document-chat__header">
        <div className="document-chat__heading">
          <p className="kicker">Conversational RAG</p>
        </div>
        <span className="badge badge--queued">{modelUsed}</span>
      </header>

      <div className="document-chat__scroll">
        <div className="chat__prompts" aria-label="Suggested prompts">
          {promptChips.map((prompt) => (
            <button
              key={prompt}
              className="chat__prompt"
              type="button"
              onClick={() => setInput(prompt)}
            >
              {prompt}
            </button>
          ))}
        </div>

        <div className="chat__history" ref={historyRef}>
          {messages.length === 0 ? (
            <div className="metric" style={{ margin: 0 }}>
              <span className="metric__label">No messages yet</span>
              <span className="metric__hint">Start with a question about structure, findings, or next steps.</span>
            </div>
          ) : (
            messages.map((message, index) => (
              <div key={`${message.role}-${index}`} className={`chat__message chat__message--${message.role}`}>
                {message.content}
              </div>
            ))
          )}
          {isSending ? <div className="chat__message chat__message--assistant">Thinking through the document...</div> : null}
        </div>
      </div>

      <form className="chat__composer" onSubmit={handleSubmit}>
        <textarea
          className="chat__field"
          placeholder="What does this document recommend, and what still needs attention?"
          value={input}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault();
              event.currentTarget.form?.requestSubmit();
            }
          }}
          onChange={(event) => setInput(event.target.value)}
        />
        <div className="chat__actions">
          <div className="chat__actions-copy">
            <div className="muted">The server route grounds each answer in the strongest matching excerpts before it responds.</div>
          </div>
          <button className="button button--primary" disabled={!canSend} type="submit">
            Send question
          </button>
        </div>
      </form>
    </section>
  );
}
