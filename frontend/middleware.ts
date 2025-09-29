import { withAuth } from "next-auth/middleware"
import { NextResponse } from "next/server"

export default withAuth(
  function middleware(req) {
    const { pathname } = req.nextUrl
    const token = req.nextauth.token

    // Protect /admin routes: only Admins
    if (pathname.startsWith('/admin')) {
      const userGroups = token?.groups as string[] || []
      console.log(userGroups)
      if (!userGroups.includes('Admins')) {
        return NextResponse.redirect(new URL('/unauthorized', req.url))
      }
    }

    // All other protected routes just need authentication
    // (handled by the authorized callback below)
    return NextResponse.next()
  },
  {
    callbacks: {
      authorized: ({ token, req }) => {
        const { pathname } = req.nextUrl
        
        // For all protected routes, require a valid token
        if (!token) {
          return false // This will redirect to sign-in page
        }
        
        return true
      }
    },
    pages: {
      signIn: '/login',
    }
  }
)

export const config = {
  matcher: [
    '/',                // Protect base route - MUST be authenticated
    '/dashboard/:path*',
    '/admin/:path*',
    '/profile/:path*',
  ]
}