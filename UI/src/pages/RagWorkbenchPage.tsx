import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import AsyncState from '@/components/common/AsyncState'
import { ragApi } from '@/api/rag'
import type { RagRequest, RetrievalResult } from '@/types/rag'

export default function RagWorkbenchPage() {
  const [query, setQuery] = useState('')

  const queryMutation = useMutation({
    mutationFn: (payload: RagRequest) => ragApi.query(payload),
    onError: (err: Error) => {
      alert(err.message)
    }
  })

  const docs = queryMutation.data?.data.retrievedDocuments ?? []

  const handleSearch = () => {
    if (!query.trim()) return
    queryMutation.mutate({ query, topK: 5, hybridSearch: true, rerankEnabled: true })
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800 dark:text-white">RAG 工作台</h1>

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
        <div className="space-y-4">
          <textarea
            rows={4}
            placeholder="请输入问题"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                       focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
          />

          <button
            onClick={handleSearch}
            disabled={queryMutation.isPending || !query.trim()}
            className="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600
                       disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {queryMutation.isPending ? '检索中...' : '执行检索'}
          </button>
        </div>

        {/* Results */}
        <div className="mt-6">
          {queryMutation.isPending || queryMutation.isError || (queryMutation.isSuccess && docs.length === 0) ? (
            <AsyncState
              loading={queryMutation.isPending}
              error={queryMutation.isError ? (queryMutation.error as Error).message : null}
              empty={queryMutation.isSuccess && docs.length === 0}
              emptyDescription="未检索到相关文档"
            />
          ) : null}

          {docs.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-medium text-gray-800 dark:text-white">
                检索结果 ({docs.length})
              </h3>
              <div className="space-y-3">
                {docs.map((item: RetrievalResult, index: number) => (
                  <div
                    key={index}
                    className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg border border-gray-200 dark:border-gray-600"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                        文档 {item.documentId}
                      </span>
                      <span className="text-sm text-blue-500">
                        评分: {item.rerankScore.toFixed(4)}
                      </span>
                    </div>
                    <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-3">
                      {item.content}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
