# 安全与发布检查

## 必须外置的配置

- `DB_PASSWORD`
- `JWT_SECRET`
- `TIANDITU_TK`
- `DEEPSEEK_API_KEY`
- OSS access key/secret
- CORS allowed origins

## 发布前检查

1. 扫描当前文件和完整 Git 历史中的密钥。
2. `.env`、`application-local.properties`、`*.pem`、`*.p12` 不进入版本库。
3. 模型权重、训练数据、用户上传图片不进入普通 Git。
4. 不使用默认 JWT secret 或示例数据库密码。
5. 通过环境变量配置 CORS 白名单。
6. 对上传文件做类型、大小和路径校验。

## 代码走读

公开仓库只提供脱敏摘录；完整工程通过面试后的只读邀请或屏幕共享展示，不发送个人访问令牌。
