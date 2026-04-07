# Security Module 使用文档

## 概述

本模块提供了企业级知识库系统的用户权限控制功能，基于Spring Security 6 + JWT实现。

## 功能特性

### 1. JWT认证
- JWT Token生成、验证、解析
- Token刷新机制
- 支持无状态会话（SessionCreationPolicy.STATELESS）

### 2. 权限控制
- 基于角色的访问控制（RBAC）
- 支持方法级权限注解（@PreAuthorize）
- 灵活的权限配置

### 3. 用户管理
- User实体和Role枚举
- 自定义UserDetailsService
- 密码加密（BCryptPasswordEncoder）

## 快速开始

### 1. 添加依赖

在服务模块的pom.xml中添加common依赖：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置JWT密钥

在服务的application.yml中添加JWT配置：

```yaml
jwt:
  secret: your-secret-key-must-be-at-least-256-bits-long
  expiration: 86400000  # 24小时
  refresh-expiration: 604800000  # 7天
```

### 3. 实现UserDetailsService

创建自定义的UserDetailsService实现，从数据库加载用户：

```java
@Service
public class MyUserDetailsService implements CustomUserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public User loadUserByUsername(String username) {
        UserEntity entity = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return User.builder()
            .id(entity.getId())
            .username(entity.getUsername())
            .password(entity.getPassword())
            .roles(entity.getRoles())
            .enabled(entity.isEnabled())
            // ... 其他字段
            .build();
    }
}
```

### 4. 使用权限注解

在Controller中使用权限注解：

```java
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    // 需要USER角色
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public Document createDocument(@RequestBody DocumentRequest request) {
        // ...
    }
    
    // 需要ADMIN角色
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteDocument(@PathVariable Long id) {
        // ...
    }
    
    // VIEWER角色即可访问
    @GetMapping("/{id}")
    public Document getDocument(@PathVariable Long id) {
        // ...
    }
}
```

## API接口

### 认证接口

#### 登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "password"
}
```

响应：
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "user",
  "role": "USER"
}
```

#### 刷新Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 注册
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "password",
  "email": "user@example.com"
}
```

## 角色说明

### ADMIN（管理员）
- 拥有所有权限
- 可以管理所有用户和文档
- 可以访问管理接口 `/api/admin/**`

### USER（普通用户）
- 可以创建和管理自己的文档
- 可以访问文档接口 `/api/documents/**`
- 可以查询知识库

### VIEWER（访客）
- 只能查看公开文档
- 可以使用查询功能 `/api/query/**`
- 不能创建或修改文档

## 使用Token

在请求头中添加Token：

```http
GET /api/documents
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## 测试用户

默认提供以下测试用户：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | ADMIN |
| user | user123 | USER |
| viewer | viewer123 | VIEWER |

## 安全最佳实践

1. **密钥管理**
   - 生产环境必须修改默认密钥
   - 密钥长度至少256位
   - 建议使用环境变量或配置中心管理密钥

2. **Token有效期**
   - 访问Token建议设置为较短时间（如1-24小时）
   - 刷新Token可以设置较长时间（如7天）

3. **HTTPS**
   - 生产环境必须使用HTTPS
   - 防止Token在传输过程中被截获

4. **密码策略**
   - 强制用户使用强密码
   - 定期提醒用户更换密码

5. **日志审计**
   - 记录所有认证事件
   - 记录敏感操作

## 故障排查

### Token无效
- 检查密钥配置是否一致
- 检查Token是否过期
- 查看日志中的错误信息

### 权限不足
- 检查用户角色是否正确
- 检查接口权限配置
- 确认Token是否有效

### 无法认证
- 确认UserDetailsService实现是否正确
- 检查密码加密方式
- 查看认证管理器配置

## 相关类说明

- `JwtTokenProvider`: JWT工具类，处理Token生成、验证
- `JwtAuthenticationFilter`: JWT过滤器，拦截请求验证Token
- `JwtConfig`: JWT配置属性
- `SecurityConfig`: Spring Security主配置
- `User`: 用户实体，实现UserDetails接口
- `Role`: 角色枚举
- `AuthService`: 认证服务接口
- `CustomUserDetailsService`: 自定义UserDetailsService接口

## 注意事项

1. 使用jakarta.*包名（Spring Boot 3.x）
2. SessionCreationPolicy.STATELESS - 无状态会话
3. 默认配置适合微服务架构
4. 实际项目中应从数据库加载用户信息
5. 生产环境必须修改默认密钥
