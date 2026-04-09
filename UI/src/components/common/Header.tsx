import { Layout, Space, Button, Typography } from 'antd'
import { BellOutlined, QuestionCircleOutlined } from '@ant-design/icons'
import { useAuthStore } from '@/stores/authStore'

const { Header: AntHeader } = Layout
const { Text } = Typography

const Header = () => {
  useAuthStore()

  return (
    <AntHeader className="bg-white shadow-sm px-6 flex items-center justify-between">
      <div>
        <Text strong>AI Knowledge Base</Text>
        <Text type="secondary" className="ml-4">企业级知识库管理系统</Text>
      </div>
      <Space>
        <Button type="text" icon={<QuestionCircleOutlined />}>帮助</Button>
        <Button type="text" icon={<BellOutlined />}>通知</Button>
      </Space>
    </AntHeader>
  )
}

export default Header
