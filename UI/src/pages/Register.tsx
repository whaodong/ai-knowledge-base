import { Form, Input, Button, Card } from 'antd'
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'

const Register = () => {
  const { register, isRegisterLoading } = useAuth()

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <Card className="w-full max-w-md shadow-xl">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold">注册账号</h1>
          <p className="text-gray-500 mt-2">创建您的知识库账号</p>
        </div>
        <Form onFinish={(v) => register(v as Parameters<typeof register>[0])} size="large">
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item name="email" rules={[{ required: true, message: '请输入邮箱' }, { type: 'email', message: '邮箱格式不正确' }]}>
            <Input prefix={<MailOutlined />} placeholder="邮箱" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }, { min: 6, message: '至少6个字符' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item name="confirmPassword" dependencies={['password']} rules={[
            { required: true, message: '请确认密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('password') === value) return Promise.resolve()
                return Promise.reject(new Error('密码不一致'))
              }
            })
          ]}>
            <Input.Password prefix={<LockOutlined />} placeholder="确认密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={isRegisterLoading} block>注册</Button>
          </Form.Item>
          <div className="text-center">
            <span className="text-gray-500">已有账号？</span>
            <Link to="/login" className="text-blue-500 ml-2">立即登录</Link>
          </div>
        </Form>
      </Card>
    </div>
  )
}

export default Register
