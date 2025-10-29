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

SYSTEM = "First using the input, identify the country and type of product: metals, agriculture, energy or others. Then return zero or more compliance tasks as a JSON array, each in the strict json format with these keys : {country, sector, task_category, task_name, description, responsible_agency, compliance_requirement, timing, reference, reference_url}. only return results that are relevant to the stated country and product type. Before returning, look at the task_name and description of the result and only return those that may be a compliance requirement for the given input. It is fine to return nothing. Return ONLY the JSON array without any markdown formatting or code blocks."

def embed(text: str):
    """Create an embedding for a given text using OpenAI."""
    return client.embeddings.create(model=EMBED_MODEL, input=text, dimensions= 1024).data[0].embedding


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
    
    # Remove markdown code blocks and other formatting
    cleaned = raw.replace("**", "").replace("*", "")
    
    # Remove markdown code block syntax
    if cleaned.startswith("```json"):
        cleaned = cleaned[7:]  # Remove ```json
    if cleaned.startswith("```"):
        cleaned = cleaned[3:]   # Remove ```
    if cleaned.endswith("```"):
        cleaned = cleaned[:-3]  # Remove ending ```
    
    # Clean up any remaining whitespace
    cleaned = cleaned.strip()
    
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

        output = {"response": ans}
        print(json.dumps(output, indent=2))
