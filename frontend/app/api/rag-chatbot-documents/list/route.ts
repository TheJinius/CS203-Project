import { NextRequest, NextResponse } from "next/server"

const PYTHON_API_URL = process.env.NEXT_PUBLIC_CHATBOT_API || "http://localhost:8000"

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export async function GET(request: NextRequest) {
  try {
    console.log("Environment variable NEXT_PUBLIC_CHATBOT_API:", process.env.NEXT_PUBLIC_CHATBOT_API)
    console.log("Using PYTHON_API_URL:", PYTHON_API_URL)
    
    const fullUrl = `${PYTHON_API_URL}/list-documents`
    console.log("Full URL:", fullUrl)
    
    // Validate URL format
    if (!PYTHON_API_URL.startsWith('http://') && !PYTHON_API_URL.startsWith('https://')) {
      console.error("Invalid PYTHON_API_URL - missing http/https protocol:", PYTHON_API_URL)
      return NextResponse.json(
        { 
          error: "Backend URL misconfigured",
          details: `Invalid URL: ${PYTHON_API_URL}`
        },
        { status: 500 }
      )
    }
    
    // Call Python backend to list documents
    const response = await fetch(fullUrl, {
      method: "GET",
    })

    console.log("Response status:", response.status)

    if (!response.ok) {
      const errorText = await response.text()
      console.error("Backend error response:", errorText)
      
      let error
      try {
        error = JSON.parse(errorText)
      } catch {
        error = { detail: errorText }
      }
      
      return NextResponse.json(
        { error: error.detail || "Failed to list documents" },
        { status: response.status }
      )
    }

    const resultText = await response.text()
    console.log("Backend response text:", resultText)
    
    const result = JSON.parse(resultText)
    console.log("Parsed result:", result)

    return NextResponse.json({
      success: true,
      documents: result.documents || [],
      total_vectors: result.total_vectors || 0,
    })
  } catch (error) {
    console.error("List documents error:", error)
    console.error("Error stack:", error instanceof Error ? error.stack : "No stack trace")
    return NextResponse.json(
      { 
        error: "Failed to list documents",
        details: error instanceof Error ? error.message : String(error)
      },
      { status: 500 }
    )
  }
}
