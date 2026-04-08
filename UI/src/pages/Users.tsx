import { useState, useEffect } from 'react'
import {
  Table,
  Card,
  Button,
  Space,
  Tag,
  Modal,
  Select,
  Input,
  message,
  Popconfirm,
  Typography,
  Row,
  Col,
  Statistic,
  Form,
  Avatar,
  Tooltip,
  Badge
} from 'antd'
import {
  UserOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  ReloadOutlined,
  LockOutlined,
  UnlockOutlined,
  TeamOutlined,
  StopOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { userApi } from '@/api/user'
import type { User, UserRole, UserStatus, UserQueryParams } from '@/types/user'
import { USER_ROLE_MAP, USER_STATUS_MAP } from '@/types/user'
import dayjs from 'dayjs'

const { Title, Text } = Typography

// 模拟数据
const mockUsers: User[] = [
  {
    id: '1',
    username: 'admin',
    email: 'admin@example.com',
    phone: '13800138000',
    avatar: undefined,
    role: 'ADMIN',
    status: 'ACTIVE',
    tokenUsed: 456789,
    tokenLimit: 1000000,
    createTime: '2024-01-01 10:00:00',
    lastLoginTime: '2024-01-07 14:30:00',
    description: '系统管理员'
  },
  {
    id: '2',
    username: 'user001',
    email: 'user001@example.com',
    phone: '13800138001',
    role: 'USER',
    status: 'ACTIVE',
    tokenUsed: 125680,
    tokenLimit: 500000,
    createTime: '2024-01-03 09:00:00',
    lastLoginTime: '2024-01-07 12:00:00'
  },
  {
    id: '3',
    username: 'user002',
    email: 'user002@example.com',
    phone: '13800138002',
    role: 'USER',
    status: 'ACTIVE',
    tokenUsed: 98765,
    tokenLimit: 500000,
    createTime: '2024-01-04 11:00:00',
    lastLoginTime: '2024-01-06 18:00:00'
  },
  {
    id: '4',
    username: 'viewer001',
    email: 'viewer001@example.com',
    role: 'VIEWER',
    status: 'INACTIVE',
    tokenUsed: 0,
    tokenLimit: 10000,
    createTime: '2024-01-05 14:00:00'
  },
  {
    id: '5',
    username: 'user003',
    email: 'user003@example.com',
    phone: '13800138003',
    role: 'USER',
    status: 'BANNED',
    tokenUsed: 256789,
    tokenLimit: 500000,
    createTime: '2024-01-02 08:00:00',
    lastLoginTime: '2024-01-05 22:00:00',
    description: '违规用户，已被封禁'
  },
  {
    id: '6',
    username: 'user004',
    email: 'user004@example.com',
    role: 'USER',
    status: 'PENDING',
    tokenUsed: 0,
    tokenLimit: 100000,
    createTime: '2024-01-07 10:00:00'
  }
]

// 格式化Token
const formatToken = (token: number): string => {
  if (token >= 1000000) return `${(token / 1000000).toFixed(1)}M`
  if (token >= 1000) return `${(token / 1000).toFixed(1)}K`
  return token.toString()
}

const Users = () => {
  const [loading, setLoading] = useState(false)
  const [users, setUsers] = useState<User[]>(mockUsers)
  const [params, setParams] = useState<UserQueryParams>({
    pageNum: 1,
    pageSize: 10
  })
  const [total, setTotal] = useState(0)
  const [createModalVisible, setCreateModalVisible] = useState(false)
  const [editModalVisible, setEditModalVisible] = useState(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()

  useEffect(() => {
    loadUsers()
  }, [params])

  // 加载用户列表
  const loadUsers = async () => {
    setLoading(true)
    try {
      // 实际项目中调用API
      // const res = await userApi.getUsers(params)
      // if (res.code === 200 && res.data) {
      //   setUsers(res.data.records)
      //   setTotal(res.data.total)
      // }
      setLoading(false)
    } catch (error) {
      message.error('加载用户列表失败')
      setLoading(false)
    }
  }

  // 创建用户
  const handleCreate = async (values: { username: string; password: string; email?: string; role: UserRole }) => {
    try {
      // await userApi.createUser(values)
      message.success('用户创建成功')
      setCreateModalVisible(false)
      form.resetFields()
      loadUsers()
    } catch (error) {
      message.error('创建失败')
    }
  }

  // 更新用户
  const handleUpdate = async (values: Partial<User>) => {
    if (!selectedUser) return
    try {
      // await userApi.updateUser({ id: selectedUser.id, ...values })
      message.success('用户更新成功')
      setEditModalVisible(false)
      setSelectedUser(null)
      editForm.resetFields()
      loadUsers()
    } catch (error) {
      message.error('更新失败')
    }
  }

  // 删除用户
  const handleDelete = async (id: string) => {
    try {
      // await userApi.deleteUser(id)
      message.success('删除成功')
      loadUsers()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 批量删除
  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的用户')
      return
    }
    try {
      // await userApi.batchDeleteUsers(selectedRowKeys)
      message.success(`已删除 ${selectedRowKeys.length} 个用户`)
      setSelectedRowKeys([])
      loadUsers()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 修改状态
  const handleChangeStatus = async (user: User, newStatus: UserStatus) => {
    try {
      // await userApi.changeUserStatus(user.id, newStatus)
      message.success(`用户已${newStatus === 'ACTIVE' ? '启用' : '禁用'}`)
      loadUsers()
    } catch (error) {
      message.error('操作失败')
    }
  }

  // 修改角色
  const handleChangeRole = async (user: User, newRole: UserRole) => {
    try {
      // await userApi.changeUserRole(user.id, newRole)
      message.success('角色已更新')
      loadUsers()
    } catch (error) {
      message.error('更新失败')
    }
  }

  // 表格列定义
  const columns: ColumnsType<User> = [
    {
      title: '用户',
      key: 'user',
      render: (_: unknown, record: User) => (
        <Space>
          <Badge dot={record.status === 'ACTIVE'} status="success">
            <Avatar
              src={record.avatar}
              icon={<UserOutlined />}
              style={{ backgroundColor: record.role === 'ADMIN' ? '#1890ff' : '#52c41a' }}
            />
          </Badge>
          <div>
            <div className="font-medium">{record.username}</div>
            <div className="text-xs text-gray-400">{record.email}</div>
          </div>
        </Space>
      )
    },
    {
      title: '角色',
      dataIndex: 'role',
      width: 100,
      render: (role: UserRole) => {
        const config = USER_ROLE_MAP[role]
        return <Tag color={config?.color}>{config?.text || role}</Tag>
      }
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: UserStatus) => {
        const config = USER_STATUS_MAP[status]
        return <Tag color={config?.color}>{config?.text || status}</Tag>
      }
    },
    {
      title: 'Token使用',
      width: 150,
      render: (_: unknown, record: User) => (
        <div>
          <div className="text-sm">{formatToken(record.tokenUsed)} / {formatToken(record.tokenLimit)}</div>
          <div className="w-24 h-1.5 bg-gray-200 rounded mt-1">
            <div
              className={`h-full rounded ${record.tokenUsed / record.tokenLimit > 0.8 ? 'bg-red-500' : 'bg-blue-500'}`}
              style={{ width: `${Math.min((record.tokenUsed / record.tokenLimit) * 100, 100)}%` }}
            />
          </div>
        </div>
      )
    },
    {
      title: '手机',
      dataIndex: 'phone',
      width: 120,
      render: (phone?: string) => phone || '-'
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 160,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm')
    },
    {
      title: '最后登录',
      dataIndex: 'lastLoginTime',
      width: 160,
      render: (time?: string) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: unknown, record: User) => (
        <Space size="small">
          <Tooltip title="编辑">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setSelectedUser(record)
                editForm.setFieldsValue(record)
                setEditModalVisible(true)
              }}
            />
          </Tooltip>
          {record.status === 'ACTIVE' ? (
            <Tooltip title="禁用">
              <Popconfirm
                title="确定禁用该用户？"
                onConfirm={() => handleChangeStatus(record, 'INACTIVE')}
              >
                <Button type="link" size="small" danger icon={<LockOutlined />} />
              </Popconfirm>
            </Tooltip>
          ) : (
            <Tooltip title="启用">
              <Button
                type="link"
                size="small"
                icon={<UnlockOutlined />}
                onClick={() => handleChangeStatus(record, 'ACTIVE')}
              />
            </Tooltip>
          )}
          <Popconfirm
            title="确定删除该用户？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      )
    }
  ]

  // 统计数据
  const statsData = {
    total: users.length,
    active: users.filter(u => u.status === 'ACTIVE').length,
    banned: users.filter(u => u.status === 'BANNED').length,
    pending: users.filter(u => u.status === 'PENDING').length
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <Title level={4}>用户管理</Title>
        <Space>
          <Text type="secondary">已选择 {selectedRowKeys.length} 项</Text>
          <Button danger disabled={selectedRowKeys.length === 0} onClick={handleBatchDelete}>
            批量删除
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalVisible(true)}>
            添加用户
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadUsers}>
            刷新
          </Button>
        </Space>
      </div>

      {/* 统计卡片 */}
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总用户数"
              value={statsData.total}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="活跃用户"
              value={statsData.active}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="待审核"
              value={statsData.pending}
              prefix={<StopOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已封禁"
              value={statsData.banned}
              prefix={<StopOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 筛选条件 */}
      <Card>
        <Space wrap>
          <Select
            placeholder="角色筛选"
            allowClear
            style={{ width: 120 }}
            onChange={(value) => setParams({ ...params, role: value })}
            options={Object.entries(USER_ROLE_MAP).map(([value, config]) => ({
              label: config.text,
              value
            }))}
          />
          <Select
            placeholder="状态筛选"
            allowClear
            style={{ width: 120 }}
            onChange={(value) => setParams({ ...params, status: value })}
            options={Object.entries(USER_STATUS_MAP).map(([value, config]) => ({
              label: config.text,
              value
            }))}
          />
          <Input.Search
            placeholder="搜索用户名/邮箱"
            style={{ width: 200 }}
            onSearch={(value) => setParams({ ...params, username: value })}
            allowClear
          />
        </Space>
      </Card>

      {/* 用户列表 */}
      <Card>
        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys as string[])
          }}
          pagination={{
            current: params.pageNum,
            pageSize: params.pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => setParams({ ...params, pageNum: page, pageSize })
          }}
        />
      </Card>

      {/* 创建用户弹窗 */}
      <Modal
        title="添加用户"
        open={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false)
          form.resetFields()
        }}
        footer={null}
        width={500}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item
            name="username"
            label="用户名"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, max: 20, message: '用户名长度为3-20个字符' }
            ]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 6, message: '密码至少6个字符' }
            ]}
          >
            <Input.Password placeholder="请输入密码" />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input type="email" placeholder="请输入邮箱（可选）" />
          </Form.Item>
          <Form.Item
            name="role"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
            initialValue="USER"
          >
            <Select
              options={[
                { label: '普通用户', value: 'USER' },
                { label: '访客', value: 'VIEWER' },
                { label: '管理员', value: 'ADMIN' }
              ]}
            />
          </Form.Item>
          <Form.Item className="mb-0">
            <Space>
              <Button type="primary" htmlType="submit">
                创建
              </Button>
              <Button onClick={() => setCreateModalVisible(false)}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑用户弹窗 */}
      <Modal
        title="编辑用户"
        open={editModalVisible}
        onCancel={() => {
          setEditModalVisible(false)
          setSelectedUser(null)
          editForm.resetFields()
        }}
        footer={null}
        width={500}
      >
        <Form form={editForm} layout="vertical" onFinish={handleUpdate}>
          <Form.Item name="email" label="邮箱">
            <Input type="email" placeholder="请输入邮箱" />
          </Form.Item>
          <Form.Item name="phone" label="手机">
            <Input placeholder="请输入手机号" />
          </Form.Item>
          <Form.Item name="role" label="角色">
            <Select
              options={[
                { label: '普通用户', value: 'USER' },
                { label: '访客', value: 'VIEWER' },
                { label: '管理员', value: 'ADMIN' }
              ]}
            />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { label: '正常', value: 'ACTIVE' },
                { label: '未激活', value: 'INACTIVE' },
                { label: '已封禁', value: 'BANNED' },
                { label: '待审核', value: 'PENDING' }
              ]}
            />
          </Form.Item>
          <Form.Item name="tokenLimit" label="Token限额">
            <Input type="number" placeholder="请输入Token限额" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述（可选）" />
          </Form.Item>
          <Form.Item className="mb-0">
            <Space>
              <Button type="primary" htmlType="submit">
                保存
              </Button>
              <Button onClick={() => setEditModalVisible(false)}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Users
