import { NextRequest, NextResponse } from "next/server"

const PYTHON_API_URL = process.env.NEXT_PUBLIC_CHATBOT_API || "http://localhost:8000"

export async function POST(request: NextRequest) {
  try {
    console.log("Upload - PYTHON_API_URL:", PYTHON_API_URL)
    
    // Validate URL format
    if (!PYTHON_API_URL.startsWith('http://') && !PYTHON_API_URL.startsWith('https://')) {
      return NextResponse.json(
        { error: "Backend URL misconfigured" },
        { status: 500 }
      )
    }
    
    const formData = await request.formData()
    const file = formData.get("file") as File

    if (!file) {
      return NextResponse.json(
        { error: "No file provided" },
        { status: 400 }
      )
    }

    // Validate file type
    if (file.type !== "application/pdf") {
      return NextResponse.json(
        { error: "Only PDF files are allowed" },
        { status: 400 }
      )
    }

    // Forward the file to Python backend
    const pythonFormData = new FormData()
    pythonFormData.append("file", file)

    const response = await fetch(`${PYTHON_API_URL}/upload-document`, {
      method: "POST",
      body: pythonFormData,
    })

    if (!response.ok) {
      const error = await response.json()
      return NextResponse.json(
        { error: error.detail || "Failed to upload document" },
        { status: response.status }
      )
    }

    const result = await response.json()

    return NextResponse.json({
      success: true,
      id: result.filename, // Use filename as ID
      filename: result.filename,
      chunks_added: result.chunks_added,
      message: result.message,
    })
  } catch (error) {
    console.error("Upload error:", error)
    console.error("Error details:", error instanceof Error ? error.message : String(error))
    return NextResponse.json(
      { 
        error: "Failed to upload document",
        details: error instanceof Error ? error.message : String(error)
      },
      { status: 500 }
    )
  }
}
