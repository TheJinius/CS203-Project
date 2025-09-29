"use client"

import { useState } from 'react'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Eye, EyeOff, Bug } from "lucide-react"

export default function DebugAuth() {
  const [showDebug, setShowDebug] = useState(false)

  if (!showDebug) {
    return (
      <Button
        variant="outline"
        size="sm"
        onClick={() => setShowDebug(true)}
        className="fixed bottom-4 right-4 z-50"
      >
        <Bug className="h-4 w-4 mr-2" />
        Debug Auth
      </Button>
    )
  }

  return (
    <Card className="fixed bottom-4 right-4 z-50 w-96 max-h-96 overflow-auto">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm flex items-center justify-between">
          🔧 Debug Info
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setShowDebug(false)}
            className="h-6 w-6 p-0"
          >
            <EyeOff className="h-3 w-3" />
          </Button>
        </CardTitle>
      </CardHeader>
      <CardContent className="text-xs space-y-2">
        <div>
          <strong>Environment Variables:</strong>
          <pre className="bg-gray-100 dark:bg-gray-800 p-2 rounded mt-1 text-xs overflow-x-auto">
            {JSON.stringify({
              AWS_REGION: process.env.AWS_REGION || 'MISSING',
              COGNITO_USER_POOL_ID: process.env.COGNITO_USER_POOL_ID ? 'SET' : 'MISSING',
              COGNITO_CLIENT_ID: process.env.COGNITO_CLIENT_ID ? 'SET' : 'MISSING',
              NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'MISSING',
            }, null, 2)}
          </pre>
        </div>
        
        <div>
          <strong>Window Location:</strong>
          <pre className="bg-gray-100 dark:bg-gray-800 p-2 rounded mt-1 text-xs">
            {typeof window !== 'undefined' ? window.location.href : 'SSR'}
          </pre>
        </div>
      </CardContent>
    </Card>
  )
}