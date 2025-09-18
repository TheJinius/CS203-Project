import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

interface ResultsTabProps {
  calculationResult: number | null
}

export default function ResultsTab({ calculationResult }: ResultsTabProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Calculation Results</CardTitle>
      </CardHeader>
      <CardContent>
        {calculationResult !== null ? (
          <div className="space-y-2">
            <div className="flex justify-between">
              <span className="text-sm text-muted-foreground">Tariff Amount:</span>
              <span className="font-medium">${calculationResult.toFixed(2)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm text-muted-foreground">Rate Applied:</span>
              <span className="font-medium">15%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm text-muted-foreground">Status:</span>
              <span className="text-green-600 font-medium">Active</span>
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">
            No calculations yet. Use the Calculate tab to get started.
          </p>
        )}
      </CardContent>
    </Card>
  )
}