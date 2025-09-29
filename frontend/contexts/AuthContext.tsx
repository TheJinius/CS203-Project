// contexts/AuthContext.tsx
'use client';
import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { useSession, signIn as nextAuthSignIn, signOut as nextAuthSignOut, SessionProvider } from 'next-auth/react';

interface User {
  username: string;
  groups: string[];
  email?: string;
}

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  signIn: (username: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  isAdmin: () => boolean;
  isAuthenticated: () => boolean;
  sessionError?: string;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function AuthProviderInner({ children }: { children: ReactNode }) {
  const { data: session, status } = useSession();
  const [user, setUser] = useState<User | null>(null);

  const isLoading = status === 'loading';

  useEffect(() => {
    if (session?.user && !session.error) {
      setUser({
        username: session.user.username || session.user.email || '',
        groups: session.user.groups || [],
        email: session.user.email || '',
      });
    } else {
      setUser(null);
    }
  }, [session]);

  // Handle session errors (like token refresh failures)
  useEffect(() => {
    if (session?.error === "RefreshAccessTokenError") {
      // Automatically sign out if refresh token is invalid
      nextAuthSignOut();
    }
  }, [session?.error]);

  const signIn = async (username: string, password: string) => {
    // For NextAuth with Cognito, we need to redirect to the Cognito hosted UI
    // The username/password flow would require additional setup
    // For now, redirect to the NextAuth sign-in
    await nextAuthSignIn('cognito');
  };

  const signOut = async () => {
    await nextAuthSignOut();
    setUser(null);
  };

  const isAdmin = () => user?.groups.includes('Admins') ?? false;

  const isAuthenticated = () => user !== null && !session?.error;

  return (
    <AuthContext.Provider value={{
      user,
      isLoading,
      signIn,
      signOut,
      isAdmin,
      isAuthenticated,
      sessionError: session?.error,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function AuthProvider({ children }: { children: ReactNode }) {
  return (
    <SessionProvider>
      <AuthProviderInner>
        {children}
      </AuthProviderInner>
    </SessionProvider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};