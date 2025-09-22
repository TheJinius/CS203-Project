import NextAuth from "next-auth"
import CognitoProvider from "next-auth/providers/cognito"

const handler = NextAuth({
  providers: [
    CognitoProvider({
      clientId: process.env.COGNITO_CLIENT_ID!,
      clientSecret: process.env.COGNITO_CLIENT_SECRET!,
      issuer: `https://${process.env.COGNITO_DOMAIN!}/oauth2`,
    }),
  ],
  session: { strategy: "jwt" },
})

export { handler as GET, handler as POST }
