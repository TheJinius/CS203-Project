import { withAuth } from "next-auth/middleware"

export default withAuth(
  function middleware(req) {
    // Additional middleware logic can go here
    const { pathname } = req.nextUrl;
    const token = req.nextauth.token;

    // Check if user has required permissions for admin routes
    if (pathname.startsWith('/admin') || pathname.startsWith('/dashboard/admin')) {
      const userGroups = token?.groups as string[] || [];
      if (!userGroups.includes('Admins')) {
        return Response.redirect(new URL('/unauthorized', req.url));
      }
    }
  },
  {
    callbacks: {
      authorized: ({ token }) => !!token
    },
    pages: {
      signIn: '/login',
    }
  }
)

export const config = {
  matcher: ['/dashboard/:path*', '/admin/:path*', '/profile/:path*']
};