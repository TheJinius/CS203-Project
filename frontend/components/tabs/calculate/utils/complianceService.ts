import { ComplianceTask, COUNTRY_NAMES } from '../types'

export async function fetchComplianceData(
  destination: string, 
  productDescription: string,
  setComplianceTasks: (tasks: ComplianceTask[]) => void,
  setComplianceError: (error: string) => void,
  setComplianceLoading: (loading: boolean) => void
) {
  setComplianceLoading(true)
  setComplianceError("")
  
  try {
    const endpoint = process.env.NEXT_PUBLIC_COMPLIANCE_API || "http://localhost:8001/query"
    const countryName = COUNTRY_NAMES[destination] || destination
    const query = `${countryName} ${productDescription}`
    
    const response = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ question: query }),
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const data = await response.json()
    
    let tasks: ComplianceTask[] = []
    try {
      let responseStr = data.response
      if (typeof responseStr === 'string') {
        responseStr = responseStr.replace(/```json\s*/g, '').replace(/```\s*/g, '').trim()
        
        const parsedResponse = JSON.parse(responseStr)
        if (Array.isArray(parsedResponse)) {
          tasks = parsedResponse
        } else {
          console.warn("Backend response is not an array:", parsedResponse)
        }
      }
    } catch (parseError) {
      console.error("Failed to parse compliance response:", parseError)
      console.error("Raw response:", data.response)
      setComplianceError("Failed to parse compliance data")
    }

    setComplianceTasks(tasks)
  } catch (err) {
    console.error("Compliance fetch error:", err)
    setComplianceError("Failed to fetch compliance data. Please make sure the Python backend is running on http://127.0.0.1:8001")
  } finally {
    setComplianceLoading(false)
  }
}
