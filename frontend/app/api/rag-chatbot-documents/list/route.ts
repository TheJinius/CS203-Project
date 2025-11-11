import { NextRequest, NextResponse } from "next/server"

// const PYTHON_API_URL = process.env.PYTHON_API_URL || "http://localhost:8000"
const PYTHON_API_URL = process.env.PYTHON_API_URL || "https://cs203chatbot.duckdns.org"

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export async function GET(request: NextRequest) {
  try {
    // Call Python backend to list documents
    const response = await fetch(`${PYTHON_API_URL}/list-documents`, {
      method: "GET",
    })

    if (!response.ok) {
      const error = await response.json()
      return NextResponse.json(
        { error: error.detail || "Failed to list documents" },
        { status: response.status }
      )
    }

    const result = await response.json()

    return NextResponse.json({
      success: true,
      documents: result.documents,
      total_vectors: result.total_vectors,
    })
  } catch (error) {
    console.error("List documents error:", error)
    return NextResponse.json(
      { error: "Failed to list documents" },
      { status: 500 }
    )
  }
}
