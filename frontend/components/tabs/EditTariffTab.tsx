import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { useRouter } from "next/navigation"

export default function EditTariffTab() {
  const router = useRouter();

  const handleGoToAdmin = () => {
    router.push("/admin");
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg text-center">To be implemented</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col items-center gap-4">
        <Button onClick={handleGoToAdmin}>
          Go to Admin Panel
        </Button>
        <p className="text-sm text-muted-foreground">
          TO BE IMPLEMENTED
        </p>
      </CardContent>
    </Card>
  )
}