import NextAuth from "next-auth"
import CognitoProvider from "next-auth/providers/cognito"

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
    async jwt({ token, account, profile }: any) {
      // Initial sign in
      if (account) {
        token.accessToken = account.access_token
        token.refreshToken = account.refresh_token
        token.idToken = account.id_token
        token.accessTokenExpires = account.expires_at * 1000 // Convert to milliseconds
      }
      
      // Extract user groups from the token
      if (token.idToken) {
        try {
          const payload = JSON.parse(Buffer.from((token.idToken as string).split('.')[1], 'base64').toString())
          token.groups = payload['cognito:groups'] || []
          token.email = payload.email
          token.username = payload['cognito:username'] || payload.preferred_username
        } catch (error) {
          console.error('Error parsing ID token:', error)
        }
      }
      
      // Return previous token if the access token has not expired yet
      if (Date.now() < token.accessTokenExpires) {
        return token
      }

      // Access token has expired, try to update it
      return refreshAccessToken(token)
    },
    async session({ session, token }: any) {
      // Send properties to the client
      if (session.user) {
        session.user.groups = token.groups || []
        session.user.username = token.username
        session.user.email = token.email || session.user.email
      }
      session.accessToken = token.accessToken
      session.error = token.error
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
async function refreshAccessToken(token: any) {
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
        refresh_token: token.refreshToken,
      }),
    })

    const refreshedTokens = await response.json()

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
    console.log(error)

    return {
      ...token,
      error: "RefreshAccessTokenError",
    }
  }
}

export { handler as GET, handler as POST }