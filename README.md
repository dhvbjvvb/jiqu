# 即取 Jiqu

<p align="center">
  <strong>轻量、快速的短视频与图集解析下载工具</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white" alt="Android 12+" />
  <img src="https://img.shields.io/badge/version-1.5.7-2EA7E0" alt="Version 1.5.7" />
  <img src="https://img.shields.io/badge/R8%20release-%7E2.71%20MB-FF8A00" alt="R8 release about 2.71 MB" />
  <img src="https://img.shields.io/badge/license-MIT-22C55E" alt="MIT License" />
</p>

> ⚡ 用更小的安装包，完成链接识别、解析、预览与下载。

即取是一款 Android Compose 应用，面向常见短视频、图集与实况链接提供无水印解析和下载体验。R8 优化后的 release APK 约 **2.69 MB**，保留完整的解析、历史记录、主题与下载能力。

## ✨ 特性

- 🌐 **十个平台支持**：哔哩哔哩、抖音、快手、皮皮虾、皮皮搞笑、今日头条、微博、微信视频号、小红书、最右。
- 🔗 **智能链接处理**：可自动识别分享文本中的链接，也可自动读取剪贴板中的受支持链接。
- 🎬 **多媒体解析**：支持视频、图集、实况和背景音乐；图集可左右滑动预览并选择下载内容。
- 🎞️ **清晰度选择**：展示可用 MP4 清晰度，过滤不适合直接下载的 DASH 资源。
- ⚡ **并行分段下载**：支持最多 20 路并发分段下载，并在服务端不支持分段时自动回退至单连接下载。
- 🖼️ **可靠历史记录**：保留最新解析结果与封面，重复链接不会产生多条历史记录。
- 🌗 **主题适配**：跟随系统、浅色和深色三种模式。
- 📦 **轻量发布**：release 启用 R8 代码压缩、混淆和资源收缩。

## 📱 使用方式

1. 复制一个受支持平台的分享链接。
2. 打开即取，自动粘贴或手动粘贴链接。
3. 点击“立即解析”。
4. 预览媒体，按需选择清晰度、图片或实况内容并下载。

下载内容默认保存到：

| 类型 | 目录 |
| --- | --- |
| 图片 | `Download/Jiqu/Picture` |
| 音频 | `Download/Jiqu/Music` |
| 视频、动图、实况 | `Download/Jiqu/video` |

## 🛠️ 构建

### 环境

- Android Studio
- JDK 21
- Android SDK，`compileSdk 37`
- Android 12（API 31）及以上设备

### 命令

```powershell
# Configure the local-only API key before building.
# The key is read from local.properties (bugpkApiKey) or BUGPK_API_KEY.

# Debug APK
.\gradlew.bat assembleDebug

# Unit tests
.\gradlew.bat testDebugUnitTest

# R8 optimized release APK
.\gradlew.bat assembleRelease
```

输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

> 发布前请使用自己的 release keystore 对 release APK 签名，切勿提交 keystore、`local.properties` 或构建产物。

解析服务使用 `https://api-new.ifphp.com/api/svparse`，通过 `X-API-Key` Header 认证。API Key 仅应放在本机的 `local.properties` 或构建环境变量中，不要提交到仓库。

## 🧱 项目结构

```text
app/
  src/main/java/com/jiqu/app/
    BugPkApiClient.kt        # 平台识别与解析接口
    ParserViewModel.kt       # 解析、历史与下载状态
    MainActivity.kt          # Compose UI 与下载实现
    ui/theme/                # 主题与配色
  src/test/                  # 单元测试
gradle/                      # Gradle Wrapper 与版本目录
```

## ⚠️ 说明

- 本项目仅提供链接解析与下载能力，不存储、不托管任何第三方媒体内容。
- 请遵守相关平台规则、版权协议及当地法律法规，仅下载和使用拥有合法授权的内容。
- 解析接口与第三方媒体地址可能随平台策略变化而失效。

## 📄 License

本项目采用 [MIT License](LICENSE)。
