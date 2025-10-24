import os, glob, uuid, time
from dotenv import load_dotenv
from openai import OpenAI
from pinecone import Pinecone

load_dotenv()

EMBED_MODEL = os.getenv("EMBED_MODEL", "text-embedding-3-small")
INDEX_NAME = os.getenv("PINECONE_INDEX", "chatbot-index")
NAMESPACE = os.getenv("PINECONE_NAMESPACE", "")  # optional, keep "" if not used

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
index = pc.Index(INDEX_NAME)

def chunk(text, size=1800):
    # simple character chunker; swap for token-aware later
    return [text[i:i+size] for i in range(0, len(text), size)]

def embed(text):
    return client.embeddings.create(model=EMBED_MODEL, input=text).data[0].embedding

def upsert_doc(path):
    with open(path, "r", encoding="utf-8") as f:
        raw = f.read()
    pieces = chunk(raw)
    vectors = []
    base = os.path.basename(path)
    for i, p in enumerate(pieces):
        vid = f"{base}-{i}-{uuid.uuid4().hex[:8]}"
        vectors.append(
            (vid, embed(p), {"text": p, "source": base, "chunk": i})
        )
    index.upsert(vectors=vectors, namespace=NAMESPACE)
    return len(vectors)

def main(folder="data"):
    files = sorted(glob.glob(os.path.join(folder, "*.txt")))
    if not files:
        print(f"No .txt files in {folder}")
        return
    total = 0
    for p in files:
        n = upsert_doc(p)
        print(f"Upserted {n} chunks from {p}")
        total += n
    # brief wait for consistency
    time.sleep(1)
    stats = index.describe_index_stats()
    print("Index stats:", stats)
    print(f"Done. Chunks added: {total}")

if __name__ == "__main__":
    import sys
    folder = sys.argv[1] if len(sys.argv) > 1 else "data"
    main(folder)
