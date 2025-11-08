import os, glob, uuid, time, json
from dotenv import load_dotenv
from openai import OpenAI
from pinecone import Pinecone
import PyPDF2
import fitz  # PyMuPDF - alternative PDF reader

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
    return client.embeddings.create(model=EMBED_MODEL, input=text, dimensions = 1024).data[0].embedding

def extract_text_from_pdf(path):
    """Extract text from PDF using PyMuPDF (recommended)"""
    try:
        doc = fitz.open(path)
        text = ""
        for page in doc:
            text += page.get_text()
        doc.close()
        return text
    except Exception as e:
        print(f"Error reading PDF with PyMuPDF: {e}")
        # Fallback to PyPDF2
        return extract_text_pypdf2(path)

def extract_text_pypdf2(path):
    """Fallback PDF reader using PyPDF2"""
    try:
        with open(path, 'rb') as file:
            pdf_reader = PyPDF2.PdfReader(file)
            text = ""
            for page in pdf_reader.pages:
                text += page.extract_text()
        return text
    except Exception as e:
        print(f"Error reading PDF with PyPDF2: {e}")
        return ""

def extract_text_from_json(path):
    """Extract text from JSON file"""
    try:
        with open(path, 'r', encoding='utf-8') as file:
            data = json.load(file)
        
        # Convert JSON to readable text format
        def json_to_text(obj, indent=0):
            text = ""
            if isinstance(obj, dict):
                for key, value in obj.items():
                    text += "  " * indent + f"{key}: "
                    if isinstance(value, (dict, list)):
                        text += "\n" + json_to_text(value, indent + 1)
                    else:
                        text += f"{value}\n"
            elif isinstance(obj, list):
                for i, item in enumerate(obj):
                    text += "  " * indent + f"[{i}]: "
                    if isinstance(item, (dict, list)):
                        text += "\n" + json_to_text(item, indent + 1)
                    else:
                        text += f"{item}\n"
            else:
                text += f"{obj}\n"
            return text
        
        return json_to_text(data)
    except Exception as e:
        print(f"Error reading JSON file: {e}")
        return ""

def upsert_doc(path):
    # Extract text based on file extension
    if path.lower().endswith('.pdf'):
        raw = extract_text_from_pdf(path)
    elif path.lower().endswith('.json'):
        raw = extract_text_from_json(path)
    else:
        # Handle other text files
        with open(path, "r", encoding="utf-8") as f:
            raw = f.read()
    
    if not raw.strip():
        print(f"Warning: No text extracted from {path}")
        return 0
        
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
    # Look for PDF and JSON files
    pdf_files = sorted(glob.glob(os.path.join(folder, "*.pdf")))
    print(pdf_files)
    json_files = sorted(glob.glob(os.path.join(folder, "*.json")))
    print(json_files)
    files = pdf_files + json_files
    
    if not files:
        print(f"No .pdf or .json files in {folder}")
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
