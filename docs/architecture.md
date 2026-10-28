# 架构说明

## 客户端

- Kotlin + Jetpack Compose
- 相机/相册选图
- ONNX Runtime 端侧检测与分类
- 地图相册、社区地图、RAG 问答

## 服务端

- Spring Boot、Java 17
- JPA/MySQL 保存用户、记录、社区和知识库
- Spring Security + JWT
- 文件上传、识别、地图代理、RAG 和 SSE
- 约 60 个 REST/SSE 接口

## 管理后台

- Vue 3、Element Plus、ECharts
- 用户、物种、知识库、内容审核、文件和监控

## 安全边界

所有云服务密钥只存在服务端环境变量。客户端只访问经过鉴权的业务接口，不直接访问天地图、DeepSeek 或 OSS 密钥。
