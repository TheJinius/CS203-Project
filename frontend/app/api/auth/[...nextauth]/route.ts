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
      // Persist the OAuth access_token and or the user id to the token right after signin
      if (account) {
        token.accessToken = account.access_token
        token.idToken = account.id_token
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
      
      return token
    },
    async session({ session, token }: any) {
      // Send properties to the client
      if (session.user) {
        session.user.groups = token.groups || []
        session.user.username = token.username
        session.user.email = token.email || session.user.email
      }
      session.accessToken = token.accessToken
      return session
    },
  },
  pages: {
    signIn: '/login',
  },
})

export { handler as GET, handler as POST }
