// RAG 工作台页面 - 支持检索测试和配置
import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import ReactMarkdown from 'react-markdown'
import { ragApi } from '@/api/rag'
import type { RagRequest, RetrievalResult } from '@/types/rag'

export default function RagWorkbenchPage() {
  const [query, setQuery] = useState('')
  const [response, setResponse] = useState<string>('')
  const [topK, setTopK] = useState(5)
  const [hybridSearch, setHybridSearch] = useState(true)
  const [rerankEnabled, setRerankEnabled] = useState(true)
  const [showConfig, setShowConfig] = useState(false)

  const queryMutation = useMutation({
    mutationFn: (payload: RagRequest) => ragApi.query(payload),
    onSuccess: (data) => {
      const docs = data.data?.retrievedDocuments || []
      if (docs.length > 0) {
        setResponse(generateResponseText(docs))
      } else {
        setResponse('未检索到相关文档')
      }
    },
    onError: (err: Error) => {
      setResponse(`错误: ${err.message}`)
    }
  })

  const docs = queryMutation.data?.data?.retrievedDocuments ?? []

  const generateResponseText = (documents: RetrievalResult[]): string => {
    let text = `## 检索结果\n\n共找到 ${documents.length} 条相关文档:\n\n`
    
    documents.forEach((doc, idx) => {
      text += `### ${idx + 1}. 文档 ${doc.documentId}\n`
      text += `- **相似度评分**: ${doc.rerankScore.toFixed(4)}\n`
      text += `- **检索类型**: ${doc.retrieverType}\n`
      text += `- **分块**: ${doc.chunkIndex + 1}/${doc.totalChunks}\n\n`
      text += `**内容预览:**\n${doc.content.slice(0, 200)}...\n\n`
      text += `---\n\n`
    })

    text += `## 性能统计\n\n`
    text += `- 向量检索耗时: ${queryMutation.data?.data?.vectorRetrievalTimeMs || 0}ms\n`
    text += `- 关键词检索耗时: ${queryMutation.data?.data?.keywordRetrievalTimeMs || 0}ms\n`
    if (queryMutation.data?.data?.rerankTimeMs) {
      text += `- 重排序耗时: ${queryMutation.data?.data?.rerankTimeMs}ms\n`
    }
    text += `- 总耗时: ${queryMutation.data?.data?.retrievalTimeMs || 0}ms\n`

    return text
  }

  const handleSearch = () => {
    if (!query.trim()) return
    queryMutation.mutate({ query, topK, hybridSearch, rerankEnabled })
  }

  const handleClear = () => {
    setQuery('')
    setResponse('')
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 dark:text-white">RAG 工作台</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            测试检索增强生成功能，查看检索结果和引用
          </p>
        </div>
        <button
          onClick={() => setShowConfig(!showConfig)}
          className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 flex items-center gap-2"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          配置
        </button>
      </div>

      {/* Config panel */}
      {showConfig && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">检索配置</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm text-gray-600 dark:text-gray-400 mb-1">Top-K 数量</label>
              <input
                type="number"
                value={topK}
                onChange={(e) => setTopK(Number(e.target.value))}
                min={1}
                max={20}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg 
                           bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              />
            </div>
            <div className="flex items-center gap-2 pt-6">
              <input
                type="checkbox"
                id="hybridSearch"
                checked={hybridSearch}
                onChange={(e) => setHybridSearch(e.target.checked)}
                className="w-4 h-4 text-blue-500 border-gray-300 rounded focus:ring-blue-500"
              />
              <label htmlFor="hybridSearch" className="text-sm text-gray-700 dark:text-gray-300">
                混合检索（向量 + 关键词）
              </label>
            </div>
            <div className="flex items-center gap-2 pt-6">
              <input
                type="checkbox"
                id="rerankEnabled"
                checked={rerankEnabled}
                onChange={(e) => setRerankEnabled(e.target.checked)}
                className="w-4 h-4 text-blue-500 border-gray-300 rounded focus:ring-blue-500"
              />
              <label htmlFor="rerankEnabled" className="text-sm text-gray-700 dark:text-gray-300">
                启用重排序
              </label>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Query panel */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">查询输入</h3>
          <textarea
            rows={6}
            placeholder="请输入查询问题..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                       focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
          />
          <div className="mt-4 flex gap-2">
            <button
              onClick={handleSearch}
              disabled={queryMutation.isPending || !query.trim()}
              className="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600
                         disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
            >
              {queryMutation.isPending && (
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              )}
              {queryMutation.isPending ? '检索中...' : '执行检索'}
            </button>
            <button
              onClick={handleClear}
              className="px-6 py-2 border border-gray-300 dark:border-gray-600 rounded-lg 
                         text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              清空
            </button>
          </div>
        </div>

        {/* Response panel */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">检索结果</h3>
          
          {queryMutation.isPending ? (
            <div className="flex items-center justify-center py-12">
              <div className="text-center">
                <div className="w-10 h-10 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto" />
                <p className="mt-3 text-gray-500 dark:text-gray-400">正在检索...</p>
              </div>
            </div>
          ) : queryMutation.isError ? (
            <div className="p-4 bg-red-50 dark:bg-red-900/20 rounded-lg">
              <p className="text-red-600 dark:text-red-400">{queryMutation.error?.message}</p>
            </div>
          ) : !response ? (
            <div className="flex items-center justify-center py-12 text-gray-400 dark:text-gray-500">
              <div className="text-center">
                <svg className="w-12 h-12 mx-auto mb-3 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <p>输入查询开始检索</p>
              </div>
            </div>
          ) : (
            <div className="prose prose-sm dark:prose-invert max-w-none max-h-[400px] overflow-auto">
              <ReactMarkdown>{response}</ReactMarkdown>
            </div>
          )}
        </div>
      </div>

      {/* Documents list */}
      {docs.length > 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">
            详细文档列表 ({docs.length})
          </h3>
          <div className="space-y-3">
            {docs.map((item: RetrievalResult, index: number) => (
              <div
                key={index}
                className="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg border border-gray-200 dark:border-gray-600"
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-1 bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 text-xs rounded">
                      #{index + 1}
                    </span>
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                      文档 {item.documentId}
                    </span>
                    <span className={`px-2 py-0.5 text-xs rounded ${
                      item.retrieverType === 'hybrid' 
                        ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400'
                        : item.retrieverType === 'vector'
                        ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                        : 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'
                    }`}>
                      {item.retrieverType}
                    </span>
                  </div>
                  <div className="flex items-center gap-3 text-sm">
                    <span className="text-gray-500 dark:text-gray-400">
                      原始: {item.rawScore.toFixed(4)}
                    </span>
                    <span className="text-blue-500 font-medium">
                      重排: {item.rerankScore.toFixed(4)}
                    </span>
                    {item.passedThreshold && (
                      <span className="px-2 py-0.5 bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400 text-xs rounded">
                        通过阈值
                      </span>
                    )}
                  </div>
                </div>
                <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-4">
                  {item.content}
                </p>
                {item.metadata && Object.keys(item.metadata).length > 0 && (
                  <div className="mt-2 pt-2 border-t border-gray-200 dark:border-gray-600">
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      元数据: {JSON.stringify(item.metadata)}
                    </p>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
