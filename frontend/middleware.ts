import { withAuth } from "next-auth/middleware"
import { NextResponse } from "next/server"

export default withAuth(
  function middleware(req) {
    const { pathname } = req.nextUrl
    const token = req.nextauth.token
    
    // // Debug logging (remove in production)
    // console.log('Middleware:', { pathname, hasToken: !!token, groups: token?.groups })
    
    // Admin route protection
    if (pathname.startsWith('/admin') || pathname.startsWith('/dashboard/admin')) {
      const userGroups = token?.groups as string[] || []
      
      if (!userGroups.includes('Admins')) {
        return NextResponse.redirect(new URL('/unauthorized', req.url))
      }
    }
    
    // You can add more role-based checks here
    // Example: Editor routes
    // if (pathname.startsWith('/editor')) {
    //   const userGroups = token?.groups as string[] || []
    //   if (!userGroups.includes('Editors') && !userGroups.includes('Admins')) {
    //     return NextResponse.redirect(new URL('/unauthorized', req.url))
    //   }
    // }
    
    return NextResponse.next()
  },
  {
    callbacks: {
      authorized: ({ token, req }) => {
        // Must have a token to access protected routes
        if (!token) return false
        
        // Optional: Add additional authorization logic here
        // For now, just check if token exists
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
    // Protect these routes
    '/dashboard/:path*', 
    '/admin/:path*', 
    '/profile/:path*',
    // Add any other protected routes
  ]
}