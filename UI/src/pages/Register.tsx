import { useState } from 'react'
import { Form, Input, Button, Card, message, Typography, Select } from 'antd'
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons'
import { useNavigate, Link } from 'react-router-dom'
import { authApi } from '@/api/auth'
import type { RegisterRequest } from '@/types/api'

const { Title, Text } = Typography

const Register = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const onFinish = async (values: RegisterRequest) => {
    setLoading(true)
    try {
      const res = await authApi.register(values)
      if (res.code === 200) {
        message.success('注册成功，请登录')
        navigate('/login')
      } else {
        message.error(res.message || '注册失败')
      }
    } catch (error) {
      message.error('注册失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container">
      <Card className="login-card" style={{ width: 400 }}>
        <div className="text-center mb-8">
          <Title level={2}>注册账号</Title>
          <Text type="secondary">创建您的知识库账号</Text>
        </div>
        
        <Form
          name="register"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, message: '用户名至少3个字符' }
            ]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>

          <Form.Item
            name="email"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' }
            ]}
          >
            <Input prefix={<MailOutlined />} placeholder="邮箱" />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 6, message: '密码至少6个字符' }
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请确认密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'))
                }
              })
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="确认密码" />
          </Form.Item>

          <Form.Item name="role" initialValue="USER">
            <Select
              options={[
                { value: 'USER', label: '普通用户' },
                { value: 'ADMIN', label: '管理员' }
              ]}
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              注册
            </Button>
          </Form.Item>

          <div className="text-center">
            <Text type="secondary">
              已有账号？ <Link to="/login">立即登录</Link>
            </Text>
          </div>
        </Form>
      </Card>
    </div>
  )
}

export default Register
