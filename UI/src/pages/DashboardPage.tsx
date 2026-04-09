import { useQuery } from '@tanstack/react-query'
import AsyncState from '@/components/common/AsyncState'
import { ragApi } from '@/api/rag'
import { embeddingsApi } from '@/api/embeddings'
import { documentsApi } from '@/api/documents'

type ServiceStatus = {
  name: string
  status: string
}

async function fetchStatuses(): Promise<ServiceStatus[]> {
  const [rag, embedding, docs] = await Promise.allSettled([
    ragApi.health(),
    embeddingsApi.health(),
    documentsApi.health()
  ])

  return [
    { name: 'rag-service', status: rag.status === 'fulfilled' ? 'UP' : 'DOWN' },
    { name: 'embedding-service', status: embedding.status === 'fulfilled' ? 'UP' : 'DOWN' },
    { name: 'document-service', status: docs.status === 'fulfilled' ? 'UP' : 'DOWN' }
  ]
}

export default function DashboardPage() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['dashboard-status'],
    queryFn: fetchStatuses
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-800 dark:text-white">仪表盘</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {isLoading || isError || !data?.length ? (
          <div className="col-span-3">
            <AsyncState
              loading={isLoading}
              error={isError ? (error as Error).message : null}
              empty={!isLoading && !isError}
              emptyDescription="暂无服务状态"
            />
          </div>
        ) : (
          data.map((item) => (
            <div
              key={item.name}
              className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700"
            >
              <h3 className="text-lg font-medium text-gray-700 dark:text-gray-300 mb-2">
                {item.name}
              </h3>
              <div className="flex items-center gap-2">
                <span
                  className={`w-3 h-3 rounded-full ${item.status === 'UP' ? 'bg-green-500' : 'bg-red-500'}`}
                />
                <span className={`font-medium ${item.status === 'UP' ? 'text-green-500' : 'text-red-500'}`}>
                  {item.status === 'UP' ? '运行中' : '已停止'}
                </span>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Quick links */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
        <h2 className="text-lg font-medium text-gray-800 dark:text-white mb-4">快捷操作</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <a
            href="/documents"
            className="flex flex-col items-center p-4 rounded-lg bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
          >
            <svg className="w-8 h-8 text-blue-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <span className="text-sm text-gray-700 dark:text-gray-300">文档管理</span>
          </a>
          <a
            href="/query"
            className="flex flex-col items-center p-4 rounded-lg bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
          >
            <svg className="w-8 h-8 text-green-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <span className="text-sm text-gray-700 dark:text-gray-300">RAG对话</span>
          </a>
          <a
            href="/monitoring"
            className="flex flex-col items-center p-4 rounded-lg bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
          >
            <svg className="w-8 h-8 text-purple-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
            <span className="text-sm text-gray-700 dark:text-gray-300">监控面板</span>
          </a>
          <a
            href="/settings"
            className="flex flex-col items-center p-4 rounded-lg bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
          >
            <svg className="w-8 h-8 text-gray-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <span className="text-sm text-gray-700 dark:text-gray-300">系统设置</span>
          </a>
        </div>
      </div>
    </div>
  )
}
