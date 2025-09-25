"use client"

import { useState } from 'react'
import { Menu, User, ChevronDown, LogOut, Moon, Sun } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { useAuth } from '@/contexts/AuthContext'
import { useTheme } from '@/contexts/ThemeContext'

interface TopBarProps {
  sidebarOpen: boolean
  onToggleSidebar: () => void
}

export default function TopBar({ sidebarOpen, onToggleSidebar }: TopBarProps) {
  const [showUserMenu, setShowUserMenu] = useState(false)
  const { user, signOut } = useAuth()
  const { theme, toggleTheme } = useTheme()

  const handleSignOut = async () => {
    await signOut()
    setShowUserMenu(false)
  }

  return (
    <>
      <header className="h-16 border-b border-border bg-card flex items-center justify-between px-4">
        <div className="flex items-center">
          {!sidebarOpen && (
            <Button variant="ghost" size="sm" onClick={onToggleSidebar} className="mr-4">
              <Menu className="h-4 w-4" />
            </Button>
          )}
          <h2 className="text-lg font-semibold text-card-foreground">Global Trade Map</h2>
        </div>

        <div className="flex items-center gap-2">
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
                      
                      {/* Debug: Raw User Object */}
                      <div className="mt-3 p-2 bg-gray-50 rounded">
                        <strong>Debug - Raw User Object:</strong>
                        <pre className="text-xs mt-1 whitespace-pre-wrap overflow-auto max-h-32">
                          {JSON.stringify(user, null, 2)}
                        </pre>
                      </div>
                    </div>
                    
                    {/* Sign Out Button */}
                    <Button 
                      onClick={handleSignOut}
                      variant="destructive"
                      size="sm"
                      className="w-full flex items-center gap-2"
                    >
                      <LogOut className="h-4 w-4" />
                      Sign Out
                    </Button>
                  </CardContent>
                </Card>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Click outside to close dropdown */}
      {showUserMenu && (
        <div 
          className="fixed inset-0 z-40" 
          onClick={() => setShowUserMenu(false)}
        />
      )}
    </>
  )
}