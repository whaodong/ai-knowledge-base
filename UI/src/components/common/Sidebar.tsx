import { Layout, Menu } from 'antd'
import { FileTextOutlined, MessageOutlined, DashboardOutlined, SettingOutlined } from '@ant-design/icons'
import { useNavigate, useLocation } from 'react-router-dom'

const { Sider } = Layout

const Sidebar = () => {
  const navigate = useNavigate()
  const location = useLocation()

  const menuItems = [
    { key: '/documents', icon: <FileTextOutlined />, label: '文档管理' },
    { key: '/query', icon: <MessageOutlined />, label: 'RAG查询' },
    { key: '/monitoring', icon: <DashboardOutlined />, label: '监控' },
    { key: '/settings', icon: <SettingOutlined />, label: '设置' }
  ]

  return (
    <Sider width={200} className="bg-white shadow-md">
      <div className="h-16 flex items-center justify-center border-b">
        <span className="text-lg font-bold text-blue-500">AI KB</span>
      </div>
      <Menu
        mode="inline"
        selectedKeys={[location.pathname]}
        items={menuItems}
        onClick={({ key }) => navigate(key)}
      />
    </Sider>
  )
}

export default Sidebar
