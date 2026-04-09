import { useNavigate } from 'react-router-dom'

export default function NotFoundPage() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center p-4">
      <div className="text-center">
        <div className="text-8xl font-bold text-gray-300 dark:text-gray-700 mb-4">404</div>
        <h1 className="text-2xl font-bold text-gray-800 dark:text-white mb-2">页面不存在</h1>
        <p className="text-gray-500 dark:text-gray-400 mb-8">
          抱歉，您访问的页面不存在或已被删除
        </p>
        <button
          onClick={() => navigate('/dashboard')}
          className="px-6 py-3 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
        >
          返回首页
        </button>
      </div>
    </div>
  )
}
