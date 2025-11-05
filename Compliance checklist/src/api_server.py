from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import uvicorn
import json
from src.chat_cli import answer

app = FastAPI(title="Tariff RAG Compliance")

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
    print(f"Request: {req}")
    try:
        # answer() returns a tuple (response_string, matches)
        ans, matches = answer(req.question)
        
        # Extract just the response string
        if isinstance(ans, str):
            response_str = ans
        else:
            response_str = str(ans)
        
        print(f"Response: {response_str}")
        return {"response": response_str}
    except Exception as e:
        print(f"Error in query_rag: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001)
