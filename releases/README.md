# iBirdWatching Android Demo

这是一个离线、可安装的端侧识别演示 APK，用于展示移动端检测与分类推理链路。

## 文件

- 文件：`iBirdWatching-demo.apk`
- 包名：`com.example.ibirdwatchingdemo`
- 版本：`1.0.0-demo`
- 最低系统：Android 12（API 31）
- 大小：约 149 MB
- 签名：debug 签名，仅用于展示，不适合上架

## 功能

- 相机拍照
- 系统相册选图
- YOLO 检测 + EfficientNet-B0 120 类分类
- 本地 ONNX Runtime 推理，不连接后端
- 展示 Top-5 中文名称、学名和置信度

## 模型版本

- 文件：`bird_efficientnet_b0.onnx`
- 格式：FP32 ONNX（外部权重 `bird_efficientnet_b0.onnx.data`）
- 类别：120 类
- 输入：224 × 224 RGB，0-255 原始像素（归一化已烘焙进模型）
- 检测模型：`detect_6_640.onnx`（单类 bird）
- ONNX SHA256：`b684f5342f3580d21a3f5dfa2bcc38ca2a728dcb0382e229cf7a242a84436000`
- 外部权重 SHA256：`c2f72e1bcf4c4b619701549657c07df82d79f1351f1860a60586667a901f8249`
- 检测模型 SHA256：`16e0daf092e2e422a25f3593bcf2dd118e3ffbf0d11893edc4194aa2a24051d0`

分类模型与主 App、服务端、`deploy_models` 中的同名模型字节一致；这份 demo 使用 FP32 分类模型和检测模型，不包含 FP16/INT8 变体。

## 不包含

- 登录、注册和 JWT 后端
- 地图和天地图网关
- 社区、数据库和用户数据
- RAG/DeepSeek 远程问答

这些功能在完整私有工程中实现，公开展示仓库只提供设计说明和脱敏代码摘录。

## 安装

```powershell
adb install -r iBirdWatching-demo.apk
```

也可以把 APK 传到 Android 手机上直接安装。它和之前的 `com.ibirdwatch.demo` 是不同包名，可以同时安装。

## 校验

```powershell
Get-FileHash .\iBirdWatching-demo.apk -Algorithm SHA256
```

校验值见 `SHA256SUMS`。

## 说明

这是展示用 demo，不是完整生产版本；模型文件和数据不应被重新分发为真实服务。
