import { Form, Input, Button, Card } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'

const Login = () => {
  const { login, isLoginLoading } = useAuth()

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <Card className="w-full max-w-md shadow-xl">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold">AI知识库系统</h1>
          <p className="text-gray-500 mt-2">企业级智能知识管理平台</p>
        </div>
        <Form onFinish={(v) => login(v as Parameters<typeof login>[0])} size="large">
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={isLoginLoading} block>登录</Button>
          </Form.Item>
          <div className="text-center">
            <span className="text-gray-500">还没有账号？</span>
            <Link to="/register" className="text-blue-500 ml-2">立即注册</Link>
          </div>
        </Form>
      </Card>
    </div>
  )
}

export default Login
