import { useNavigate } from 'react-router-dom'
import { Button, Card, Form, Input, Typography, message } from 'antd'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import type { LoginRequest } from '@/types/auth'
import { useAuthStore } from '@/store/auth'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()
  const [form] = Form.useForm<LoginRequest>()

  const loginMutation = useMutation({
    mutationFn: (payload: LoginRequest) => authApi.login(payload),
    onSuccess: (res) => {
      setAuth({
        accessToken: res.data.accessToken,
        refreshToken: res.data.refreshToken,
        username: res.data.username,
        role: res.data.role
      })
      message.success('登录成功')
      navigate('/dashboard')
    },
    onError: (err) => {
      message.error((err as Error).message)
    }
  })

  return (
    <div style={{ minHeight: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
      <Card title="后台登录" style={{ width: 380 }}>
        <Typography.Paragraph type="secondary">
          默认测试账号：admin / admin123
        </Typography.Paragraph>
        <Form form={form} layout="vertical" initialValues={{ username: 'admin', password: 'admin123' }}>
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password />
          </Form.Item>
          <Button
            type="primary"
            block
            loading={loginMutation.isPending}
            onClick={async () => {
              const values = await form.validateFields()
              loginMutation.mutate(values)
            }}
          >
            登录
          </Button>
        </Form>
      </Card>
    </div>
  )
}
