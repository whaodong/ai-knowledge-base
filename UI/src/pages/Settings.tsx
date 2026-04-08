import { Card, Form, Input, Button, Select, Switch, Divider, message, Space, InputNumber } from 'antd'
import { useSettingsStore } from '@/stores/settingsStore'
import { EMBEDDING_MODELS } from '@/types/embedding'

const Settings = () => {
  const { theme, setTheme, ragConfig, setRagConfig } = useSettingsStore()
  const [form] = Form.useForm()

  const handleSave = () => {
    message.success('设置已保存')
  }

  return (
    <div className="space-y-4">
      {/* 外观设置 */}
      <Card title="外观设置">
        <Form layout="vertical">
          <Form.Item label="主题">
            <Select
              value={theme}
              onChange={setTheme}
              options={[
                { value: 'light', label: '浅色模式' },
                { value: 'dark', label: '深色模式' }
              ]}
              style={{ width: 200 }}
            />
          </Form.Item>
        </Form>
      </Card>

      {/* RAG 配置 */}
      <Card title="RAG 参数配置">
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            topK: ragConfig.topK,
            similarityThreshold: ragConfig.similarityThreshold,
            hybridSearch: ragConfig.hybridSearch,
            rerankEnabled: ragConfig.rerankEnabled,
            embeddingModel: ragConfig.embeddingModel
          }}
          onValuesChange={(changed, values) => setRagConfig(values)}
        >
          <Form.Item label="检索数量 (Top K)" name="topK">
            <InputNumber min={1} max={50} style={{ width: 200 }} />
          </Form.Item>
          
          <Form.Item label="相似度阈值" name="similarityThreshold">
            <InputNumber min={0} max={1} step={0.1} style={{ width: 200 }} />
          </Form.Item>
          
          <Form.Item label="启用混合检索" name="hybridSearch" valuePropName="checked">
            <Switch />
          </Form.Item>
          
          <Form.Item label="启用重排序" name="rerankEnabled" valuePropName="checked">
            <Switch />
          </Form.Item>
          
          <Form.Item label="Embedding 模型" name="embeddingModel">
            <Select options={EMBEDDING_MODELS} style={{ width: 300 }} />
          </Form.Item>
        </Form>
      </Card>

      {/* API 配置 */}
      <Card title="API 配置">
        <Form layout="vertical">
          <Form.Item label="API 地址">
            <Input 
              value={import.meta.env.VITE_API_URL || 'http://localhost:8080'} 
              disabled 
              style={{ width: 400 }} 
            />
          </Form.Item>
          <Form.Item label="通义千问 API Key">
            <Input.Password 
              placeholder="请输入 DashScope API Key" 
              style={{ width: 400 }} 
            />
          </Form.Item>
        </Form>
      </Card>

      <Divider />

      <Space>
        <Button type="primary" onClick={handleSave}>保存设置</Button>
        <Button onClick={() => form.resetFields()}>重置</Button>
      </Space>
    </div>
  )
}

export default Settings
