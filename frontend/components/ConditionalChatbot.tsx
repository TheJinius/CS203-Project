"use client"

import { useSession } from "next-auth/react"
import { usePathname } from "next/navigation"
import FloatingChatbot from "./FloatingChatbot"

export default function ConditionalChatbot() {
  const { data: session, status } = useSession()
  const pathname = usePathname()

  // Don't show chatbot on login or unauthorized pages
  const isAuthPage = pathname === "/login" || pathname === "/unauthorized"

  // Only show chatbot if user is logged in and not on auth pages
  if (status === "loading" || !session || isAuthPage) {
    return null
  }

  return <FloatingChatbot />
}