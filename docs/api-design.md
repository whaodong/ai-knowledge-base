# RESTful API 设计文档

## Week 3 Day 15-17 - 知识库核心功能API设计

### 1. 统一响应格式

#### 1.1 Result<T> 统一响应对象

```java
public class Result<T> {
    private Integer code;        // 响应码
    private String message;      // 响应消息
    private T data;             // 响应数据
    private LocalDateTime timestamp;  // 时间戳
    private String traceId;     // 追踪ID
}
```

#### 1.2 使用示例

**成功响应：**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": { ... },
    "timestamp": "2024-04-07T17:00:00",
    "traceId": "req-12345"
}
```

**失败响应：**
```json
{
    "code": 400,
    "message": "参数错误",
    "timestamp": "2024-04-07T17:00:00",
    "traceId": "req-12345"
}
```

### 2. 分页参数

#### 2.1 PageRequest 分页请求

```java
public class PageRequest {
    private Integer pageNum = 1;      // 页码（从1开始）
    private Integer pageSize = 10;    // 每页大小
    private String sortBy;            // 排序字段
    private String sortOrder = "DESC"; // 排序方向
}
```

#### 2.2 PageResponse<T> 分页响应

```java
public class PageResponse<T> {
    private List<T> records;       // 数据列表
    private Long total;           // 总记录数
    private Integer pageNum;      // 当前页码
    private Integer pageSize;     // 每页大小
    private Integer totalPages;   // 总页数
    private Boolean hasNext;      // 是否有下一页
    private Boolean hasPrevious;  // 是否有上一页
}
```

### 3. 异常处理

#### 3.1 业务异常

```java
public class BusinessException extends RuntimeException {
    private final Integer code;
    private final String message;
}
```

#### 3.2 错误码枚举

| 错误码 | 说明 |
|-------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 2001 | 文档不存在 |
| 2002 | 文档上传失败 |
| 2003 | 文档解析失败 |
| 3001 | 向量生成失败 |
| 4001 | RAG查询失败 |

### 4. 文档管理API

#### 4.1 上传文档

**请求：**
```
POST /api/v1/documents
Content-Type: multipart/form-data
```

**参数：**
- file: 文档文件（必需）
- request: DocumentUploadRequest

**响应：**
```json
{
    "code": 200,
    "message": "文档上传成功",
    "data": {
        "id": 1,
        "fileName": "guide.pdf",
        "fileType": "pdf",
        "fileSize": 1024000,
        "status": "UPLOADED",
        "createTime": "2024-04-07T17:00:00"
    }
}
```

#### 4.2 批量上传文档

**请求：**
```
POST /api/v1/documents/batch
Content-Type: multipart/form-data
```

**参数：**
- files: 文档文件数组（必需）
- request: DocumentBatchUploadRequest

**响应：**
```json
{
    "code": 200,
    "message": "批量上传完成",
    "data": {
        "total": 5,
        "successCount": 4,
        "failedCount": 1,
        "successIds": [1, 2, 3, 4],
        "failedFiles": ["error.pdf"]
    }
}
```

#### 4.3 查询文档列表

**请求：**
```
GET /api/v1/documents?pageNum=1&pageSize=10&status=UPLOADED
```

**参数：**
- pageNum: 页码
- pageSize: 每页大小
- fileName: 文件名关键词（可选）
- fileType: 文件类型（可选）
- status: 文档状态（可选）

**响应：**
```json
{
    "code": 200,
    "data": {
        "records": [...],
        "total": 100,
        "pageNum": 1,
        "pageSize": 10,
        "totalPages": 10,
        "hasNext": true,
        "hasPrevious": false
    }
}
```

#### 4.4 获取文档详情

**请求：**
```
GET /api/v1/documents/{id}
```

**响应：**
```json
{
    "code": 200,
    "data": {
        "id": 1,
        "fileName": "guide.pdf",
        "filePath": "/uploads/xxx.pdf",
        "fileType": "pdf",
        "fileSize": 1024000,
        "status": "UPLOADED",
        "createTime": "2024-04-07T17:00:00"
    }
}
```

#### 4.5 删除文档

**请求：**
```
DELETE /api/v1/documents/{id}
```

**响应：**
```json
{
    "code": 200,
    "message": "操作成功"
}
```

### 5. 向量化API

#### 5.1 文本向量化

**请求：**
```
POST /api/v1/embeddings
Content-Type: application/json
```

**请求体：**
```json
{
    "text": "这是需要向量化的文本",
    "model": "text-embedding-3-small",
    "documentId": 1,
    "async": false
}
```

**响应：**
```json
{
    "code": 200,
    "message": "向量化成功",
    "data": {
        "taskId": "task-123",
        "text": "这是需要向量化的文本",
        "embedding": [0.1, 0.2, ...],
        "dimension": 1536,
        "model": "text-embedding-3-small",
        "status": "COMPLETED",
        "duration": 150
    }
}
```

#### 5.2 批量文本向量化

**请求：**
```
POST /api/v1/embeddings/batch
Content-Type: application/json
```

**请求体：**
```json
{
    "texts": [
        {"text": "文本1"},
        {"text": "文本2"}
    ],
    "model": "text-embedding-3-small",
    "async": true
}
```

**响应：**
```json
{
    "code": 200,
    "message": "批量向量化完成",
    "data": {
        "batchTaskId": "batch-123",
        "total": 2,
        "successCount": 2,
        "failedCount": 0,
        "results": [...]
    }
}
```

#### 5.3 查询向量化任务状态

**请求：**
```
GET /api/v1/embeddings/status/{taskId}
```

**响应：**
```json
{
    "code": 200,
    "data": {
        "taskId": "task-123",
        "status": "COMPLETED",
        "embedding": [...]
    }
}
```

### 6. RAG查询API

#### 6.1 RAG查询

**请求：**
```
POST /api/v1/rag/query
Content-Type: application/json
```

**请求体：**
```json
{
    "query": "什么是人工智能？",
    "topK": 10,
    "similarityThreshold": 0.7,
    "hybridSearch": true,
    "rerankEnabled": true,
    "vectorWeight": 0.7,
    "keywordWeight": 0.3,
    "maxContextLength": 4000
}
```

**响应：**
```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "success": true,
        "retrievedDocuments": [
            {
                "content": "人工智能是...",
                "score": 0.95,
                "metadata": {...}
            }
        ],
        "fusedContext": "拼接后的上下文文本...",
        "retrievalTimeMs": 150,
        "retrieverStats": {
            "vectorRetrievedCount": 20,
            "keywordRetrievedCount": 15,
            "afterRerankCount": 10
        }
    }
}
```

#### 6.2 流式对话

**请求：**
```
POST /api/v1/rag/chat
Content-Type: application/json
Accept: text/event-stream
```

**请求体：**
```json
{
    "message": "什么是人工智能？",
    "sessionId": "session-123",
    "stream": true,
    "enableRag": true,
    "topK": 5,
    "temperature": 0.7
}
```

**响应（SSE流式）：**
```
event: message
data: {"sessionId":"session-123","reply":"基于您的查询...","finished":false}

event: message
data: {"sessionId":"session-123","reply":"基于您的查询，我找到了...","finished":false}

event: done
data: {"sessionId":"session-123","reply":"完整回复...","finished":true}
```

#### 6.3 获取会话历史

**请求：**
```
GET /api/v1/rag/history/{sessionId}
```

**响应：**
```json
{
    "code": 200,
    "data": {
        "sessionId": "session-123",
        "messages": [
            {
                "role": "user",
                "content": "什么是人工智能？",
                "timestamp": "2024-04-07T17:00:00"
            },
            {
                "role": "assistant",
                "content": "人工智能是...",
                "timestamp": "2024-04-07T17:00:05"
            }
        ],
        "createTime": "2024-04-07T17:00:00",
        "updateTime": "2024-04-07T17:00:05"
    }
}
```

### 7. API文档访问

启动服务后，可通过以下地址访问Swagger文档：

- Document Service: http://localhost:8081/swagger-ui.html
- Embedding Service: http://localhost:8082/swagger-ui.html
- RAG Service: http://localhost:8083/swagger-ui.html

### 8. 设计原则

1. **RESTful规范**：遵循RESTful API设计原则，使用标准HTTP方法
2. **统一响应格式**：所有API返回统一的Result<T>格式
3. **分页标准化**：使用PageRequest和PageResponse统一分页参数
4. **异常处理**：通过GlobalExceptionHandler统一处理异常
5. **参数校验**：使用Jakarta Validation进行参数校验
6. **API文档**：使用Swagger/OpenAPI注解生成在线文档
7. **版本控制**：API路径包含版本号（/api/v1/）
8. **追踪支持**：响应中包含traceId便于问题追踪

### 9. 技术实现

- Spring Boot 3.3.0
- Spring AI 1.0.0-M3
- Jakarta Validation
- SpringDoc OpenAPI 2.3.0
- Lombok
- Spring Data JPA

### 10. 后续优化

1. 添加JWT认证和授权
2. 实现API限流
3. 添加API版本兼容性
4. 实现请求日志记录
5. 添加性能监控
6. 实现API缓存
