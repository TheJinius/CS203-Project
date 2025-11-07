from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import uvicorn
from .chat_cli import answer

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

import re

@app.get("/health")
def health_check():
    """Health check endpoint to verify API is running"""
    return {
        "status": "healthy",
        "service": "Tariff RAG Chatbot API",
        "version": "1.0.0"
    }

@app.post("/query")
def query_rag(req: QueryRequest):
    try:
        ans, refs = answer(req.question)
        
        # Helper function to split text into sentences
        def split_into_sentences(text):
            # Split on period, exclamation, question mark followed by space or newline
            sentences = re.split(r'(?<=[.!?])\s+(?=[A-Z(])', text)
            return [s.strip() for s in sentences if s.strip()]
        
        # Extract key concepts from the response and query DYNAMICALLY
        response_lower = ans.lower()
        query_lower = req.question.lower()
        
        # Extract all significant words from query (nouns, technical terms)
        query_words = re.findall(r'\b[a-z]{4,}\b', query_lower)  # Words 4+ chars
        stopwords = {'what', 'when', 'where', 'which', 'does', 'have', 'there', 'they', 'this', 
                     'that', 'with', 'from', 'about', 'between', 'the', 'and', 'for', 'are', 'but',
                     'not', 'you', 'all', 'can', 'her', 'was', 'one', 'our', 'out', 'were', 'will'}
        key_query_terms = [w for w in query_words if w not in stopwords]
        
        # Extract compound terms (any hyphenated words)
        compound_terms = re.findall(r'\b\w+(?:-\w+)+\b', query_lower)
        
        # Extract multi-word phrases from query (2-3 word combinations)
        query_phrases = re.findall(r'\b[a-z]+\s+[a-z]+(?:\s+[a-z]+)?\b', query_lower)
        query_phrases = [p for p in query_phrases if len(p) > 8]  # Only meaningful phrases
        
        # Extract percentage mentions from response
        percent_terms = re.findall(r'\b\d+\s*percent\b', response_lower)
        
        # Extract numeric constraints from response
        numeric_patterns = re.findall(r'(?:more than|less than|at least|exceeding?|up to)\s+\d+', response_lower)
        
        # Generic constraint/composition words
        constraint_words = ['must', 'may', 'cannot', 'shall', 'should', 'exceed', 'less than', 
                           'more than', 'at least', 'contain', 'consist', 'predominate', 
                           'include', 'require', 'allow', 'permit', 'restrict']
        
        sources = []
        for r in refs:
            meta = r["metadata"]
            source_text = meta.get("text", "")
            
            # Split source into sentences
            sentences = split_into_sentences(source_text)
            
            # Find relevant sentences that contain important information
            relevant_sentences = []
            for sentence in sentences:
                sentence_lower = sentence.lower()
                
                # Check if sentence contains important terms or concepts
                score = 0
                
                # Check for key query terms
                for term in key_query_terms:
                    if term in sentence_lower:
                        score += 2
                
                # Check for compound terms from query
                for term in compound_terms:
                    if term in sentence_lower:
                        score += 4
                
                # Check for query phrases
                for phrase in query_phrases:
                    if phrase in sentence_lower:
                        score += 3
                
                # Check for percentage information
                for term in percent_terms:
                    if term in sentence_lower:
                        score += 3
                
                # Check for numeric patterns
                for pattern in numeric_patterns:
                    if pattern in sentence_lower:
                        score += 2
                
                # Check for constraint/composition words
                for word in constraint_words:
                    if word in sentence_lower:
                        score += 1
                
                # If sentence is relevant (score > 1 to filter noise), include it
                if score > 1:
                    relevant_sentences.append(sentence)
            
            # Merge consecutive relevant sentences into continuous regions
            merged_regions = []
            if relevant_sentences:
                # Find positions of relevant sentences in original text
                current_region = []
                for sent in relevant_sentences:
                    if sent in source_text:
                        # Check if this sentence is consecutive with the last one
                        if current_region:
                            # Find if sentences are adjacent in original text
                            last_sent = current_region[-1]
                            last_end = source_text.find(last_sent) + len(last_sent)
                            current_start = source_text.find(sent)
                            
                            # If they're close (within 50 chars), merge them
                            if current_start - last_end < 50:
                                current_region.append(sent)
                            else:
                                # Start new region
                                merged_regions.append(' '.join(current_region))
                                current_region = [sent]
                        else:
                            current_region.append(sent)
                
                # Add the last region
                if current_region:
                    merged_regions.append(' '.join(current_region))
            
            sources.append({
                "source_document": meta.get("source", "?"),
                "line_range": f"{meta.get('line_start', '?')}–{meta.get('line_end', '?')}",
                "text": source_text,
                "score": r.get("score", 0),
                "highlight_regions": merged_regions  # Complete sentences/paragraphs to highlight
            })

        return {"response": ans, "sources": sources}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
