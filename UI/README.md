# AI知识库管理系统前端

企业级AI知识库管理平台的前端项目，基于 React + Vite + TypeScript 构建，提供文档管理、RAG查询、监控仪表盘等功能。

## 🚀 技术栈

- **构建工具**: Vite 5
- **框架**: React 18
- **语言**: TypeScript
- **样式**: Tailwind CSS
- **组件库**: Ant Design 5
- **路由**: React Router 6
- **状态管理**: Zustand
- **数据请求**: TanStack Query (React Query)
- **图表**: ECharts + echarts-for-react
- **Markdown渲染**: react-markdown + remark-gfm + rehype-highlight
- **HTTP客户端**: Axios

## 📋 功能特性

### 1. 用户认证
- JWT Token 认证
- 登录/注册
- 角色权限管理 (USER/ADMIN)
- 自动 Token 刷新

### 2. 文档管理
- 文档上传（支持 PDF、TXT、Word、Markdown）
- 批量上传
- 拖拽上传
- 文档预览
- 文档分块展示
- 搜索、筛选、排序
- 批量删除

### 3. RAG 查询
- 实时对话界面
- SSE 流式响应
- Markdown 渲染
- 代码语法高亮
- 引用来源展示
- 历史会话管理

### 4. 监控仪表盘
- 服务健康状态监控
- QPS 实时曲线
- 延迟分布图 (P50/P90/P99)
- Token 使用量趋势
- 缓存命中率统计

### 5. 系统设置
- 主题切换（浅色/深色）
- 语言切换
- RAG 参数配置
- 管理员功能

## 📦 安装

```bash
# 克隆项目
git clone <repository-url>
cd ai-knowledge-base-frontend

# 安装依赖
npm install

# 或使用 pnpm
pnpm install

# 或使用 yarn
yarn install
```

## ⚙️ 配置

创建 `.env` 文件：

```env
VITE_API_URL=http://localhost:8080
```

## 🚀 启动

```bash
# 开发模式
npm run dev

# 构建
npm run build

# 预览构建结果
npm run preview

# 代码检查
npm run lint
```

## 📁 项目结构

```
ai-knowledge-base-frontend/
├── public/               # 静态资源
├── src/
│   ├── api/             # API层
│   │   ├── client.ts    # Axios实例
│   │   ├── auth.ts      # 认证API
│   │   ├── documents.ts # 文档API
│   │   ├── rag.ts       # RAG查询API
│   │   └── monitoring.ts# 监控API
│   ├── components/      # 组件
│   │   ├── common/      # 通用组件
│   │   ├── documents/   # 文档相关
│   │   ├── chat/        # 对话相关
│   │   └── monitoring/  # 监控相关
│   ├── hooks/           # 自定义Hooks
│   ├── pages/           # 页面
│   ├── stores/          # Zustand状态
│   ├── types/           # TypeScript类型
│   ├── utils/           # 工具函数
│   ├── styles/          # 样式
│   ├── App.tsx          # 根组件
│   └── main.tsx         # 入口文件
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.js
└── tsconfig.json
```

## 🔌 API 接口

### 认证接口
- `POST /api/v1/auth/login` - 登录
- `POST /api/v1/auth/register` - 注册
- `GET /api/v1/auth/me` - 获取当前用户
- `POST /api/v1/auth/logout` - 登出

### 文档接口
- `GET /api/v1/documents` - 获取文档列表
- `POST /api/v1/documents/upload` - 上传文档
- `GET /api/v1/documents/:id` - 获取文档详情
- `DELETE /api/v1/documents/:id` - 删除文档
- `GET /api/v1/documents/:id/preview` - 预览文档

### RAG 查询接口
- `POST /api/v1/rag/query` - 查询
- `GET /api/v1/rag/stream` - 流式查询 (SSE)
- `GET /api/v1/rag/history` - 获取历史

### 监控接口
- `GET /api/v1/monitoring/health` - 服务健康状态
- `GET /api/v1/monitoring/qps` - QPS 指标
- `GET /api/v1/monitoring/latency` - 延迟指标
- `GET /api/v1/monitoring/tokens` - Token 使用量
- `GET /api/v1/monitoring/cache` - 缓存指标

## 🎨 主题定制

项目支持浅色/深色主题切换，主题配置位于：

- `tailwind.config.js` - Tailwind CSS 主题配置
- `src/styles/globals.css` - 全局样式和 Markdown 样式
- `src/stores/settingsStore.ts` - 主题状态管理

## 📊 性能优化

1. **代码分割**: 使用 React.lazy 和 Suspense 进行路由级代码分割
2. **图片优化**: 使用 WebP 格式，懒加载图片
3. **缓存策略**: React Query 缓存 + localStorage 持久化
4. **按需加载**: Ant Design 组件按需加载

## 🔐 安全特性

1. **JWT 认证**: 基于 Token 的身份验证
2. **请求拦截**: 自动添加 Token 到请求头
3. **401 处理**: Token 过期自动跳转登录
4. **XSS 防护**: React 默认转义 + DOMPurify

## 📱 响应式设计

- 支持桌面端和移动端
- 使用 Tailwind CSS 响应式类
- Ant Design 响应式布局组件

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 License

MIT License
