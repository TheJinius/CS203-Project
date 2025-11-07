import { ClipboardCheck, Loader2 } from "lucide-react"
import { ComplianceTask, COUNTRY_NAMES } from '../types'

interface ComplianceTabProps {
  complianceTasks: ComplianceTask[]
  complianceLoading: boolean
  selectedDestination: string
  getPriorityColor: (category: string) => string
}

export function ComplianceTab({
  complianceTasks,
  complianceLoading,
  selectedDestination,
  getPriorityColor
}: ComplianceTabProps) {
  return (
    <div className="p-3 bg-gradient-to-br from-green-50 to-emerald-50 dark:from-green-900/20 dark:to-emerald-900/20 rounded border border-green-200 dark:border-green-800">
      <div className="font-medium text-slate-900 dark:text-slate-100 mb-3 flex items-center gap-2">
        <ClipboardCheck className="h-4 w-4 text-green-600 dark:text-green-400" />
        Compliance Requirements for {COUNTRY_NAMES[selectedDestination] || selectedDestination}
        {complianceLoading && <Loader2 className="h-4 w-4 animate-spin text-green-600 dark:text-green-400" />}
      </div>
      
      {complianceLoading ? (
        <div className="flex items-center justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-green-600 dark:text-green-400" />
          <span className="ml-2 text-sm text-slate-600 dark:text-slate-400">
            Fetching compliance requirements...
          </span>
        </div>
      ) : complianceTasks.length > 0 ? (
        <div className="space-y-3">
          {complianceTasks.map((task, index) => (
            <div 
              key={index} 
              className="p-3 bg-white dark:bg-slate-800 rounded border border-green-100 dark:border-green-900"
            >
              <div className="flex items-start justify-between gap-2 mb-2">
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium text-slate-900 dark:text-slate-100">
                    {task.task_name}
                  </div>
                  <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">
                    {task.description}
                  </div>
                </div>
                <div className={`flex-shrink-0 px-2 py-1 rounded-full text-xs font-medium ${getPriorityColor(task.task_category)}`}>
                  {task.task_category}
                </div>
              </div>
              
              <div className="grid grid-cols-2 gap-2 text-xs mt-2">
                <div>
                  <span className="text-slate-500 dark:text-slate-400">Agency:</span>
                  <span className="ml-1 text-slate-700 dark:text-slate-300">{task.responsible_agency}</span>
                </div>
                <div>
                  <span className="text-slate-500 dark:text-slate-400">Timing:</span>
                  <span className="ml-1 text-slate-700 dark:text-slate-300">{task.timing}</span>
                </div>
                <div className="col-span-2">
                  <span className="text-slate-500 dark:text-slate-400">Requirement:</span>
                  <span className="ml-1 text-slate-700 dark:text-slate-300">{task.compliance_requirement}</span>
                </div>
                {task.reference && (
                  <div className="col-span-2">
                    <span className="text-slate-500 dark:text-slate-400">Reference:</span>
                    {task.reference_url ? (
                      <a 
                        href={task.reference_url} 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="ml-1 text-blue-600 dark:text-blue-400 hover:underline"
                      >
                        {task.reference}
                      </a>
                    ) : (
                      <span className="ml-1 text-slate-700 dark:text-slate-300">{task.reference}</span>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center py-6">
          <div className="text-sm text-slate-600 dark:text-slate-400">
            No specific compliance requirements found for this product and destination.
          </div>
          <div className="text-xs text-slate-500 dark:text-slate-500 mt-1">
            This could mean standard import procedures apply, or the compliance database may not have specific rules for this combination.
          </div>
        </div>
      )}
    </div>
  )
}
