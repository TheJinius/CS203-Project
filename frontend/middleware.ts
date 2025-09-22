import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  // Define protected routes
  const adminRoutes = ['/admin', '/dashboard/admin'];
  const protectedRoutes = ['/dashboard', '/profile', ...adminRoutes];
  
  const { pathname } = request.nextUrl;
  
  // Check if the route requires protection
  const isProtectedRoute = protectedRoutes.some(route => 
    pathname.startsWith(route)
  );
  
  if (isProtectedRoute) {
    // In a real implementation, you'd validate the JWT token here
    // For now, we'll rely on client-side protection
    const token = request.cookies.get('cognito-session');
    
    if (!token) {
      return NextResponse.redirect(new URL('/login', request.url));
    }
  }
  
  return NextResponse.next();
}

export const config = {
  matcher: ['/dashboard/:path*', '/admin/:path*', '/profile/:path*']
};