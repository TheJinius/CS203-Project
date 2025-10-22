import os, json
from dotenv import load_dotenv
from openai import OpenAI
from pinecone import Pinecone

load_dotenv()

CHAT_MODEL = os.getenv("CHAT_MODEL", "gpt-4o-mini")
EMBED_MODEL = os.getenv("EMBED_MODEL", "text-embedding-3-small")
INDEX_NAME = os.getenv("PINECONE_INDEX", "chatbot-index")
NAMESPACE = os.getenv("PINECONE_NAMESPACE", "")
TOP_K = int(os.getenv("TOP_K", "3"))
SCORE_THRESHOLD = float(os.getenv("SCORE_THRESHOLD", "0.55"))

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
index = pc.Index(INDEX_NAME)

SYSTEM = "Answer using only the provided context. If the context is insufficient, say so."

def embed(text: str):
    """Create an embedding for a given text using OpenAI."""
    return client.embeddings.create(model=EMBED_MODEL, input=text).data[0].embedding


def retrieve(query: str, top_k: int = TOP_K):
    """Retrieve top-k similar chunks from Pinecone based on semantic and keyword filters."""
    qvec = embed(query)
    res = index.query(
        vector=qvec,
        top_k=top_k,
        include_metadata=True,
        namespace=NAMESPACE
    )

    matches = res.get("matches", [])
    if not matches:
        return "", []

    # Step 1. Score filter
    high_scores = [m for m in matches if m.get("score", 0) >= SCORE_THRESHOLD]
    matches = high_scores if high_scores else matches

    # Step 2. Keyword relevance filter
    query_lower = query.lower()
    keywords = [w for w in query_lower.replace("?", "").replace(".", "").split() if len(w) > 2]

    filtered = []
    for m in matches:
        text = m["metadata"]["text"].lower()
        if any(k in text for k in keywords):
            filtered.append(m)
    if filtered:
        matches = filtered

    # Step 3. Context build
    snippets = [m["metadata"]["text"] for m in matches]
    return "\n\n".join(snippets), matches


def answer(query: str):
    """Send the query + retrieved context to ChatGPT for a grounded answer."""
    context, matches = retrieve(query)
    prompt = f"Context:\n{context}\n\nQuestion:\n{query}"

    resp = client.chat.completions.create(
        model=CHAT_MODEL,
        temperature=0.2,
        messages=[
            {"role": "system", "content": SYSTEM},
            {"role": "user", "content": prompt}
        ],
    )

    raw = resp.choices[0].message.content.strip()
    cleaned = raw.replace("**", "").replace("*", "")
    return cleaned, matches


if __name__ == "__main__":
    print("Ask a question. Ctrl+C to exit.")
    while True:
        q = input("> ").strip()
        if not q:
            continue

        ans, refs = answer(q)

        print("========= Chatbot Answer =========")
        print(ans)
        print("========= Sources =========")

        sources = []
        if refs:
            for r in refs:
                meta = r["metadata"]
                text = meta.get("text", "")
                source = meta.get("source", "?")

                # Estimate approximate line position
                if q.split():
                    first_word = q.split()[0].lower()
                    pos = text.lower().find(first_word)
                    approx_line = text[:pos].count("\n") + 1 if pos >= 0 else 1
                else:
                    approx_line = 1

                total_lines = text.count("\n") + 1

                sources.append({
                    "source_document": source,
                    "approx_line": approx_line,
                    "total_lines_in_chunk": total_lines,
                    "text": text
                })

        output = {"response": ans, "sources": sources}
        print(json.dumps(output, indent=2))
