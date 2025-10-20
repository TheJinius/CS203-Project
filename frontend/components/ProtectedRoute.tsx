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
  // requireAdmin = false,
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

  // Log session for debugging (this uses the session variable)
  if (session) {
    console.log("User authenticated:", session.user?.email)
  }
  
  // At this point, middleware has already handled authentication and authorization
  // If we're here, the user should be authorized, so just render the children
  return <>{children}</>
}