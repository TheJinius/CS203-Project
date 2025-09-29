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
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <Card className="w-full max-w-md p-8 space-y-6">
        <div className="text-center">
          <h1 className="text-3xl font-bold font-bold text-gray-900 dark:text-[#33b5ff]">Sign In</h1>
          <p className="mt-2 text-gray-600">
            Access your tariff application dashboard
          </p>
        </div>
        
        <Button 
          onClick={handleSignIn}
          className="w-full"
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