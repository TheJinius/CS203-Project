"use client"

import { AlertCircle } from "lucide-react"

export default function OtherDutyForm() {
  return (
    <div className="space-y-3">
      <div className="text-xs font-semibold text-amber-700 dark:text-amber-300 uppercase tracking-wide mb-2">
        ⚠️ Other Duty Type
      </div>
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
      
      {/* Info Box */}
      <div className="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
        <p className="text-xs font-semibold text-blue-900 dark:text-blue-100 mb-1">
          💡 About Other Duty Types
        </p>
        <p className="text-xs text-blue-700 dark:text-blue-300">
          Other duty types may include special arrangements, exemptions, or complex duty structures that require manual calculation. Contact your administrator for more information about this specific tariff.
        </p>
      </div>
    </div>
  )
}
