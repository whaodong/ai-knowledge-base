import { useState } from 'react'
import type { Collection, CollectionStats } from '@/types/vector'

// 模拟数据
const mockCollections: Collection[] = [
  { name: 'documents', dimension: 1536, indexType: 'HNSW', metricType: 'COSINE', vectorCount: 125680, totalChunks: 125680, usingField: 'embedding', status: 'READY', createTime: '2024-01-01 10:00:00', description: '文档向量集合' },
  { name: 'knowledge_base', dimension: 1536, indexType: 'IVF_FLAT', metricType: 'L2', vectorCount: 45678, totalChunks: 45678, usingField: 'text_embedding', status: 'READY', createTime: '2024-01-05 14:30:00', description: '知识库向量集合' },
  { name: 'faq_vectors', dimension: 768, indexType: 'FLAT', metricType: 'IP', vectorCount: 2345, totalChunks: 2345, usingField: 'faq_embedding', status: 'BUILDING', createTime: '2024-01-07 09:00:00', description: 'FAQ问答向量集合' }
]

const mockStats: CollectionStats = { totalCollections: 3, totalVectors: 173703, totalDimension: 3840 }

const Vectors = () => {
  const [collections] = useState<Collection[]>(mockCollections)
  const [stats] = useState<CollectionStats>(mockStats)
  const [searchKeyword, setSearchKeyword] = useState('')

  const filteredCollections = searchKeyword
    ? collections.filter(c => c.name.toLowerCase().includes(searchKeyword.toLowerCase()))
    : collections

  const handleViewDetail = (collection: Collection) => {
    alert(`查看 ${collection.name} 详情`)
  }

  const handleRebuildIndex = (collection: Collection) => {
    alert(`重建 ${collection.name} 索引`)
  }

  const handleDelete = (collection: Collection) => {
    if (confirm(`确定删除 Collection "${collection.name}" 吗？此操作不可恢复！`)) {
      alert(`删除 ${collection.name}`)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800 dark:text-white">向量管理</h1>
        <div className="flex gap-2">
          <input
            type="text"
            placeholder="搜索Collection"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white w-48"
          />
          <button className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600">
            刷新
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">Collection数量</p>
          <p className="text-2xl font-bold text-blue-500 mt-1">{stats.totalCollections}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">总向量数</p>
          <p className="text-2xl font-bold text-green-500 mt-1">{stats.totalVectors.toLocaleString()}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">总维度</p>
          <p className="text-2xl font-bold text-yellow-500 mt-1">{stats.totalDimension}</p>
        </div>
      </div>

      {/* Collection list */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Collection名称</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">维度</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">索引类型</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">度量类型</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-28">向量数量</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">状态</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-36">创建时间</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {filteredCollections.map((collection) => (
                <tr key={collection.name} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
                      </svg>
                      <span className="font-medium text-gray-800 dark:text-gray-200">{collection.name}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className="px-2 py-1 bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400 rounded text-xs">
                      {collection.dimension}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className="px-2 py-1 bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400 rounded text-xs">
                      {collection.indexType}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{collection.metricType}</td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{collection.vectorCount.toLocaleString()}</td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded text-xs ${collection.status === 'READY' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'}`}>
                      {collection.status === 'READY' ? '就绪' : '构建中'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{collection.createTime?.split(' ')[0]}</td>
                  <td className="px-4 py-3 text-sm">
                    <div className="flex items-center gap-2">
                      <button onClick={() => handleViewDetail(collection)} className="text-blue-500 hover:text-blue-600">详情</button>
                      <button onClick={() => handleRebuildIndex(collection)} className="text-blue-500 hover:text-blue-600">重建索引</button>
                      <button onClick={() => handleDelete(collection)} className="text-red-500 hover:text-red-600">删除</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

export default Vectors
