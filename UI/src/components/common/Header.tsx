import { Layout, Dropdown, Avatar, Button } from 'antd'
import { UserOutlined, LogoutOutlined, BulbOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { useSettingsStore } from '@/stores/settingsStore'

const { Header: AntHeader } = Layout

const Header = () => {
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()
  const { theme, toggleTheme } = useSettingsStore()

  const menuItems = [
    { key: 'logout', icon: <LogoutOutlined />, label: '退出', onClick: () => { logout(); navigate('/login') } }
  ]

  return (
    <AntHeader className="flex items-center justify-between bg-white shadow-sm px-6">
      <div className="text-lg font-semibold">AI知识库</div>
      <div className="flex items-center gap-4">
        <Button icon={<BulbOutlined />} onClick={toggleTheme}>
          {theme === 'light' ? '深色' : '浅色'}
        </Button>
        <Dropdown menu={{ items: menuItems }}>
          <div className="flex items-center cursor-pointer">
            <Avatar icon={<UserOutlined />} />
            <span className="ml-2">{user?.username}</span>
          </div>
        </Dropdown>
      </div>
    </AntHeader>
  )
}

export default Header
