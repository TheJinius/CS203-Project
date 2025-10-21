import NextAuth from "next-auth"
import CognitoProvider from "next-auth/providers/cognito"
import { Account, User, Session } from "next-auth"
import { JWT } from "next-auth/jwt"

// Type definitions
interface ExtendedJWT extends JWT {
  accessToken?: string
  refreshToken?: string
  idToken?: string
  accessTokenExpires?: number
  groups?: string[]
  username?: string
  email?: string
  error?: string
}

interface CognitoTokenPayload {
  'cognito:groups'?: string[]
  'cognito:username'?: string
  email?: string
  preferred_username?: string
}

interface CognitoRefreshResponse {
  access_token: string
  id_token: string
  expires_in: number
  refresh_token?: string
}

interface JWTCallbackParams {
  token: JWT
  account?: Account | null
  user?: User
}

interface SessionCallbackParams {
  session: Session
  token: JWT
}

// Helper function to extract user info from ID token
function extractUserInfoFromToken(idToken: string): Partial<ExtendedJWT> {
  try {
    const payload = JSON.parse(
      Buffer.from(idToken.split('.')[1], 'base64').toString()
    ) as CognitoTokenPayload
    
    return {
      groups: payload['cognito:groups'] || [],
      email: payload.email,
      username: payload['cognito:username'] || payload.preferred_username
    }
  } catch (error) {
    console.error('Error parsing ID token:', error)
    return {}
  }
}

// Helper function to check if token is expired
function isTokenExpired(accessTokenExpires?: number): boolean {
  return !accessTokenExpires || Date.now() >= accessTokenExpires
}

/**
 * Takes a token, and returns a new token with updated
 * `accessToken` and `accessTokenExpires`. If an error occurs,
 * returns the old token and an error property
 */
async function refreshAccessToken(token: ExtendedJWT): Promise<ExtendedJWT> {
  try {
    const url = `https://${process.env.COGNITO_DOMAIN}.auth.${process.env.AWS_REGION}.amazoncognito.com/oauth2/token`
    
    const response = await fetch(url, {
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      method: "POST",
      body: new URLSearchParams({
        client_id: process.env.COGNITO_CLIENT_ID!,
        client_secret: process.env.COGNITO_CLIENT_SECRET!,
        grant_type: "refresh_token",
        refresh_token: token.refreshToken || '',
      }),
    })

    const refreshedTokens = await response.json() as CognitoRefreshResponse

    if (!response.ok) {
      throw refreshedTokens
    }

    return {
      ...token,
      accessToken: refreshedTokens.access_token,
      idToken: refreshedTokens.id_token,
      accessTokenExpires: Date.now() + refreshedTokens.expires_in * 1000,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken, // Fall back to old refresh token
    }
  } catch (error) {
    console.error('Error refreshing access token:', error)

    return {
      ...token,
      error: "RefreshAccessTokenError",
    }
  }
}

const handler = NextAuth({
  providers: [
    CognitoProvider({
      clientId: process.env.COGNITO_CLIENT_ID!,
      clientSecret: process.env.COGNITO_CLIENT_SECRET!,
      issuer: `https://cognito-idp.${process.env.AWS_REGION}.amazonaws.com/${process.env.COGNITO_USER_POOL_ID}`,
    }),
  ],
  session: { strategy: "jwt" },
  callbacks: {
    async jwt({ token, account }: JWTCallbackParams): Promise<ExtendedJWT> {
      const extendedToken = token as ExtendedJWT
      
      // Initial sign in
      if (account) {
        extendedToken.accessToken = account.access_token
        extendedToken.refreshToken = account.refresh_token
        extendedToken.idToken = account.id_token
        extendedToken.accessTokenExpires = account.expires_at ? account.expires_at * 1000 : undefined // Convert to milliseconds
      }
      
      // Extract user groups from the token
      if (extendedToken.idToken) {
        const userInfo = extractUserInfoFromToken(extendedToken.idToken)
        Object.assign(extendedToken, userInfo)
      }
      
      // Return previous token if the access token has not expired yet
      if (!isTokenExpired(extendedToken.accessTokenExpires)) {
        return extendedToken
      }

      // Access token has expired, try to update it
      return refreshAccessToken(extendedToken)
    },
    
    async session({ session, token }: SessionCallbackParams): Promise<Session> {
      const extendedToken = token as ExtendedJWT
      
      // Add custom properties to session.user
      session.user = {
        ...session.user,
        groups: extendedToken.groups || [],
        username: extendedToken.username || '',
        email: extendedToken.email || session.user?.email || '',
      }
      
      // Optionally attach accessToken and error to session
      session.accessToken = extendedToken.accessToken
      session.error = extendedToken.error
      
      // Ensure 'expires' property is present (already provided by NextAuth)
      return session
    },
  },
  pages: {
    signIn: '/login',
  },
})

export { handler as GET, handler as POST }