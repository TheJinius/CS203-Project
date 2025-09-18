import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

export default function TariffsTab() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Tariff Management</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <Label htmlFor="tariff-name">Tariff Name</Label>
          <Input id="tariff-name" placeholder="e.g., US Tariff 2025" />
        </div>
        <div>
          <Label htmlFor="tariff-code">Tariff Code</Label>
          <Input id="tariff-code" placeholder="e.g., TAR2201" />
        </div>
        <div>
          <Label htmlFor="tariff-type">Tariff Type</Label>
          <Select>
            <SelectTrigger>
              <SelectValue placeholder="Select type" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ad-valorem">Ad Valorem</SelectItem>
              <SelectItem value="compound">Compound</SelectItem>
              <SelectItem value="reciprocal">Reciprocal</SelectItem>
              <SelectItem value="specific">Specific</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div>
          <Label htmlFor="rate-value">Rate Value</Label>
          <Input id="rate-value" type="number" step="0.01" placeholder="e.g., 0.15" />
        </div>
        <Button className="w-full">Add Tariff</Button>
      </CardContent>
    </Card>
  )
}