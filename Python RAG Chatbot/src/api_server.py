from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import uvicorn
from chat_cli import answer

app = FastAPI(title="Tariff RAG Chatbot API")

# Add CORS middleware to allow frontend connections
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000", 
        "http://localhost:3001",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:3001"
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class QueryRequest(BaseModel):
    question: str

@app.post("/query")
def query_rag(req: QueryRequest):
    try:
        ans, refs = answer(req.question)
        sources = []
        for r in refs:
            meta = r["metadata"]
            sources.append({
                "source_document": meta.get("source", "?"),
                "line_range": f"{meta.get('line_start', '?')}–{meta.get('line_end', '?')}",
                "text": meta.get("text", "")
            })

        return {"response": ans, "sources": sources}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
