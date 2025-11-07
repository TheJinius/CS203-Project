"use client"

import { useEffect, useRef, useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Send, Bot, User, Loader2, X, MessageCircle, Maximize2, Minimize2, ArrowUpLeft } from "lucide-react"

interface Message {
  role: "user" | "assistant"
  content: string
  sources?: Array<{
    source_document: string
    line_range: string
    text: string
    score?: number
    highlight_regions?: string[]  // Complete sentences/paragraphs to highlight
  }>
}

export default function FloatingChatbot() {
  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([
    {
      role: "assistant",
      content:
        "Hello! I'm your tariff compliance assistant. Ask me anything about tariff rules, HS classifications, or documentation requirements.",
    },
  ])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [activeSource, setActiveSource] = useState<number | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Resize state
  const [size, setSize] = useState({ width: 384, height: 600 }) // 384px = w-96
  const [isResizing, setIsResizing] = useState(false)
  const [isMaximized, setIsMaximized] = useState(false)
  const resizeRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  // Handle resize functionality
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isResizing || isMaximized) return

      const newWidth = window.innerWidth - e.clientX - 24 // 24px for right margin
      const newHeight = window.innerHeight - e.clientY - 24 // 24px for bottom margin

      setSize({
        width: Math.max(384, Math.min(newWidth, window.innerWidth - 48)),
        height: Math.max(400, Math.min(newHeight, window.innerHeight - 48)),
      })
    }

    const handleMouseUp = () => {
      setIsResizing(false)
    }

    if (isResizing) {
      document.addEventListener("mousemove", handleMouseMove)
      document.addEventListener("mouseup", handleMouseUp)
    }

    return () => {
      document.removeEventListener("mousemove", handleMouseMove)
      document.removeEventListener("mouseup", handleMouseUp)
    }
  }, [isResizing, isMaximized])

  const handleResizeStart = (e: React.MouseEvent) => {
    e.preventDefault()
    setIsResizing(true)
  }

  const toggleMaximize = () => {
    setIsMaximized(!isMaximized)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!input.trim() || loading) return

    const userMessage = input.trim()
    setInput("")
    setError("")
    setMessages((prev) => [...prev, { role: "user", content: userMessage }])
    setLoading(true)

    try {
      const response = await fetch("https://cs203chatbot.duckdns.org/query", {
      // const response = await fetch("http://localhost:8000/query", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: userMessage }),
      })

      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)

      const data = await response.json()
      
      // Only include sources if the response doesn't indicate lack of context
      const hasNoContext = data.response.toLowerCase().includes("does not contain") || 
                          data.response.toLowerCase().includes("no information") ||
                          data.response.toLowerCase().includes("cannot find") ||
                          data.response.toLowerCase().includes("insufficient")
      
      setMessages((prev) => [
        ...prev,
        { 
          role: "assistant", 
          content: data.response, 
          sources: hasNoContext ? undefined : data.sources 
        },
      ])
    } catch (err) {
      console.error("Chatbot error:", err)
      setError(
        "Failed to get response from chatbot. Please make sure the Python backend is running on http://127.0.0.1:8000"
      )
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: "Sorry, I encountered an error. Please try again." },
      ])
    } finally {
      setLoading(false)
    }
  }

  const handleMouseEnterSource = (index: number) => {
    // Toggle source visibility on click
    setActiveSource(activeSource === index ? null : index)
  }

  const handleMouseLeaveSource = () => {
    // No longer needed for click-based interaction
  }

  // Highlight complete sentences/regions in text
  const highlightText = (text: string, regions?: string[]) => {
    if (!regions || regions.length === 0) {
      return text
    }

    // Sort regions by length (longest first) to handle nested matches
    const sortedRegions = [...regions].sort((a, b) => b.length - a.length)
    
    // Find all region positions in the text
    const matches: Array<{start: number, end: number, text: string}> = []
    
    for (const region of sortedRegions) {
      const escapedRegion = region.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      const regex = new RegExp(escapedRegion, 'gi')
      let match
      
      while ((match = regex.exec(text)) !== null) {
        const start = match.index
        const end = match.index + match[0].length
        
        // Check if this overlaps with existing matches
        const overlaps = matches.some(m => 
          (start >= m.start && start < m.end) ||
          (end > m.start && end <= m.end) ||
          (start <= m.start && end >= m.end)
        )
        
        if (!overlaps) {
          matches.push({start, end, text: match[0]})
        }
      }
    }
    
    // Sort matches by position
    matches.sort((a, b) => a.start - b.start)
    
    // Build the result with highlighted regions
    const parts: (string | React.ReactElement)[] = []
    let lastIndex = 0
    let keyCounter = 0
    
    for (const match of matches) {
      // Add text before match
      if (match.start > lastIndex) {
        parts.push(text.substring(lastIndex, match.start))
      }
      
      // Add highlighted region
      parts.push(
        <mark 
          key={`highlight-${keyCounter++}`} 
          className="bg-yellow-200 dark:bg-yellow-600/80 text-gray-900 dark:text-gray-100 font-medium px-1.5 py-1 rounded leading-relaxed"
        >
          {match.text}
        </mark>
      )
      lastIndex = match.end
    }
    
    // Add remaining text
    if (lastIndex < text.length) {
      parts.push(text.substring(lastIndex))
    }
    
    return parts.length > 0 ? parts : text
  }

  // Calculate dynamic dimensions - respect user's resized dimensions
  const chatWidth = isMaximized ? window.innerWidth - 48 : size.width
  const chatHeight = isMaximized ? window.innerHeight - 48 : size.height

  return (
    <>
      {/* Floating Chat Button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="fixed bottom-6 right-6 w-14 h-14 bg-blue-600 hover:bg-blue-700 text-white rounded-full shadow-lg flex items-center justify-center transition-all duration-300 hover:scale-110 z-50"
          aria-label="Open chatbot"
        >
          <MessageCircle className="h-6 w-6" />
        </button>
      )}

      {/* Chat Window - Dynamic sizing, fixed right edge */}
      {isOpen && (
        <Card
          ref={resizeRef}
          className="fixed bottom-6 right-6 shadow-2xl flex flex-col z-50 animate-in slide-in-from-bottom-4 transition-all duration-300"
          style={{
            width: `${chatWidth}px`,
            height: `${chatHeight}px`,
          }}
        >
          {/* Resize handle - top-left corner */}
          {!isMaximized && (
            <div
              className="absolute -top-1 -left-1 w-6 h-6 cursor-nwse-resize opacity-30 hover:opacity-100 transition-opacity flex items-center justify-center"
              onMouseDown={handleResizeStart}
            >
              <ArrowUpLeft className="w-4 h-4 text-white" />
            </div>
          )}

          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4 border-b">
            <CardTitle className="flex items-center gap-2 text-lg">
              <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center">
                <Bot className="h-5 w-5 text-white" />
              </div>
              Compliance Assistant
            </CardTitle>
            <div className="flex items-center gap-1">
              <Button
                variant="ghost"
                size="icon"
                onClick={toggleMaximize}
                className="h-8 w-8"
                title={isMaximized ? "Restore" : "Maximize"}
              >
                {isMaximized ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
              </Button>
              <Button variant="ghost" size="icon" onClick={() => setIsOpen(false)} className="h-8 w-8">
                <X className="h-4 w-4" />
              </Button>
            </div>
          </CardHeader>

          <CardContent className="flex-1 flex flex-col overflow-hidden p-4">
            {error && (
              <div className="mb-4 p-3 bg-red-100 dark:bg-red-900/20 text-red-800 dark:text-red-200 rounded-md text-sm">
                {error}
              </div>
            )}

            {/* Messages Container */}
            <div className="flex-1 overflow-y-auto mb-4 space-y-4 pr-2">
              {messages.map((message, index) => (
                <div
                  key={index}
                  className={`flex gap-3 ${message.role === "user" ? "justify-end" : "justify-start"}`}
                >
                  {message.role === "assistant" && (
                    <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center">
                      <Bot className="h-4 w-4 text-white" />
                    </div>
                  )}

                  <div className="flex-1 flex flex-col">
                    <div
                      className={`${
                        message.role === "user"
                          ? "bg-blue-600 text-white self-end max-w-[75%]"
                          : "bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-gray-100 self-start"
                      } rounded-lg p-3`}
                    >
                      <div className="text-sm whitespace-pre-wrap">{message.content}</div>
                    </div>

                    {/* Sources button aligned to the right */}
                    {message.role === "assistant" && 
                     message.sources && 
                     message.sources.length > 0 && 
                     message.sources.some(source => source.text && source.text.trim().length > 0) && (
                      <div className="flex justify-end mt-2 relative">
                        <button
                          className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded-md hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors cursor-pointer"
                          aria-label="Show sources"
                          title="Click to show sources"
                          onClick={() => handleMouseEnterSource(index)}
                        >
                          Sources · {message.sources.filter(s => s.text && s.text.trim().length > 0).length}
                        </button>

                        {activeSource === index && (
                          <div
                            className="absolute top-full right-0 mt-2 bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-lg shadow-xl p-4 z-[60] overflow-auto transition-all duration-150"
                            style={{
                              width: `${Math.min(size.width - 32, size.width * 0.9)}px`,
                              maxHeight: `${Math.min(size.height * 0.6, 500)}px`
                            }}
                          >
                            <div className="space-y-3">
                              {message.sources.map((source, idx) => {
                                // Calculate relevance indicator - log for debugging
                                const score = source.score || 0
                                console.log(`Source ${idx + 1} score:`, score, 'Document:', source.source_document)
                                
                                // If no scores available, treat first source as most relevant
                                const isHighRelevance = score >= 0.70 || (score === 0 && idx === 0)
                                const isMediumRelevance = (score >= 0.55 && score < 0.70) || (score === 0 && idx === 1)
                                
                                return (
                                  <div
                                    key={idx}
                                    className={`p-3 rounded border-2 ${
                                      isHighRelevance 
                                        ? 'bg-blue-50 dark:bg-blue-950/50 border-blue-400 dark:border-blue-600' 
                                        : isMediumRelevance
                                        ? 'bg-yellow-50 dark:bg-yellow-950/30 border-yellow-400 dark:border-yellow-700'
                                        : 'bg-gray-50 dark:bg-gray-800 border-gray-300 dark:border-gray-600'
                                    }`}
                                  >
                                    <div className="flex items-start gap-2 mb-2">
                                      <span className={`flex-shrink-0 w-6 h-6 ${
                                        isHighRelevance ? 'bg-blue-600' : isMediumRelevance ? 'bg-yellow-600' : 'bg-gray-500'
                                      } text-white rounded-full flex items-center justify-center text-xs font-semibold`}>
                                        {idx + 1}
                                      </span>
                                      <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 flex-wrap">
                                          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 break-all">
                                            {source.source_document}
                                          </p>
                                          {isHighRelevance && (
                                            <span className="text-xs px-2 py-0.5 bg-blue-600 text-white rounded-full font-medium">
                                              ⭐ Most Relevant
                                            </span>
                                          )}
                                          {isMediumRelevance && (
                                            <span className="text-xs px-2 py-0.5 bg-yellow-600 text-white rounded-full font-medium">
                                              Relevant
                                            </span>
                                          )}
                                        </div>
                                        <p className="text-xs text-gray-600 dark:text-gray-400 mt-1 font-medium">
                                          Relevance: {score > 0 ? (score * 100).toFixed(1) + '%' : (idx === 0 ? 'Highest' : idx === 1 ? 'High' : 'Moderate')}
                                        </p>
                                      </div>
                                    </div>

                                    {/* FULL SOURCE TEXT */}
                                    <div className={`mt-2 p-3 bg-white dark:bg-gray-900 rounded text-xs text-gray-700 dark:text-gray-300 whitespace-pre-wrap ${
                                      isHighRelevance ? 'border-l-4 border-blue-600' : isMediumRelevance ? 'border-l-4 border-yellow-600' : 'border-l-2 border-gray-400'
                                    }`}>
                                      {highlightText(source.text, source.highlight_regions)}
                                    </div>
                                  </div>
                                )
                              })}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {message.role === "user" && (
                    <div className="flex-shrink-0 w-8 h-8 rounded-full bg-gray-500 flex items-center justify-center">
                      <User className="h-4 w-4 text-white" />
                    </div>
                  )}
                </div>
              ))}

              {loading && (
                <div className="flex gap-3 justify-start">
                  <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center">
                    <Bot className="h-4 w-4 text-white" />
                  </div>
                  <div className="bg-gray-100 dark:bg-gray-800 rounded-lg p-3">
                    <Loader2 className="h-5 w-5 animate-spin" />
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>

            {/* Input Form */}
            <form onSubmit={handleSubmit} className="flex gap-2 border-t pt-4">
              <Input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Ask about tariffs..."
                disabled={loading}
                className="flex-1"
              />
              <Button type="submit" disabled={loading || !input.trim()} size="icon">
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}
    </>
  )
}