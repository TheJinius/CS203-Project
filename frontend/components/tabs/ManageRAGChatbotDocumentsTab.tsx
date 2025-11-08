"use client"

import { useState, useEffect } from "react"
import { Upload, File, Trash2, AlertCircle, CheckCircle, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Alert, AlertDescription } from "@/components/ui/alert"

interface UploadedDocument {
  id: string
  filename: string
  uploadedAt?: string
  size?: number
  chunks?: number
}

export default function ManageRAGChatbotDocumentsTab() {
  const [documents, setDocuments] = useState<UploadedDocument[]>([])
  const [uploading, setUploading] = useState(false)
  const [loading, setLoading] = useState(true)
  const [uploadProgress, setUploadProgress] = useState<string>("")
  const [error, setError] = useState<string>("")
  const [success, setSuccess] = useState<string>("")

  // Fetch existing documents on mount
  useEffect(() => {
    fetchDocuments()
  }, [])

  const fetchDocuments = async () => {
    try {
      setLoading(true)
      const response = await fetch("/api/rag-chatbot-documents/list")
      
      if (!response.ok) {
        throw new Error("Failed to fetch documents")
      }

      const result = await response.json()
      
      if (result.success && result.documents) {
        setDocuments(result.documents)
      }
    } catch (err) {
      console.error("Error fetching documents:", err)
      // Don't set error state here to avoid showing error on initial load
    } finally {
      setLoading(false)
    }
  }

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files
    if (!files || files.length === 0) return

    setUploading(true)
    setError("")
    setSuccess("")
    setUploadProgress("")

    try {
      for (let i = 0; i < files.length; i++) {
        const file = files[i]
        
        // Validate file type
        if (file.type !== "application/pdf") {
          setError(`${file.name} is not a PDF file`)
          continue
        }

        setUploadProgress(`Uploading ${file.name}... (${i + 1}/${files.length})`)

        // Create FormData
        const formData = new FormData()
        formData.append("file", file)

        // Updated API endpoint
        const response = await fetch("/api/rag-chatbot-documents/upload", {
          method: "POST",
          body: formData,
        })

        if (!response.ok) {
          throw new Error(`Failed to upload ${file.name}`)
        }

        const result = await response.json()

        // Add to documents list
        const newDoc: UploadedDocument = {
          id: result.id || result.filename,
          filename: result.filename,
          chunks: result.chunks_added,
        }

        setDocuments(prev => [...prev, newDoc])
      }

      setSuccess(`Successfully uploaded ${files.length} document(s)`)
      setUploadProgress("")
      
      // Refresh the documents list
      await fetchDocuments()
      
      // Clear the input
      event.target.value = ""
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to upload documents")
    } finally {
      setUploading(false)
      setUploadProgress("")
    }
  }

  const handleDelete = async (docId: string) => {
    if (!confirm("Are you sure you want to delete this document?")) return

    try {
      // Updated API endpoint
      const response = await fetch(`/api/rag-chatbot-documents/${docId}`, {
        method: "DELETE",
      })

      if (!response.ok) {
        throw new Error("Failed to delete document")
      }

      const result = await response.json()
      
      // Refresh the documents list
      await fetchDocuments()
      setSuccess(result.message || "Document deleted successfully")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete document")
    }
  }

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + " B"
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + " KB"
    return (bytes / (1024 * 1024)).toFixed(2) + " MB"
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  return (
    <div className="p-4 space-y-4">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl font-semibold">Manage RAG Chatbot Documents</CardTitle>
          <CardDescription>
            Upload PDF documents to enhance the RAG chatbot&apos;s knowledge base
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Upload Section */}
          <div className="border-2 border-dashed border-slate-300 dark:border-slate-600 rounded-lg p-6 text-center">
            <Upload className="h-12 w-12 mx-auto mb-4 text-slate-400" />
            <h3 className="text-lg font-medium mb-2">Upload PDF Documents</h3>
            <p className="text-sm text-slate-500 dark:text-slate-400 mb-4">
              Select one or more PDF files to add to the knowledge base
            </p>
            <input
              type="file"
              id="file-upload"
              accept=".pdf"
              multiple
              onChange={handleFileUpload}
              disabled={uploading}
              className="hidden"
            />
            <Button
              onClick={() => document.getElementById("file-upload")?.click()}
              disabled={uploading}
              className="bg-blue-600 hover:bg-blue-700"
            >
              {uploading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  {uploadProgress || "Uploading..."}
                </>
              ) : (
                <>
                  <Upload className="h-4 w-4 mr-2" />
                  Choose Files
                </>
              )}
            </Button>
          </div>

          {/* Status Messages */}
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {success && (
            <Alert className="bg-green-50 dark:bg-green-900/20 border-green-500 text-green-700 dark:text-green-400">
              <CheckCircle className="h-4 w-4" />
              <AlertDescription>{success}</AlertDescription>
            </Alert>
          )}

          {/* Documents List */}
          <div className="space-y-2">
            <h3 className="text-lg font-medium">Uploaded Documents</h3>
            {loading ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
                <span className="ml-2 text-sm text-slate-500">Loading documents...</span>
              </div>
            ) : documents.length === 0 ? (
              <p className="text-sm text-slate-500 dark:text-slate-400 py-8 text-center">
                No documents uploaded yet
              </p>
            ) : (
              <div className="space-y-2">
                {documents.map((doc) => (
                  <div
                    key={doc.id}
                    className="flex items-center justify-between p-3 bg-slate-50 dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700"
                  >
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <File className="h-5 w-5 text-blue-600 flex-shrink-0" />
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-sm truncate">{doc.filename}</p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          {doc.chunks ? `${doc.chunks} chunks` : doc.size ? formatFileSize(doc.size) : 'Unknown size'}
                          {doc.uploadedAt && ` • ${formatDate(doc.uploadedAt)}`}
                        </p>
                      </div>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDelete(doc.id)}
                      className="text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Info Box */}
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription className="text-sm">
              <strong>Note:</strong> After uploading new documents, it may take a few minutes for
              the chatbot to process and index the content. The chatbot will automatically use the
              new information once processing is complete.
            </AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    </div>
  )
}
