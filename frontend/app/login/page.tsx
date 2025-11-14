// app/login/page.tsx
'use client';
import { useEffect } from 'react';
import { useSession, signIn } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { Button } from '../../components/ui/button';
import { Card } from '../../components/ui/card';

export default function LoginPage() {
  const { data: session, status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (session) {
      router.push('/');
    }
  }, [session, router]);

  const handleSignIn = async () => {
    await signIn('cognito', { callbackUrl: '/' });
  };

  if (status === 'loading') {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">Loading...</div>
      </div>
    );
  }

  if (session) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">Redirecting...</div>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-slate-900">
      <Card className="w-full max-w-md p-8 space-y-6 bg-white dark:bg-slate-800 border-gray-200 dark:border-slate-700">
        <div className="text-center">
          <h1 className="text-3xl font-bold font-bold text-blue-600 dark:text-blue-400">Sign In</h1>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            Access your tariff application dashboard
          </p>
        </div>
        
        <Button 
          onClick={handleSignIn}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white"
          size="lg"
        >
          Sign in with AWS Cognito
        </Button>
        
        <div className="text-center text-sm text-gray-500">
          <p>You will be redirected to AWS Cognito for authentication</p>
        </div>
      </Card>
    </div>
  );
}