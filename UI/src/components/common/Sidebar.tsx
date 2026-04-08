import { Layout, Menu, Avatar, Dropdown } from 'antd'
import { 
  FileTextOutlined, 
  MessageOutlined, 
  DashboardOutlined, 
  SettingOutlined,
  LogoutOutlined,
  UserOutlined 
} from '@ant-design/icons'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

const { Sider } = Layout

const Sidebar = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuthStore()

  const menuItems = [
    { key: '/documents', icon: <FileTextOutlined />, label: '文档管理' },
    { key: '/query', icon: <MessageOutlined />, label: 'RAG对话' },
    { key: '/monitoring', icon: <DashboardOutlined />, label: '监控面板' },
    { key: '/settings', icon: <SettingOutlined />, label: '系统设置' }
  ]

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

  return (
    <Sider width={200} className="bg-white shadow-md">
      {/* Logo */}
      <div className="h-16 flex items-center justify-center border-b">
        <span className="text-xl font-bold text-blue-500">AI KB</span>
      </div>
      
      {/* 导航菜单 */}
      <Menu
        mode="inline"
        selectedKeys={[location.pathname]}
        items={menuItems}
        onClick={({ key }) => navigate(key)}
        className="border-none"
      />
      
      {/* 用户信息 */}
      <div className="absolute bottom-0 left-0 right-0 p-4 border-t bg-white">
        <Dropdown
          menu={{ items: userMenuItems, onClick: handleUserMenuClick }}
          placement="topRight"
        >
          <div className="flex items-center cursor-pointer hover:bg-gray-50 p-2 rounded">
            <Avatar icon={<UserOutlined />} />
            <div className="ml-2">
              <div className="text-sm font-medium">{user?.username || '用户'}</div>
              <div className="text-xs text-gray-400">{user?.role || 'USER'}</div>
            </div>
          </div>
        </Dropdown>
      </div>
    </Sider>
  )
}

export default Sidebar
