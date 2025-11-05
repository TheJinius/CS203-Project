"use client"

import { AlertCircle, FileText, Info } from "lucide-react"

interface OtherDutyFormProps {
  rawText?: string
  isComputable?: boolean
  currentValue?: string | number
}

export default function OtherDutyForm({ rawText, isComputable, currentValue }: OtherDutyFormProps) {
  // Analyze raw text to detect potential duty type
  const detectPotentialType = (text: string | undefined): string | null => {
    if (!text) return null
    
    const lowerText = text.toLowerCase()
    
    // Check for ad valorem (percentage)
    if (text.match(/\d+(\.\d+)?%/) || lowerText.includes('percent')) {
      return 'Ad Valorem'
    }
    
    // Check for specific duty (currency)
    if (text.match(/\$\d+/) || text.match(/¢\d+/) || lowerText.includes('per kg') || 
        lowerText.includes('per lb') || lowerText.includes('cents/')) {
      return 'Specific'
    }
    
    // Check for combined (both)
    if ((text.match(/\d+(\.\d+)?%/) && text.match(/\$\d+/)) ||
        (lowerText.includes('%') && (lowerText.includes('per') || lowerText.includes('$')))) {
      return 'Combined'
    }
    
    return null
  }

  const potentialType = detectPotentialType(rawText)

  return (
    <div className="space-y-3">
      <div className="text-xs font-semibold text-amber-700 dark:text-amber-300 uppercase tracking-wide mb-2">
        ⚠️ Other Duty Type
      </div>
      
      {/* Raw Text Display */}
      {rawText && (
        <div className="p-4 bg-gray-50 dark:bg-gray-900/40 border border-gray-300 dark:border-gray-700 rounded-lg">
          <div className="flex items-start gap-2 mb-2">
            <FileText className="h-4 w-4 text-gray-600 dark:text-gray-400 flex-shrink-0 mt-0.5" />
            <p className="text-xs font-semibold text-gray-900 dark:text-gray-100">
              Original Duty Text from CSV
            </p>
          </div>
          <div className="bg-white dark:bg-gray-800 p-3 rounded border border-gray-200 dark:border-gray-700">
            <p className="text-sm font-mono text-gray-800 dark:text-gray-200 break-words whitespace-pre-wrap">
              {rawText}
            </p>
          </div>
          {isComputable !== undefined && (
            <div className="mt-2 flex items-center gap-2">
              <Info className="h-3.5 w-3.5 text-blue-600 dark:text-blue-400" />
              <p className="text-xs text-gray-600 dark:text-gray-400">
                <span className="font-semibold">Computable:</span> {isComputable ? 'Yes' : 'No'}
              </p>
            </div>
          )}
        </div>
      )}
      
      {/* Reclassification Suggestion */}
      {potentialType && (
        <div className="p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
          <div className="flex items-start gap-2 mb-3">
            <AlertCircle className="h-5 w-5 text-green-600 dark:text-green-400 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-semibold text-green-900 dark:text-green-100 mb-2">
                ✨ Reclassification Recommended
              </p>
              <p className="text-xs text-green-800 dark:text-green-200 mb-3">
                This duty appears to be <strong>{potentialType}</strong> type based on the text pattern. 
                Consider deleting this tariff and recreating it as {potentialType} for better data quality.
              </p>
            </div>
          </div>
          
          <div className="bg-green-100 dark:bg-green-900/40 p-3 rounded-md">
            <p className="text-xs font-semibold text-green-900 dark:text-green-100 mb-2">
              Recommended Action:
            </p>
            <ol className="text-xs text-green-800 dark:text-green-200 ml-4 space-y-1.5 list-decimal">
              <li>Copy the product code and other details</li>
              <li>Delete this tariff</li>
              <li>Create a new tariff with duty type: <strong>{potentialType}</strong></li>
              <li>Extract and enter the duty rate from the text above</li>
            </ol>
          </div>
        </div>
      )}
      
      {/* Generic Info when no raw text */}
      {!rawText && (
        <div className="p-4 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg">
          <div className="flex items-start gap-2 mb-3">
            <AlertCircle className="h-5 w-5 text-yellow-600 dark:text-yellow-400 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-semibold text-yellow-900 dark:text-yellow-100 mb-2">
                Special Duty Type
              </p>
              <p className="text-xs text-yellow-800 dark:text-yellow-200 mb-3">
                This tariff has a special duty type that doesn't fall into standard categories (Ad Valorem, Specific, or Combined).
              </p>
            </div>
          </div>
          
          <div className="bg-yellow-100 dark:bg-yellow-900/40 p-3 rounded-md">
            <p className="text-xs font-semibold text-yellow-900 dark:text-yellow-100 mb-2">
              To convert to a standard duty type:
            </p>
            <ul className="text-xs text-yellow-800 dark:text-yellow-200 ml-4 space-y-1.5 list-disc">
              <li>
                <strong>For Ad Valorem:</strong> Add an Ad Valorem Rate (%) only
              </li>
              <li>
                <strong>For Specific:</strong> Add a Specific Rate + Unit only
              </li>
              <li>
                <strong>For Combined:</strong> Add both Ad Valorem and Specific components
              </li>
            </ul>
          </div>
        </div>
      )}
      
      {/* Info Box */}
      <div className="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
        <p className="text-xs font-semibold text-blue-900 dark:text-blue-100 mb-1">
          💡 About Other Duty Types
        </p>
        <p className="text-xs text-blue-700 dark:text-blue-300">
          Other duty types may include special arrangements, exemptions, or complex duty structures that require manual calculation. 
          {rawText 
            ? ' The original text from the CSV file is displayed above to help with reclassification.' 
            : ' Contact your administrator for more information about this specific tariff.'}
        </p>
      </div>
    </div>
  )
}
