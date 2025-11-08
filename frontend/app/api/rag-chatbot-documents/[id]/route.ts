import { NextRequest, NextResponse } from "next/server"

const PYTHON_API_URL = process.env.PYTHON_API_URL || "http://localhost:8000"

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
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
    return NextResponse.json(
      { error: "Failed to delete document" },
      { status: 500 }
    )
  }
}
