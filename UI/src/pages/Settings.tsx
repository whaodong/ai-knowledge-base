import { Card, Form, Select } from 'antd'
import { useSettingsStore } from '@/stores/settingsStore'

const { Option } = Select

const Settings = () => {
  const { theme, toggleTheme } = useSettingsStore()

  return (
    <Card title="系统设置" className="max-w-2xl">
      <Form layout="vertical">
        <Form.Item label="主题模式">
          <Select value={theme} onChange={toggleTheme}>
            <Option value="light">浅色</Option>
            <Option value="dark">深色</Option>
          </Select>
        </Form.Item>
      </Form>
    </Card>
  )
}

export default Settings
