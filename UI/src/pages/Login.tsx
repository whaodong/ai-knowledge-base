import { useState } from 'react'
import { Form, Input, Button, Card, message, Typography } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate, Link } from 'react-router-dom'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import type { LoginRequest } from '@/types/api'
import './Login.module.css'

const { Title, Text } = Typography

const Login = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { login } = useAuthStore()

  const onFinish = async (values: LoginRequest) => {
    setLoading(true)
    try {
      const res = await authApi.login(values)
      if (res.code === 200 && res.data) {
        const { accessToken, refreshToken, username, role } = res.data
        login(
          { id: '', username, role: role as 'USER' | 'ADMIN', createdAt: '', updatedAt: '' },
          accessToken,
          refreshToken
        )
        message.success('登录成功')
        navigate('/documents')
      } else {
        message.error(res.message || '登录失败')
      }
    } catch (error) {
      message.error('登录失败，请检查用户名和密码')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container">
      <Card className="login-card" style={{ width: 400 }}>
        <div className="text-center mb-8">
          <Title level={2}>AI Knowledge Base</Title>
          <Text type="secondary">企业级知识库管理系统</Text>
        </div>
        
        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>

          <div className="text-center">
            <Text type="secondary">
              还没有账号？ <Link to="/register">立即注册</Link>
            </Text>
          </div>
        </Form>
      </Card>
    </div>
  )
}

export default Login
