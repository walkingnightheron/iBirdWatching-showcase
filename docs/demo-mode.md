# Demo 模式设计

公开仓库的目标是让别人看到功能和工程判断，而不是拿到可滥用的真实服务。

## 1. 登录和注册

### API

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

### 流程

1. 注册时校验用户名、密码和可选邮箱。
2. 密码用 BCrypt 哈希后写入 demo 数据库。
3. 登录时比较 BCrypt 哈希，签发 access/refresh token。
4. 后续请求在 `Authorization: Bearer <access_token>` 中携带 token。
5. refresh token 只用于换新 access token，不在前端日志中输出。

### 配置

```properties
JWT_SECRET=replace-with-a-long-random-value
DEMO_SEED_USER=demo_user
DEMO_SEED_PASSWORD=replace-me
```

公开仓库只提交 `application.example.properties`，不提交 `.env` 或 `application-local.properties`。

## 2. 地图网关

业务层只依赖接口：

```java
public interface MapTileProvider {
    byte[] load(String layer, int z, int x, int y);
}
```

### 默认 demo 适配器

`MockMapTileProvider`：

- 不访问外部网络。
- 返回固定占位瓦片。
- 用于展示鉴权、缓存和接口结构。

### 真实适配器

`TiandituTileProvider`：

- 从 `TIANDITU_TK` 环境变量读取密钥。
- 只允许服务端调用。
- 带磁盘缓存、限流和超时。
- 没有配置密钥时直接禁用，不静默使用默认公共 token。

客户端只请求：

```text
GET /api/proxy/tiles/{layer}/{z}/{x}/{y}
```

请求必须经过 JWT 或短期 tile token 校验。

## 3. RAG / DeepSeek

```text
GET/POST /api/chat/conversations
POST /api/chat/conversations/{id}/messages
```

默认 `LocalKnowledgeProvider` 返回示例知识库内容；配置 `DEEPSEEK_API_KEY` 后才启用远程模型。模型不可用时返回本地片段，而不是把内部异常直接返回给客户端。

## 4. 公开仓库不要包含

- 真实数据库密码
- JWT secret
- OSS access key/secret
- 天地图 tk
- DeepSeek API key
- 用户上传图片
- 完整模型权重和训练数据集

## 5. 招聘方如何验证

- 阅读 README、架构图和代码摘录
- 运行不含真实密钥的 demo 配置
- 面试时由作者共享屏幕进行代码走读
- 如需完整私有仓库，由作者按需邀请只读 collaborator
