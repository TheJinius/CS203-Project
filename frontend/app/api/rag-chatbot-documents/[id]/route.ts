import { NextRequest, NextResponse } from "next/server"

const PYTHON_API_URL = process.env.NEXT_PUBLIC_CHATBOT_API || "http://localhost:8000"

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    console.log("Delete - PYTHON_API_URL:", PYTHON_API_URL)
    
    // Validate URL format
    if (!PYTHON_API_URL.startsWith('http://') && !PYTHON_API_URL.startsWith('https://')) {
      return NextResponse.json(
        { error: "Backend URL misconfigured" },
        { status: 500 }
      )
    }
    
    const { id } = await params

    if (!id) {
      return NextResponse.json(
        { error: "Document ID is required" },
        { status: 400 }
      )
    } 

    // Call Python backend to delete the document
    const response = await fetch(`${PYTHON_API_URL}/delete-document/${encodeURIComponent(id)}`, {
      method: "DELETE",
    })

    if (!response.ok) {
      const error = await response.json()
      return NextResponse.json(
        { error: error.detail || "Failed to delete document" },
        { status: response.status }
      )
    }

    const result = await response.json()

    return NextResponse.json({
      success: true,
      filename: result.filename,
      chunks_deleted: result.chunks_deleted,
      message: result.message,
    })
  } catch (error) {
    console.error("Delete error:", error)
    console.error("Error details:", error instanceof Error ? error.message : String(error))
    return NextResponse.json(
      { 
        error: "Failed to delete document",
        details: error instanceof Error ? error.message : String(error)
      },
      { status: 500 }
    )
  }
}
