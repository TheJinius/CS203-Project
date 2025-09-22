// contexts/AuthContext.tsx
'use client';
import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { AuthenticationDetails, CognitoUser, CognitoUserPool, CognitoUserSession } from 'amazon-cognito-identity-js';
import { cognitoConfig } from '../lib/cognito-config';

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
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const userPool = new CognitoUserPool({
  UserPoolId: cognitoConfig.userPoolId,
  ClientId: cognitoConfig.userPoolWebClientId,
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const getCurrentSession = (): Promise<CognitoUserSession> => {
    return new Promise((resolve, reject) => {
      const cognitoUser = userPool.getCurrentUser();
      if (!cognitoUser) {
        reject(new Error('No current user'));
        return;
      }

      cognitoUser.getSession((err: any, session: CognitoUserSession) => {
        if (err) {
          reject(err);
        } else {
          resolve(session);
        }
      });
    });
  };

  const extractUserGroups = (session: CognitoUserSession): string[] => {
    const payload = session.getAccessToken().payload;
    return payload['cognito:groups'] || [];
  };

  useEffect(() => {
    const checkAuth = async () => {
      try {
        const session = await getCurrentSession();
        const cognitoUser = userPool.getCurrentUser();
        
        if (cognitoUser && session.isValid()) {
          const groups = extractUserGroups(session);
          const payload = session.getAccessToken().payload;
          
          setUser({
            username: payload.username,
            groups,
            email: payload.email,
          });
        }
      } catch (error) {
        console.log('No valid session');
      } finally {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  const signIn = async (username: string, password: string) => {
    return new Promise<void>((resolve, reject) => {
      const authenticationDetails = new AuthenticationDetails({
        Username: username,
        Password: password,
      });

      const cognitoUser = new CognitoUser({
        Username: username,
        Pool: userPool,
      });

      cognitoUser.authenticateUser(authenticationDetails, {
        onSuccess: (session: CognitoUserSession) => {
          const groups = extractUserGroups(session);
          const payload = session.getAccessToken().payload;
          
          setUser({
            username: payload.username,
            groups,
            email: payload.email,
          });
          resolve();
        },
        onFailure: (err) => {
          reject(err);
        },
      });
    });
  };

  const signOut = async () => {
    const cognitoUser = userPool.getCurrentUser();
    if (cognitoUser) {
      cognitoUser.signOut();
    }
    setUser(null);
  };

  const isAdmin = () => user?.groups.includes('Admins') ?? false;
  const isAuthenticated = () => user !== null;

  return (
    <AuthContext.Provider value={{
      user,
      isLoading,
      signIn,
      signOut,
      isAdmin,
      isAuthenticated,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};