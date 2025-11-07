import { CheckCircle, XCircle, AlertTriangle } from "lucide-react"

interface StatusMessagesProps {
  error: string
  success: string
  complianceError: string
}

export function StatusMessages({ error, success, complianceError }: StatusMessagesProps) {
  return (
    <>
      {(error || success) && (
        <div className={`flex items-start gap-2 p-3 rounded-lg text-sm font-medium ${success
            ? 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300 border border-green-200 dark:border-green-800'
            : 'bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 border border-red-200 dark:border-red-800'
          }`}>
          {success ? (
            <CheckCircle className="h-4 w-4 mt-0.5 text-green-600 dark:text-green-400 flex-shrink-0" />
          ) : (
            <XCircle className="h-4 w-4 mt-0.5 text-red-600 dark:text-red-400 flex-shrink-0" />
          )}
          <span className="flex-1 break-words">{success || error}</span>
        </div>
      )}

      {complianceError && (
        <div className="flex items-start gap-2 p-3 rounded-lg text-sm font-medium bg-orange-50 dark:bg-orange-900/20 text-orange-700 dark:text-orange-300 border border-orange-200 dark:border-orange-800">
          <AlertTriangle className="h-4 w-4 mt-0.5 text-orange-600 dark:text-orange-400 flex-shrink-0" />
          <span className="flex-1 break-words">{complianceError}</span>
        </div>
      )}
    </>
  )
}
