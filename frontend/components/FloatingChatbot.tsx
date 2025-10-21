"use client"

import { useEffect, useRef, useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Send, Bot, User, Loader2, X, MessageCircle } from "lucide-react"

interface Message {
  role: "user" | "assistant"
  content: string
  sources?: Array<{
    source_document: string
    line_range: string
    text: string
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
  const [hoveredSource, setHoveredSource] = useState<number | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const hoverTimeoutRef = useRef<NodeJS.Timeout | null>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!input.trim() || loading) return

    const userMessage = input.trim()
    setInput("")
    setError("")
    setMessages((prev) => [...prev, { role: "user", content: userMessage }])
    setLoading(true)

    try {
      const response = await fetch("http://127.0.0.1:8000/query", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: userMessage }),
      })

      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)

      const data = await response.json()
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: data.response, sources: data.sources },
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

  // Calculate total source text length for the hovered message
  const getSourcesLength = (index: number) => {
    const message = messages[index]
    if (!message?.sources) return 0
    return message.sources.reduce((acc, source) => acc + source.text.length, 0)
  }

  // Determine if we should expand the chatbot based on sources
  const shouldExpand = hoveredSource !== null && getSourcesLength(hoveredSource) > 500

  const handleMouseEnterSource = (index: number) => {
    if (hoverTimeoutRef.current) {
      clearTimeout(hoverTimeoutRef.current)
    }
    setHoveredSource(index)
  }

  const handleMouseLeaveSource = () => {
    if (hoverTimeoutRef.current) {
      clearTimeout(hoverTimeoutRef.current)
    }
    hoverTimeoutRef.current = setTimeout(() => {
      setHoveredSource(null)
    }, 150)
  }

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
          className={`fixed bottom-6 right-6 shadow-2xl flex flex-col z-50 animate-in slide-in-from-bottom-4 transition-all duration-300 ${
            shouldExpand ? "w-[48rem] h-[700px]" : "w-96 h-[600px]"
          }`}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4 border-b">
            <CardTitle className="flex items-center gap-2 text-lg">
              <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center">
                <Bot className="h-5 w-5 text-white" />
              </div>
              Compliance Assistant
            </CardTitle>
            <Button variant="ghost" size="icon" onClick={() => setIsOpen(false)} className="h-8 w-8">
              <X className="h-4 w-4" />
            </Button>
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
                    {message.role === "assistant" && message.sources && message.sources.length > 0 && (
                      <div
                        className="flex justify-end mt-2 relative"
                        onMouseEnter={() => handleMouseEnterSource(index)}
                        onMouseLeave={handleMouseLeaveSource}
                      >
                        <button
                          className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded-md hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors cursor-pointer"
                          aria-label="Show sources"
                        >
                          Sources · {message.sources.length}
                        </button>

                        {hoveredSource === index && (
                          <div
                            className={`absolute top-full right-0 mt-2 bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-lg shadow-xl p-4 z-[60] ${
                              shouldExpand ? "w-[42rem] max-h-[32rem]" : "w-[28rem] max-h-[22rem]"
                            } overflow-auto`}
                            onMouseEnter={() => handleMouseEnterSource(index)}
                            onMouseLeave={handleMouseLeaveSource}
                          >
                            <div className="space-y-3">
                              {message.sources.map((source, idx) => (
                                <div
                                  key={idx}
                                  className="p-3 bg-gray-50 dark:bg-gray-800 rounded border border-gray-200 dark:border-gray-700"
                                >
                                  <div className="flex items-start gap-2 mb-2">
                                    <span className="flex-shrink-0 w-6 h-6 bg-blue-600 text-white rounded-full flex items-center justify-center text-xs font-semibold">
                                      {idx + 1}
                                    </span>
                                    <div className="flex-1 min-w-0">
                                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 break-all">
                                        {source.source_document}
                                      </p>
                                      {source.line_range && (
                                        <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                                          Lines: {source.line_range}
                                        </p>
                                      )}
                                    </div>
                                  </div>

                                  {/* FULL SOURCE TEXT */}
                                  <div className="mt-2 p-3 bg-white dark:bg-gray-900 rounded text-xs text-gray-700 dark:text-gray-300 whitespace-pre-wrap border-l-2 border-blue-500">
                                    {source.text}
                                  </div>
                                </div>
                              ))}
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