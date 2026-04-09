import { Layout, Menu, Typography, Button, Space } from 'antd'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  DashboardOutlined,
  FileTextOutlined,
  RobotOutlined,
  DeploymentUnitOutlined,
  LogoutOutlined
} from '@ant-design/icons'
import { useMemo } from 'react'
import { useAuthStore } from '@/store/auth'

const { Header, Sider, Content } = Layout

export default function AdminLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { username, logout } = useAuthStore()

  const selectedKeys = useMemo(() => [location.pathname], [location.pathname])

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="light" width={220}>
        <div style={{ padding: 16 }}>
          <Typography.Title level={4} style={{ margin: 0 }}>
            AI 知识库后台
          </Typography.Title>
        </div>
        <Menu
          mode="inline"
          selectedKeys={selectedKeys}
          items={[
            { key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
            { key: '/documents', icon: <FileTextOutlined />, label: '文档管理' },
            { key: '/rag', icon: <RobotOutlined />, label: 'RAG 工作台' },
            { key: '/embeddings', icon: <DeploymentUnitOutlined />, label: '向量任务' }
          ]}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 16px' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text type="secondary">后端网关：/api 到 http://localhost:8080</Typography.Text>
            <Space>
              <Typography.Text>{username || '未登录用户'}</Typography.Text>
              <Button
                icon={<LogoutOutlined />}
                onClick={() => {
                  logout()
                  navigate('/login')
                }}
              >
                退出
              </Button>
            </Space>
          </Space>
        </Header>
        <Content style={{ margin: 16 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
