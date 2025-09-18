import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export default function CountriesTab() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Country Management</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <Label htmlFor="iso-code">ISO Code</Label>
          <Input id="iso-code" placeholder="e.g., SG" />
        </div>
        <div>
          <Label htmlFor="country-name">Country Name</Label>
          <Input id="country-name" placeholder="e.g., Singapore" />
        </div>
        <Button className="w-full">Add Country</Button>
      </CardContent>
    </Card>
  )
}