import React, { useState } from 'react';
import { Form, Input, Button, Checkbox, message, Card } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import type { LoginRequest } from '@/types/api';
import styles from './Login.module.css';

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const onFinish = async (values: LoginRequest) => {
    setLoading(true);
    try {
      await login(values.username, values.password);
      message.success('登录成功');
      navigate('/documents');
    } catch (error: any) {
      message.error(error.response?.data?.message || '登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      {/* 背景装饰 */}
      <div className={styles.background}>
        <div className={styles.gradientOrb1} />
        <div className={styles.gradientOrb2} />
        <div className={styles.gradientOrb3} />
      </div>

      {/* 登录卡片 */}
      <Card className={styles.loginCard}>
        {/* Logo区域 */}
        <div className={styles.logoSection}>
          <div className={styles.logoIcon}>
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M2 17L12 22L22 17" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M2 12L12 17L22 12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <h1 className={styles.title}>AI Knowledge Base</h1>
          <p className={styles.subtitle}>企业级智能知识库系统</p>
        </div>

        {/* 登录表单 */}
        <Form
          name="login"
          onFinish={onFinish}
          layout="vertical"
          className={styles.form}
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined className={styles.inputIcon} />}
              placeholder="用户名或邮箱地址"
              className={styles.input}
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined className={styles.inputIcon} />}
              placeholder="密码"
              className={styles.input}
            />
          </Form.Item>

          <Form.Item>
            <div className={styles.formOptions}>
              <Checkbox name="remember" defaultChecked>
                保持登录会话
              </Checkbox>
              <Link to="/forgot-password" className={styles.forgotLink}>
                忘记密码？
              </Link>
            </div>
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              className={styles.submitButton}
              block
            >
              登录
            </Button>
          </Form.Item>
        </Form>

        {/* 注册提示 */}
        <div className={styles.footer}>
          <span className={styles.footerText}>还没有账号？</span>
          <Link to="/register" className={styles.registerLink}>
            立即注册
          </Link>
        </div>
      </Card>

      {/* 底部版权 */}
      <div className={styles.copyright}>
        © 2026 AI Knowledge Base. All rights reserved.
      </div>
    </div>
  );
};

export default Login;
