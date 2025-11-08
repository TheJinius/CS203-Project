'use client';

import { ProtectedRoute } from '../../components/ProtectedRoute';
import { useAuth } from '../../contexts/AuthContext';
import TariffManagementTab from '../../components/tabs/TariffManagementTab';
import TopBar from '../../components/TopBar';
import { Button } from '../../components/ui/button';
import { ArrowLeft, Home } from 'lucide-react';
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function EditTariffPage() {
  const { isAdmin } = useAuth();
  const router = useRouter();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Redirect non-admins
  useEffect(() => {
    if (!isAdmin()) {
      router.push('/unauthorized');
    }
  }, [isAdmin, router]);

  const onToggleSidebar = () => setSidebarOpen(prev => !prev);

  // Don't render anything if not admin (will redirect)
  if (!isAdmin()) {
    return null;
  }

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-slate-50 dark:bg-slate-900">
        <TopBar sidebarOpen={sidebarOpen} onToggleSidebar={onToggleSidebar} />
        
        <div className="container mx-auto px-4 py-6">
          <div className="mb-6">
            <div className="flex items-center gap-4 mb-4">
              <Button
                variant="outline"
                onClick={() => router.push('/')}
                className="flex items-center gap-2"
              >
                <ArrowLeft className="h-4 w-4" />
                Back to Home
              </Button>
              <Button
                variant="ghost"
                onClick={() => router.push('/admindashboard')}
                className="flex items-center gap-2 text-slate-600 dark:text-slate-400"
              >
                <Home className="h-4 w-4" />
                Admin Dashboard
              </Button>
            </div>
            
            <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-100">Tariff Management</h1>
            <p className="text-slate-600 dark:text-slate-400 mt-2">
              Search, edit, add, and manage all tariff entries (Admin Only)
            </p>
          </div>

          <div className="max-w-7xl">
            <TariffManagementTab />
          </div>
        </div>
      </div>
    </ProtectedRoute>
  );
}
