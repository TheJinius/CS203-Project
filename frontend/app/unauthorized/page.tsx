// app/unauthorized/page.tsx
'use client';
import { useAuth } from '../../contexts/AuthContext';
import { Card } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { useRouter } from 'next/navigation';

export default function UnauthorizedPage() {
  const { user, signOut } = useAuth();
  const router = useRouter();

  const handleGoBack = () => {
    router.push('/');
  };

  const handleSignOut = async () => {
    await signOut();
    router.push('/login');
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <Card className="w-full max-w-md p-8 space-y-6 text-center">
        <div className="space-y-4">
          <h1 className="text-3xl font-bold text-red-600">Access Denied</h1>
          <p className="text-gray-600">
            You don't have permission to access this page.
          </p>
          {user && (
            <div className="text-sm text-gray-500">
              <p>Signed in as: <span className="font-medium">{user.username}</span></p>
              <p>Groups: <span className="font-medium">{user.groups.join(', ') || 'None'}</span></p>
            </div>
          )}
        </div>
        
        <div className="space-y-3">
          <Button 
            onClick={handleGoBack}
            className="w-full"
            variant="outline"
          >
            Go to Dashboard
          </Button>
          
          <Button 
            onClick={handleSignOut}
            className="w-full"
            variant="destructive"
          >
            Sign Out
          </Button>
        </div>
      </Card>
    </div>
  );
}