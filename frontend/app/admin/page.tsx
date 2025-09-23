// app/admin/page.tsx
'use client';
import { ProtectedRoute } from '../../components/ProtectedRoute';
import { useAuth } from '../../contexts/AuthContext';
import { Button } from '../../components/ui/button';
import { Card } from '../../components/ui/card';

export default function AdminPage() {
  const { user, signOut } = useAuth();

  const handleSignOut = async () => {
    await signOut();
  };

  return (
    <ProtectedRoute requireAdmin={true}>
      <div className="min-h-screen bg-gray-50 p-6">
        <div className="max-w-4xl mx-auto space-y-6">
          <div className="flex justify-between items-center">
            <h1 className="text-3xl font-bold text-gray-900">Admin Panel</h1>
            <div className="space-x-4">
              <Button onClick={() => window.location.href = '/dashboard'} variant="outline">
                Back to Dashboard
              </Button>
              <Button onClick={handleSignOut} variant="outline">
                Sign Out
              </Button>
            </div>
          </div>

          <Card className="p-6 border-red-200 bg-red-50">
            <h2 className="text-xl font-semibold mb-4 text-red-800">⚠️ Admin Access Only</h2>
            {user && (
              <div className="space-y-2 text-red-700">
                <p><strong>Admin User:</strong> {user.username}</p>
                <p><strong>Email:</strong> {user.email}</p>
                <p><strong>Groups:</strong> {user.groups.join(', ')}</p>
              </div>
            )}
          </Card>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <Card className="p-6">
              <h3 className="text-lg font-semibold mb-3">User Management</h3>
              <p className="text-gray-600 mb-4">Manage user accounts and permissions</p>
              <Button className="w-full" disabled>
                Manage Users
              </Button>
            </Card>

            <Card className="p-6">
              <h3 className="text-lg font-semibold mb-3">Data Management</h3>
              <p className="text-gray-600 mb-4">Update tariff and country data</p>
              <Button className="w-full" disabled>
                Manage Data
              </Button>
            </Card>

            <Card className="p-6">
              <h3 className="text-lg font-semibold mb-3">System Settings</h3>
              <p className="text-gray-600 mb-4">Configure system parameters</p>
              <Button className="w-full" disabled>
                Settings
              </Button>
            </Card>
          </div>

          <Card className="p-6">
            <h3 className="text-lg font-semibold mb-3">System Information</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <p className="font-medium">Total Users</p>
                <p className="text-2xl font-bold text-blue-600">--</p>
              </div>
              <div>
                <p className="font-medium">Active Sessions</p>
                <p className="text-2xl font-bold text-green-600">--</p>
              </div>
              <div>
                <p className="font-medium">API Calls Today</p>
                <p className="text-2xl font-bold text-purple-600">--</p>
              </div>
              <div>
                <p className="font-medium">System Status</p>
                <p className="text-2xl font-bold text-green-600">✓</p>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </ProtectedRoute>
  );
}