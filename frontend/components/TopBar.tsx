"use client"

import { useState, useEffect } from 'react'
import { User, ChevronDown, LogOut, Moon, Sun, AlertTriangle, Calculator, TrendingUp, FolderUp, GitCompare, Edit, Menu } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { FloatingDock } from '@/components/ui/floating-dock'
import { useAuth } from '@/contexts/AuthContext'
import { useTheme } from '@/contexts/ThemeContext'
import { useRouter, usePathname } from 'next/navigation'

interface TopBarProps {
  sidebarOpen: boolean
  onToggleSidebar: () => void
  activeTab?: string
  onTabChange?: (tab: string) => void
}

export default function TopBar({ sidebarOpen, onToggleSidebar, activeTab, onTabChange }: TopBarProps) {
  const [showUserMenu, setShowUserMenu] = useState(false)
  const { user, signOut, sessionError, isAdmin } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const router = useRouter()
  const pathname = usePathname()

  const handleSignOut = async () => {
    await signOut()
    setShowUserMenu(false)
  }

  // Show session error alert
  useEffect(() => {
    if (sessionError === "RefreshAccessTokenError") {
      console.error("Session expired, please sign in again");
    }
  }, [sessionError]);

  // Helper function to check if we're on the base route
  const isOnBaseRoute = () => pathname === '/'

  // Helper function to navigate to base route and then change tab
  const navigateToBaseAndChangeTab = (tab: string) => {
    if (!isOnBaseRoute()) {
      // Store the desired tab in localStorage
      localStorage.setItem('pendingTab', tab)
      // Navigate to base route
      router.push('/')
    } else {
      // Already on base route, just change tab
      if (!sidebarOpen) onToggleSidebar()
      if (onTabChange) onTabChange(tab)
    }
  }

  // Build dock items dynamically based on user role
  const dockItems = [
    {
      title: "Calculate Tariff",
      icon: <Calculator className="h-full w-full" />,
      onClick: () => navigateToBaseAndChangeTab("calculate"),
      isActive: pathname === '/' && activeTab === 'calculate'
    },
    // Admin-only: Manage Chatbot Documents tab
    ...(isAdmin() ? [{
      title: "Manage Chatbot Documents",
      icon: <FolderUp className="h-full w-full" />,
      onClick: () => navigateToBaseAndChangeTab("manage-docs"),
      isActive: pathname === '/' && activeTab === 'manage-docs'
    }] : []),
    // Admin-only: Tariff Management
    ...(isAdmin() ? [{
      title: "Tariff Management",
      icon: <Edit className="h-full w-full" />,
      onClick: () => router.push('/edit'),
      isActive: pathname === '/edit'
    }] : []),
    {
      title: "Results",
      icon: <TrendingUp className="h-full w-full" />,
      onClick: () => router.push('/compare'),
      isActive: pathname === '/compare'
    },
  ]

  return (
    <>
      <header className="h-20 border-b border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 flex items-center justify-center px-6 relative">
        {/* Left side - Logo and Burger Menu */}
        <div className="absolute left-6 flex items-center gap-3">
          {/* Burger Menu Button - Only show when sidebar is closed */}
          {!sidebarOpen && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onToggleSidebar}
              className="text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
            >
              <Menu className="h-5 w-5" />
            </Button>
          )}
        </div>

        {/* Center - Floating Dock Navigation */}
        <div className="flex justify-center">
          <FloatingDock items={dockItems} />
        </div>

        {/* Right side - Controls */}
        <div className="absolute right-6 flex items-center gap-2">
          {/* Session Error Indicator */}
          {sessionError && (
            <Alert variant="destructive" className="w-auto p-2">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription className="text-xs">
                Session expired
              </AlertDescription>
            </Alert>
          )}

          {/* Theme Toggle Button */}
          <Button
            variant="ghost"
            size="sm"
            onClick={toggleTheme}
            className="hover:bg-accent"
          >
            {theme === "light" ? (
              <Moon className="h-5 w-5" />
            ) : (
              <Sun className="h-5 w-5" />
            )}
          </Button>

          {/* User Profile Dropdown */}
          <div className="relative">
            <Button 
              onClick={() => setShowUserMenu(!showUserMenu)}
              variant="outline"
              size="sm"
              className="flex items-center gap-2"
            >
              <User className="h-4 w-4" />
              {user?.username || 'Profile'}
              <ChevronDown className="h-4 w-4" />
            </Button>

            {/* Dropdown Menu */}
            {showUserMenu && (
              <div className="absolute right-0 top-full mt-2 z-50">
                <Card className="w-80 shadow-lg">
                  <CardHeader>
                    <CardTitle className="text-sm">User Profile</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    {/* User Info */}
                    <div className="text-xs space-y-2">
                      <div>
                        <strong>Username:</strong> {user?.username || 'N/A'}
                      </div>
                      <div>
                        <strong>Email:</strong> {user?.email || 'N/A'}
                      </div>
                      <div>
                        <strong>Groups:</strong> {user?.groups?.join(', ') || 'None'}
                      </div>
                      
                      {/* Session Status */}
                      <div>
                        <strong>Session Status:</strong> 
                        {sessionError ? ` Error: ${sessionError}` : ' Active'}
                      </div>
                    </div>
                    
                    {/* Sign Out Button */}
                    <Button 
                      onClick={handleSignOut}
                      variant="destructive"
                      size="sm"
                      className="w-full"
                    >
                      <LogOut className="h-4 w-4 mr-2" />
                      Sign Out
                    </Button>
                  </CardContent>
                </Card>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Click outside to close menu */}
      {showUserMenu && (
        <div 
          className="fixed inset-0 z-40" 
          onClick={() => setShowUserMenu(false)} 
        />
      )}
    </>
  )
}