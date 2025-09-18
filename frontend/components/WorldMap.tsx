import { Globe } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"

export default function WorldMap() {
  return (
    <main className="flex-1 bg-gradient-to-br from-blue-50 to-blue-100 relative overflow-hidden">
      {/* Placeholder world map */}
      <div className="absolute inset-0 flex items-center justify-center">
        <div className="text-center space-y-4">
          <Globe className="h-24 w-24 text-blue-300 mx-auto" />
          <div>
            <h3 className="text-xl font-semibold text-gray-700 mb-2">Interactive World Map</h3>
            <p className="text-gray-500 max-w-md">
              This area will display an interactive world map showing trade routes, tariff zones, and calculation
              results. Click on countries to view tariff information and trade relationships.
            </p>
          </div>
        </div>
      </div>

      {/* Map overlay info */}
      <div className="absolute top-4 right-4 space-y-2">
        <Card className="w-64">
          <CardContent className="p-4">
            <div className="flex items-center gap-2 mb-2">
              <div className="w-3 h-3 bg-green-500 rounded-full"></div>
              <span className="text-sm">Active Trade Routes</span>
            </div>
            <div className="flex items-center gap-2 mb-2">
              <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
              <span className="text-sm">Pending Tariffs</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 bg-red-500 rounded-full"></div>
              <span className="text-sm">High Tariff Zones</span>
            </div>
          </CardContent>
        </Card>
      </div>
    </main>
  )
}