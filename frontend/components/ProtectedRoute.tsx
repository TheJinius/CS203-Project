// components/ProtectedRoute.tsx

//------------------------------USAGE--------------------------------
// // app/dashboard/page.tsx - Protected for all authenticated users
// 'use client';
// import { ProtectedRoute } from '../../components/ProtectedRoute';

// export default function Dashboard() {
//   return (
//     <ProtectedRoute>
//       <h1>User Dashboard</h1>
//       {/* Dashboard content */}
//     </ProtectedRoute>
//   );
// }

// // app/admin/page.tsx - Protected for Admins only
// 'use client';
// import { ProtectedRoute } from '../../components/ProtectedRoute';

// export default function AdminPanel() {
//   return (
//     <ProtectedRoute requireAdmin={true}>
//       <h1>Admin Panel</h1>
//       {/* Admin content */}
//     </ProtectedRoute>
//   );
// }
// Conditional rendering based on user role
// 'use client';
// import { useAuth } from '../../contexts/AuthContext';

// export default function SomeComponent() {
//   const { isAdmin, user } = useAuth();
  
//   return (
//     <div>
//       <h1>Welcome, {user?.username}</h1>
//       {isAdmin() && (
//         <button>Admin Only Action</button>
//       )}
//     </div>
//   );
// }
//-------------------------------------------------------------------
'use client';
import { useAuth } from '../contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect, ReactNode } from 'react';

interface ProtectedRouteProps {
  children: ReactNode;
  requireAdmin?: boolean;
}

export function ProtectedRoute({ children, requireAdmin = false }: ProtectedRouteProps) {
  const { isAuthenticated, isAdmin, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated()) {
        router.push('/api/auth/signin');
        return;
      }
      
      if (requireAdmin && !isAdmin()) {
        router.push('/unauthorized');
        return;
      }
    }
  }, [isAuthenticated, isAdmin, isLoading, router, requireAdmin]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">Loading...</div>
      </div>
    );
  }

  if (!isAuthenticated() || (requireAdmin && !isAdmin())) {
    return null;
  }

  return <>{children}</>;
}