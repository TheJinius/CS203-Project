import os
import json
from dotenv import load_dotenv
from openai import OpenAI
from pinecone import Pinecone

# =======================================
# Load environment variables
# =======================================
load_dotenv()

CHAT_MODEL = os.getenv("CHAT_MODEL", "gpt-4o-mini")
EMBED_MODEL = os.getenv("EMBED_MODEL", "text-embedding-3-small")
INDEX_NAME = os.getenv("PINECONE_INDEX", "chatbot-index")
NAMESPACE = os.getenv("PINECONE_NAMESPACE", "") or None
TOP_K = int(os.getenv("TOP_K", "3"))
SCORE_THRESHOLD = float(os.getenv("SCORE_THRESHOLD", "0.55"))
DEBUG = os.getenv("DEBUG", "true").lower() == "true"

# =======================================
# Initialize clients
# =======================================
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
index = pc.Index(INDEX_NAME)

SYSTEM = "Answer using only the provided context. If the context is insufficient, say so."


# =======================================
# Embedding helper
# =======================================
def embed(text: str):
    """Create an embedding for a given text using OpenAI."""
    res = client.embeddings.create(
        model=EMBED_MODEL,
        input=text,
        dimensions=1024
    )
    return res.data[0].embedding


# =======================================
# Retrieval from Pinecone
# =======================================
def retrieve(query: str, top_k: int = TOP_K):
    """Retrieve top-k relevant chunks from Pinecone."""
    if DEBUG:
        print(f"[DEBUG] Retrieving for query: {query}")

    qvec = embed(query)
    res = index.query(
        vector=qvec,
        top_k=top_k,
        include_metadata=True,
        namespace=NAMESPACE
    )

    matches = getattr(res, "matches", [])
    if not matches:
        if DEBUG:
            print("[DEBUG] No matches found.")
        return "", []

    # Filter by score threshold
    high_scores = [m for m in matches if getattr(m, "score", 0) >= SCORE_THRESHOLD]
    if high_scores:
        matches = high_scores

    # Build text context
    snippets = [m.metadata.get("text", "") for m in matches if hasattr(m, "metadata")]
    context = "\n\n".join(snippets)
    
    if DEBUG:
        print(f"[DEBUG] Retrieved {len(matches)} matches:")
        for i, m in enumerate(matches, 1):
            print(f"  {i}. Score={m.score:.3f} | Source={m.metadata.get('source', 'N/A')}")

    return context, matches


# =======================================
# Generate grounded answer
# =======================================
def answer(query: str):
    """Send query + retrieved context to ChatGPT for a grounded answer."""
    context, matches = retrieve(query)

    if not context.strip():
        return "No relevant context found in the database.", []

    prompt = f"Context:\n{context}\n\nQuestion:\n{query}"

    resp = client.chat.completions.create(
        model=CHAT_MODEL,
        temperature=0.2,
        messages=[
            {"role": "system", "content": SYSTEM},
            {"role": "user", "content": prompt}
        ]
    )

    raw = resp.choices[0].message.content.strip()
    cleaned = raw.replace("**", "").replace("*", "")
    return cleaned, matches


# =======================================
# CLI loop
# =======================================
if __name__ == "__main__":
    print("💬 Chatbot CLI (Pinecone + OpenAI)")
    print("Type a question (Ctrl+C to exit)")
    print("=====================================")

    while True:
        try:
            q = input("> ").strip()
            if not q:
                continue

            ans, refs = answer(q)

            print("\n========= Chatbot Answer =========")
            print(ans)

            print("\n========= Sources =========")
            sources = []
            if refs:
                for r in refs:
                    meta = r.metadata
                    text = meta.get("text", "")
                    source = meta.get("source", "?")

                    # Approximate match line
                    first_word = q.split()[0].lower() if q.split() else ""
                    pos = text.lower().find(first_word)
                    approx_line = text[:pos].count("\n") + 1 if pos >= 0 else 1
                    total_lines = text.count("\n") + 1

                    sources.append({
                        "source_document": source,
                        "score": r.score,
                        "approx_line": approx_line,
                        "total_lines_in_chunk": total_lines,
       "text_preview": text[:250] + ("..." if len(text) > 250 else "")
                    })

            output = {"response": ans, "sources": sources}
            print(json.dumps(output, indent=2))

        except KeyboardInterrupt:
            print("\nExiting chat_cli.")
            break
        except Exception as e:
            print(f"[ERROR] {e}")

