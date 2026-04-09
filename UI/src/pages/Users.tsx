import { useState } from 'react'
import type { User, UserRole, UserStatus, UserQueryParams } from '@/types/user'

// 模拟数据
const mockUsers: User[] = [
  { id: '1', username: 'admin', email: 'admin@example.com', role: 'ADMIN', status: 'ACTIVE', tokenUsed: 456789, tokenLimit: 1000000, createTime: '2024-01-01 10:00:00', lastLoginTime: '2024-01-07 14:30:00' },
  { id: '2', username: 'user001', email: 'user001@example.com', role: 'USER', status: 'ACTIVE', tokenUsed: 125680, tokenLimit: 500000, createTime: '2024-01-03 09:00:00', lastLoginTime: '2024-01-07 12:00:00' },
  { id: '3', username: 'user002', email: 'user002@example.com', role: 'USER', status: 'ACTIVE', tokenUsed: 98765, tokenLimit: 500000, createTime: '2024-01-04 11:00:00', lastLoginTime: '2024-01-06 18:00:00' },
  { id: '4', username: 'viewer001', email: 'viewer001@example.com', role: 'VIEWER', status: 'INACTIVE', tokenUsed: 0, tokenLimit: 10000, createTime: '2024-01-05 14:00:00' },
  { id: '5', username: 'user003', email: 'user003@example.com', role: 'USER', status: 'BANNED', tokenUsed: 256789, tokenLimit: 500000, createTime: '2024-01-02 08:00:00', lastLoginTime: '2024-01-05 22:00:00' },
  { id: '6', username: 'user004', email: 'user004@example.com', role: 'USER', status: 'PENDING', tokenUsed: 0, tokenLimit: 100000, createTime: '2024-01-07 10:00:00' }
]

const roleConfig: Record<string, { color: string; text: string }> = {
  ADMIN: { color: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400', text: '管理员' },
  USER: { color: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400', text: '普通用户' },
  VIEWER: { color: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-400', text: '访客' }
}

const statusConfig: Record<string, { color: string; text: string }> = {
  ACTIVE: { color: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400', text: '正常' },
  INACTIVE: { color: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400', text: '未激活' },
  BANNED: { color: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400', text: '已封禁' },
  PENDING: { color: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400', text: '待审核' }
}

const formatToken = (token: number): string => {
  if (token >= 1000000) return `${(token / 1000000).toFixed(1)}M`
  if (token >= 1000) return `${(token / 1000).toFixed(1)}K`
  return token.toString()
}

const Users = () => {
  const [users] = useState<User[]>(mockUsers)
  const [params, setParams] = useState<UserQueryParams>({ pageNum: 1, pageSize: 10 })
  const [selectedRowKeys] = useState<string[]>([])

  const statsData = {
    total: users.length,
    active: users.filter(u => u.status === 'ACTIVE').length,
    banned: users.filter(u => u.status === 'BANNED').length,
    pending: users.filter(u => u.status === 'PENDING').length
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800 dark:text-white">用户管理</h1>
        <div className="flex gap-2">
          <span className="px-3 py-2 text-sm text-gray-500 dark:text-gray-400">已选择 {selectedRowKeys.length} 项</span>
          <button className="px-4 py-2 bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 rounded-lg hover:bg-red-200 dark:hover:bg-red-900/50" disabled={selectedRowKeys.length === 0}>
            批量删除
          </button>
          <button className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 flex items-center gap-2">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            添加用户
          </button>
          <button className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600">
            刷新
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">总用户数</p>
          <p className="text-2xl font-bold text-blue-500 mt-1">{statsData.total}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">活跃用户</p>
          <p className="text-2xl font-bold text-green-500 mt-1">{statsData.active}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">待审核</p>
          <p className="text-2xl font-bold text-yellow-500 mt-1">{statsData.pending}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">已封禁</p>
          <p className="text-2xl font-bold text-red-500 mt-1">{statsData.banned}</p>
        </div>
      </div>

      {/* Filter */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
        <div className="flex flex-wrap gap-3">
          <select
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            onChange={(e) => setParams({ ...params, role: e.target.value as UserRole })}
          >
            <option value="">全部角色</option>
            {Object.entries(roleConfig).map(([value, config]) => (
              <option key={value} value={value}>{config.text}</option>
            ))}
          </select>
          <select
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            onChange={(e) => setParams({ ...params, status: e.target.value as UserStatus })}
          >
            <option value="">全部状态</option>
            {Object.entries(statusConfig).map(([value, config]) => (
              <option key={value} value={value}>{config.text}</option>
            ))}
          </select>
          <input
            type="text"
            placeholder="搜索用户名/邮箱"
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white w-48"
          />
        </div>
      </div>

      {/* User list */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">用户</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">角色</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">状态</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-40">Token使用</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-28">手机</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-36">创建时间</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-36">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center text-white text-sm font-medium">
                        {user.username.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <p className="font-medium text-gray-800 dark:text-gray-200">{user.username}</p>
                        <p className="text-xs text-gray-500 dark:text-gray-400">{user.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded text-xs ${roleConfig[user.role]?.color}`}>
                      {roleConfig[user.role]?.text || user.role}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded text-xs ${statusConfig[user.status]?.color}`}>
                      {statusConfig[user.status]?.text || user.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <div className="w-24">
                      <p className="text-xs text-gray-600 dark:text-gray-400">{formatToken(user.tokenUsed)} / {formatToken(user.tokenLimit)}</p>
                      <div className="h-1.5 bg-gray-200 dark:bg-gray-600 rounded mt-1">
                        <div
                          className={`h-full rounded ${user.tokenUsed / user.tokenLimit > 0.8 ? 'bg-red-500' : 'bg-blue-500'}`}
                          style={{ width: `${Math.min((user.tokenUsed / user.tokenLimit) * 100, 100)}%` }}
                        />
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{user.phone || '-'}</td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{user.createTime?.split(' ')[0]}</td>
                  <td className="px-4 py-3 text-sm">
                    <div className="flex items-center gap-2">
                      <button className="text-blue-500 hover:text-blue-600">编辑</button>
                      <button className="text-red-500 hover:text-red-600">删除</button>
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

export default Users
