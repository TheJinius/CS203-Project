import os, glob, uuid, time
from dotenv import load_dotenv
from openai import OpenAI
from pinecone import Pinecone
from pypdf import PdfReader

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
    # Use dimensions=1024 to match Pinecone index dimension
    return client.embeddings.create(
        model=EMBED_MODEL, 
        input=text,
        dimensions=1024
    ).data[0].embedding

def read_file(path):
    """Read content from either txt or pdf file"""
    ext = os.path.splitext(path)[1].lower()
    if ext == '.pdf':
        reader = PdfReader(path)
        text = ""
        for page in reader.pages:
            text += page.extract_text() + "\n"
        return text
    else:  # assume text file
        with open(path, "r", encoding="utf-8") as f:
            return f.read()

def upsert_doc(path):
    raw = read_file(path)
    pieces = chunk(raw)
    vectors = []
    base = os.path.basename(path)
    for i, p in enumerate(pieces):
        # Sanitize the ID to only ASCII characters
        safe_base = base.encode('ascii', 'ignore').decode('ascii')
        vid = f"{safe_base}-{i}-{uuid.uuid4().hex[:8]}"
        vectors.append(
            (vid, embed(p), {"text": p, "source": base, "chunk": i})
        )
    # Only pass namespace if it's not empty
    if NAMESPACE:
        index.upsert(vectors=vectors, namespace=NAMESPACE)
    else:
        index.upsert(vectors=vectors)
    return len(vectors)

def main(folder="data"):
    txt_files = sorted(glob.glob(os.path.join(folder, "*.txt")))
    pdf_files = sorted(glob.glob(os.path.join(folder, "*.pdf")))
    files = txt_files + pdf_files
    if not files:
        print(f"No .txt or .pdf files in {folder}")
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
    # Default to data folder relative to script location (one level up)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    default_data = os.path.join(os.path.dirname(script_dir), "data")
    folder = sys.argv[1] if len(sys.argv) > 1 else default_data
    main(folder)
