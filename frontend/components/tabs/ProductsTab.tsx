import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

export default function ProductsTab() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Product Management</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <Label htmlFor="hs-code">HS Code</Label>
          <Input id="hs-code" placeholder="e.g., VA123" />
        </div>
        <div>
          <Label htmlFor="product-name">Product Name</Label>
          <Input id="product-name" placeholder="e.g., Aluminium plates" />
        </div>
        <div>
          <Label htmlFor="category">Category</Label>
          <Select>
            <SelectTrigger>
              <SelectValue placeholder="Select category" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="metals">Metals</SelectItem>
              <SelectItem value="textiles">Textiles</SelectItem>
              <SelectItem value="electronics">Electronics</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <Button className="w-full">Add Product</Button>
      </CardContent>
    </Card>
  )
}