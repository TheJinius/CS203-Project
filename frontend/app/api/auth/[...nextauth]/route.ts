import NextAuth from "next-auth"
import CognitoProvider from "next-auth/providers/cognito"
import { Account, User } from "next-auth"
import { JWT } from "next-auth/jwt"

interface CognitoUser {
  username?: string
  email?: string
  groups?: string[]
}

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

// interface ExtendedSession {
//   user?: CognitoUser
//   accessToken?: string
//   error?: string
// }

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
    async jwt({ token, account }: { token: JWT; account?: Account | null; user?: User }): Promise<ExtendedJWT> {
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
        try {
          const payload = JSON.parse(
            Buffer.from(extendedToken.idToken.split('.')[1], 'base64').toString()
          ) as {
            'cognito:groups'?: string[]
            'cognito:username'?: string
            email?: string
            preferred_username?: string
          }
          
          extendedToken.groups = payload['cognito:groups'] || []
          extendedToken.email = payload.email
          extendedToken.username = payload['cognito:username'] || payload.preferred_username
        } catch (error) {
          console.error('Error parsing ID token:', error)
        }
      }
      
      // Return previous token if the access token has not expired yet
      if (extendedToken.accessTokenExpires && Date.now() < extendedToken.accessTokenExpires) {
        return extendedToken
      }

      // Access token has expired, try to update it
      return refreshAccessToken(extendedToken)
    },
    async session({ session, token }: any) {
      const extendedToken = token as ExtendedJWT
      
      // Send properties to the client
      if (session.user) {
        session.user.groups = extendedToken.groups || []
        session.user.username = extendedToken.username || ''
        session.user.email = extendedToken.email || session.user.email
      }
      session.accessToken = extendedToken.accessToken
      session.error = extendedToken.error
      return session
    },
  },
  pages: {
    signIn: '/login',
  },
})

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

    const refreshedTokens = await response.json() as {
      access_token: string
      id_token: string
      expires_in: number
      refresh_token?: string
    }

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

export { handler as GET, handler as POST }