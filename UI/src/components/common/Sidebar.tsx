import { useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, Badge } from 'antd'
import { 
  FileTextOutlined, 
  MessageOutlined, 
  DashboardOutlined, 
  SettingOutlined,
  LogoutOutlined,
  UserOutlined,
  BarChartOutlined,
  ApartmentOutlined,
  DatabaseOutlined,
  ThunderboltOutlined,
  TeamOutlined
} from '@ant-design/icons'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

const { Sider } = Layout

interface MenuItem {
  key: string
  icon: React.ReactNode
  label: string
  children?: MenuItem[]
}

const Sidebar = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuthStore()
  const [openKeys, setOpenKeys] = useState<string[]>([])

  // 判断是否为管理员
  const isAdmin = user?.role === 'ADMIN'

  // 主菜单项
  const mainMenuItems: MenuItem[] = [
    { key: '/documents', icon: <FileTextOutlined />, label: '文档管理' },
    { key: '/query', icon: <MessageOutlined />, label: 'RAG对话' },
    { key: '/monitoring', icon: <DashboardOutlined />, label: '监控面板' }
  ]

  // 监控与统计子菜单
  const statsSubMenu: MenuItem = {
    key: 'stats',
    icon: <BarChartOutlined />,
    label: '监控与统计',
    children: [
      { key: '/token-stats', icon: <ThunderboltOutlined />, label: 'Token统计' },
      { key: '/embedding-tasks', icon: <ApartmentOutlined />, label: '向量化任务' }
    ]
  }

  // 高级功能子菜单（仅管理员可见）
  const advancedSubMenu: MenuItem = {
    key: 'advanced',
    icon: <SettingOutlined />,
    label: '高级功能',
    children: [
      { key: '/vectors', icon: <DatabaseOutlined />, label: '向量管理' },
      { key: '/cache', icon: <ThunderboltOutlined />, label: '缓存管理' },
      { key: '/users', icon: <TeamOutlined />, label: '用户管理' }
    ]
  }

  // 构建完整菜单
  const buildMenuItems = (): MenuItem[] => {
    const items = [...mainMenuItems]
    
    // 添加监控与统计菜单
    items.push(statsSubMenu)
    
    // 仅管理员显示高级功能
    if (isAdmin) {
      items.push(advancedSubMenu)
    }
    
    return items
  }

  const menuItems = buildMenuItems()

  // 用户下拉菜单
  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心'
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      danger: true
    }
  ]

  const handleUserMenuClick = ({ key }: { key: string }) => {
    if (key === 'logout') {
      logout()
      navigate('/login')
    }
  }

  // 处理菜单点击
  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  // 处理submenu展开/收起
  const handleOpenChange = (keys: string[]) => {
    setOpenKeys(keys)
  }

  // 检查当前路径是否匹配菜单项
  const selectedKey = menuItems.some(item => {
    if (item.key === location.pathname) return true
    if (item.children) {
      return item.children.some(child => child.key === location.pathname)
    }
    return false
  }) ? location.pathname : ''

  // 获取当前展开的菜单keys
  const currentOpenKeys = menuItems
    .filter(item => item.children?.some(child => location.pathname.startsWith(child.key)))
    .map(item => item.key)

  return (
    <Sider width={220} className="bg-white shadow-md">
      {/* Logo */}
      <div className="h-16 flex items-center justify-center border-b">
        <span className="text-xl font-bold text-blue-500">AI KB</span>
      </div>
      
      {/* 导航菜单 */}
      <Menu
        mode="inline"
        selectedKeys={[selectedKey]}
        defaultOpenKeys={currentOpenKeys}
        openKeys={openKeys.length > 0 ? openKeys : currentOpenKeys}
        items={menuItems}
        onClick={handleMenuClick}
        onOpenChange={handleOpenChange}
        className="border-none"
        style={{ height: 'calc(100vh - 140px)', overflowY: 'auto' }}
      />
      
      {/* 用户信息 */}
      <div className="absolute bottom-0 left-0 right-0 p-4 border-t bg-white">
        <Dropdown
          menu={{ items: userMenuItems, onClick: handleUserMenuClick }}
          placement="topRight"
        >
          <div className="flex items-center cursor-pointer hover:bg-gray-50 p-2 rounded">
            <Badge dot={user?.status === 'ACTIVE'}>
              <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#1890ff' }} />
            </Badge>
            <div className="ml-2 flex-1 min-w-0">
              <div className="text-sm font-medium truncate">{user?.username || '用户'}</div>
              <div className="text-xs text-gray-400">
                <Badge 
                  status={isAdmin ? 'success' : 'processing'} 
                  text={user?.role === 'ADMIN' ? '管理员' : user?.role === 'USER' ? '用户' : '访客'} 
                />
              </div>
            </div>
          </div>
        </Dropdown>
      </div>
    </Sider>
  )
}

export default Sidebar
