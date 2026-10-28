# iBirdWatching Android Demo

这是一个离线、可安装的分类演示 APK，用于展示移动端 ONNX Runtime 推理链路。

## 文件

- 文件：`iBirdWatching-demo.apk`
- 包名：`com.ibirdwatch.demo`
- 版本：`0.1.1-demo`
- 最低系统：Android 12（API 31）
- 签名：demo keystore，仅用于展示

## 功能

- 内置 EfficientNet-B0 120 类鸟类分类模型
- 内置两张示例鸟图
- 本地 ONNX Runtime 推理
- 展示 Top-5 中文名称、学名和置信度
- 不联网，不依赖私有后端

## 模型版本

- 文件：`bird_efficientnet_b0.onnx`
- 格式：FP32 ONNX（外部权重 `bird_efficientnet_b0.onnx.data`）
- 类别：120 类
- 输入：224 × 224 RGB，0-255 原始像素（归一化已烘焙进模型）
- 用途：本 demo 只跑分类，不包含 YOLO 检测
- ONNX SHA256：`b684f5342f3580d21a3f5dfa2bcc38ca2a728dcb0382e229cf7a242a84436000`
- 外部权重 SHA256：`c2f72e1bcf4c4b619701549657c07df82d79f1351f1860a60586667a901f8249`

这份模型与主 App、服务端、`deploy_models` 中的同名模型字节一致；FP16/INT8 版本不在这份 demo 中使用。

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

也可以把 APK 传到 Android 手机上直接安装。首次运行需要把模型从 assets 复制到应用私有目录。

## 校验

```powershell
Get-FileHash .\iBirdWatching-demo.apk -Algorithm SHA256
```

校验值见 `SHA256SUMS`。

## 说明

这是展示用 demo，不是完整生产版本；模型文件和数据不应被重新分发为真实服务。
