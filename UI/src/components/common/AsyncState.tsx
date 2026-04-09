interface AsyncStateProps {
  loading?: boolean
  error?: string | null
  empty?: boolean
  emptyDescription?: string
}

// Spinner component
const Spinner = () => (
  <div className="flex justify-center items-center py-12">
    <div className="animate-spin rounded-full h-10 w-10 border-4 border-blue-500 border-t-transparent"></div>
  </div>
)

// Alert component
const Alert = ({ type, title, description }: { type: 'error' | 'warning' | 'info'; title: string; description?: string }) => {
  const colors = {
    error: 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800 text-red-800 dark:text-red-200',
    warning: 'bg-yellow-50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-800 text-yellow-800 dark:text-yellow-200',
    info: 'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800 text-blue-800 dark:text-blue-200',
  }
  const iconColors = {
    error: 'text-red-500',
    warning: 'text-yellow-500',
    info: 'text-blue-500',
  }

  return (
    <div className={`flex items-start gap-3 p-4 rounded-lg border ${colors[type]}`}>
      <svg className={`w-5 h-5 flex-shrink-0 mt-0.5 ${iconColors[type]}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <div>
        <h4 className="font-medium">{title}</h4>
        {description && <p className="mt-1 text-sm opacity-80">{description}</p>}
      </div>
    </div>
  )
}

// Empty state component
const Empty = ({ description }: { description: string }) => (
  <div className="flex flex-col items-center justify-center py-12 text-gray-500 dark:text-gray-400">
    <svg className="w-16 h-16 mb-4 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
    </svg>
    <p className="text-sm">{description}</p>
  </div>
)

export default function AsyncState({
  loading = false,
  error = null,
  empty = false,
  emptyDescription = '暂无数据'
}: AsyncStateProps) {
  if (loading) {
    return <Spinner />
  }

  if (error) {
    return <Alert type="error" title="请求失败" description={error} />
  }

  if (empty) {
    return <Empty description={emptyDescription} />
  }

  return null
}
