"use client"
import { useSession } from "next-auth/react"
import { type ReactNode } from "react"

interface ProtectedRouteProps {
  children: ReactNode
  requireAdmin?: boolean
  fallback?: ReactNode
}

export function ProtectedRoute({ 
  children, 
  requireAdmin = false, 
  fallback 
}: ProtectedRouteProps) {
  const { data: session, status } = useSession()

  // Show loading state while session is loading
  if (status === "loading") {
    return (
      fallback || (
        <div className="flex items-center justify-center min-h-screen">
          <div className="animate-pulse">
            <div className="text-lg">Loading...</div>
          </div>
        </div>
      )
    )
  }

  // At this point, middleware has already handled redirects
  // If we're here and have a session, user is authorized
  if (session) {
    // Optional: Additional client-side admin check (though middleware should handle this)
    if (requireAdmin) {
      const userGroups = (session as any)?.groups || []
      if (!userGroups.includes('Admins')) {
        return (
          <div className="flex items-center justify-center min-h-screen">
            <div className="text-center">
              <h1 className="text-2xl font-bold text-red-600">Access Denied</h1>
              <p className="mt-2">You don't have permission to access this page.</p>
            </div>
          </div>
        )
      }
    }
    
    return <>{children}</>
  }

  // If no session, middleware should have redirected, but just in case:
  return null
}