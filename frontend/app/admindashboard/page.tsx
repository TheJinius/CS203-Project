// app/dashboard/page.tsx
'use client';
import { useState } from 'react';
import { ProtectedRoute } from '../../components/ProtectedRoute';
import { useAuth } from '../../contexts/AuthContext';
import { Button } from '../../components/ui/button';
import { Card } from '../../components/ui/card';
import TopBar from '../../components/TopBar';
export default function DashboardPage() {
  const { user, signOut, isAdmin } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleSignOut = async () => {
    await signOut();
  };

  const onToggleSidebar = () => setSidebarOpen(prev => !prev);

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-gray-50 p-6">
        <div className="max-w-4xl mx-auto space-y-6">
          <div className="flex justify-between items-center">
            <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
            <Button onClick={handleSignOut} variant="outline">
              Sign Out
            </Button>
          </div>
          <TopBar sidebarOpen={sidebarOpen} onToggleSidebar={onToggleSidebar} />

          <Card className="p-6">
            <h2 className="text-xl font-semibold mb-4">Welcome!</h2>
            {user && (
              <div className="space-y-2">
                <p><strong>Username:</strong> {user.username}</p>
                <p><strong>Email:</strong> {user.email}</p>
                <p><strong>Groups:</strong> {user.groups.join(', ') || 'None'}</p>
                <p><strong>Admin Access:</strong> {isAdmin() ? 'Yes' : 'No'}</p>
              </div>
            )}
          </Card>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Card className="p-6">
              <h3 className="text-lg font-semibold mb-3">User Features</h3>
              <ul className="space-y-2 text-gray-600">
                <li>• Calculate tariffs</li>
                <li>• View country data</li>
                <li>• Access product information</li>
                <li>• View results</li>
              </ul>
            </Card>

            {isAdmin() && (
              <Card className="p-6 border-blue-200 bg-blue-50">
                <h3 className="text-lg font-semibold mb-3 text-blue-800">Admin Features</h3>
                <ul className="space-y-2 text-blue-600">
                  <li>• Manage users</li>
                  <li>• Update tariff data</li>
                  <li>• System configuration</li>
                  <li>• Analytics dashboard</li>
                </ul>
                <div className="mt-4 space-y-2">
                  <Button className="w-full" onClick={() => window.location.href = '/admin'}>
                    Go to Admin Panel
                  </Button>
                  <Button className="w-full" variant="outline" onClick={() => window.location.href = '/edit'}>
                    Edit Tariffs
                  </Button>
                </div>
              </Card>
            )}
          </div>
        </div>
      </div>
    </ProtectedRoute>
  );
}