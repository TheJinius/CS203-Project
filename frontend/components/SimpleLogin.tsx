"use client"

import { useState } from 'react'
import { signIn } from 'aws-amplify/auth'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Eye, EyeOff, Loader2, Mail, Lock, AlertTriangle } from "lucide-react"
import { useAuth } from "@/contexts/AuthContext"
import { isAmplifyConfigured } from '@/lib/cognito-config'

export default function SimpleLogin() {
  const { configurationError } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  const handleSignIn = async () => {
    if (!isAmplifyConfigured) {
      setError('Authentication service is not configured. Please check environment variables.')
      return
    }

    if (!email || !password) {
      setError('Please fill in all fields')
      return
    }

    setLoading(true)
    setError('')
    setSuccess('')

    try {
      console.log('🔐 Attempting sign in for:', email)
      
      const result = await signIn({
        username: email,
        password: password,
      })

      console.log('✅ Sign in result:', result)
      
      if (result.isSignedIn) {
        setSuccess('Successfully signed in! Redirecting...')
        setTimeout(() => {
          window.location.reload()
        }, 1000)
      } else {
        setError('Sign in incomplete. Please check your credentials.')
      }
    } catch (error: any) {
      console.error('❌ Sign in error:', error)
      
      if (error.name === 'NotAuthorizedException') {
        setError('Invalid email or password')
      } else if (error.name === 'UserNotFoundException') {
        setError('No account found with this email')
      } else if (error.name === 'UserNotConfirmedException') {
        setError('Please verify your email first')
      } else if (error.message?.includes('Auth UserPool not configured')) {
        setError('Authentication service not configured. Please check environment variables.')
      } else {
        setError(error.message || 'Sign in failed')
      }
    }
    
    setLoading(false)
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !loading && email && password && isAmplifyConfigured) {
      handleSignIn()
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold text-center">
            🌍 Tariff Calculator
          </CardTitle>
          <p className="text-center text-muted-foreground">
            Sign in to access the application
          </p>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Configuration Error Alert */}
          {(configurationError || !isAmplifyConfigured) && (
            <Alert variant="destructive">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>
                {configurationError || 'Authentication service not configured properly. Please check environment variables.'}
              </AlertDescription>
            </Alert>
          )}

          {/* Error Alert */}
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* Success Alert */}
          {success && (
            <Alert variant="success">
              <AlertDescription>{success}</AlertDescription>
            </Alert>
          )}

          {/* Email Field */}
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <div className="relative">
              <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                id="email"
                type="email"
                placeholder="your@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onKeyDown={handleKeyPress}
                className="pl-10"
                disabled={loading || !isAmplifyConfigured}
                autoComplete="email"
              />
            </div>
          </div>

          {/* Password Field */}
          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyDown={handleKeyPress}
                className="pl-10 pr-10"
                disabled={loading || !isAmplifyConfigured}
                autoComplete="current-password"
              />
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                onClick={() => setShowPassword(!showPassword)}
                disabled={loading || !isAmplifyConfigured}
                tabIndex={-1}
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </Button>
            </div>
          </div>

          {/* Sign In Button */}
          <Button 
            onClick={handleSignIn} 
            disabled={loading || !email || !password || !isAmplifyConfigured}
            className="w-full"
          >
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {loading ? 'Signing In...' : 'Sign In'}
          </Button>

          {/* Debug info for development */}
          {process.env.NODE_ENV === 'development' && (
            <div className="mt-4 p-3 bg-gray-100 dark:bg-gray-800 rounded text-xs">
              <strong>🔧 Debug Info:</strong>
              <pre className="mt-1 text-xs overflow-x-auto">
                {JSON.stringify({
                  amplifyConfigured: isAmplifyConfigured,
                  region: process.env.NEXT_PUBLIC_AWS_REGION || 'MISSING',
                  userPoolId: process.env.NEXT_PUBLIC_COGNITO_USER_POOL_ID ? 'SET' : 'MISSING',
                  clientId: process.env.NEXT_PUBLIC_COGNITO_CLIENT_ID ? 'SET' : 'MISSING',
                  apiUrl: process.env.NEXT_PUBLIC_API_URL || 'MISSING',
                  configurationError: configurationError || 'None'
                }, null, 2)}
              </pre>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}