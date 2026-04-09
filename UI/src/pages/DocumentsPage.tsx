// 文档管理页面
import DocumentList from '@/components/documents/DocumentList'

export default function DocumentsPage() {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 dark:text-white">文档管理</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            管理知识库中的文档，支持上传、查看、删除等操作
          </p>
        </div>
      </div>

      <DocumentList />
    </div>
  )
}
