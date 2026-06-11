# SpO₂-Wireless-Android

> 基于 STM32F405VGT6 的无线血氧双波长（660 nm / 940 nm）监测系统 —— Android 上位机（显示端）

![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Language](https://img.shields.io/badge/language-Java-007396)
![minSdk](https://img.shields.io/badge/minSdk-19-blue)
![targetSdk](https://img.shields.io/badge/targetSdk-28-blue)
![Transport](https://img.shields.io/badge/transport-Wi--Fi%20TCP%20Socket-orange)

本仓库为本科毕业设计 **《基于 STM32F405VGT6 的无线血氧双波长监测系统的设计与实现》** 的 Android 端源码。下位机（STM32 + 光电采集前端）通过 ESP8266 Wi-Fi 模块将采集到的两波长光电容积脉搏波（PPG）信号无线发送至手机，App 在屏幕上以四路实时滚动波形展示 **660 nm / 940 nm 的直流分量（DC）与交流分量（AC）**，用于血氧饱和度（SpO₂）相关信号的观测与算法验证。

> 本仓库只包含 Android 上位机源码，不含 STM32 固件与硬件原理图。

---

## 目录

- [系统概述](#系统概述)
- [整体架构](#整体架构)
- [功能特性](#功能特性)
- [界面预览](#界面预览)
- [技术栈与环境](#技术栈与环境)
- [通信协议](#通信协议)
- [代码结构](#代码结构)
- [核心模块说明](#核心模块说明)
- [快速开始](#快速开始)
- [硬件联调指引](#硬件联调指引)
- [本次重构说明](#本次重构说明)
- [二次开发建议](#二次开发建议)
- [已知限制](#已知限制)
- [许可与免责声明](#许可与免责声明)

---

## 系统概述

血氧饱和度的测量基于双波长光电法：人体组织对 **红光（660 nm）** 与 **近红外光（940 nm）** 的吸收差异随血红蛋白氧合状态而变化。前端通过两个波长的 LED 与光电探测器获取透射/反射光强，经过滤波分离出 **直流分量（DC）** 与 **交流分量（AC）**，二者的比值（R 值）是计算 SpO₂ 的核心输入。

下位机（STM32F405VGT6，内置 12 位 ADC，量程 0–4095）完成光电采集、模数转换与信号分离，并将四路数据封包后经 ESP8266 通过 Wi-Fi 发送；本仓库实现的是 **上位机显示端**：负责无线接收、解包与四路波形的实时绘制。波形绘制算法已经完成，服务器端（数据来源）与客户端（接收端）可按需改写或替换。

---

## 整体架构

```
┌─────────────────────────┐      Wi-Fi (SoftAP)      ┌──────────────────────────┐
│  下位机 / 数据源          │                          │  Android 上位机 (本仓库)   │
│                          │                          │                          │
│  光电前端 (660/940nm)     │                          │  TCP Client              │
│        │                 │      TCP Socket          │   可在"无线连接"页配置     │
│  STM32F405VGT6 (12bit ADC)│ ───────────────────────▶ │  默认 192.168.4.1:8080   │
│        │                 │   10 字节/帧, 4×16bit     │        │                 │
│  ESP8266 (汇承 HC, AP)    │                          │   DataInputStream         │
│  默认网关 192.168.4.1     │                          │   .readFully() 定长读取   │
│                          │                          │        │                 │
│                          │                          │  SpO2 自定义 View ×4      │
│                          │                          │  线程安全的实时波形绘制    │
└─────────────────────────┘                          └──────────────────────────┘
```

- **网络拓扑**：ESP8266 工作于 **SoftAP（热点）模式**，手机连接其热点；ESP8266 默认网关为 `192.168.4.1`，App 作为 **TCP 客户端** 连接该地址。
- **连接参数可配置**：在"无线连接"页输入 IP / 端口，校验通过后会持久化保存，下次自动回填。
- **数据流向**：STM32 采集 → ESP8266 封包发送 → Android 按 10 字节定长帧读取 → 解包为四路 16 位整数 → 分别送入四个自定义波形控件绘制。

---

## 功能特性

- **四路实时波形**：660 nm DC、940 nm DC、660 nm AC、940 nm AC，分通道卡片展示，标题/波长/颜色一一对应。
- **自定义高性能波形控件 `SpO2`**：基于 `View` + `Canvas` 绘制，带网格背景，满屏后自动滚动覆盖刷新；**线程安全**——数据接收线程写入与 UI 线程绘制之间通过锁与快照机制隔离。
- **可配置无线连接**：连接页可编辑 IP / 端口并做格式校验（IPv4 正则 + 端口范围），通过 `SharedPreferences` 持久化，下次自动回填。
- **健壮的 TCP 接收**：`DataInputStream.readFully()` 保证读满每一帧，正确处理连接断开（EOF），避免使用残留/半帧数据驱动绘图。
- **连接状态可视化**：监测页顶部状态点 + 文字（未连接 / 连接中 / 已连接 / 连接失败），开始/停止按钮防止重复建立连接。
- **现代化 UI**：医疗科技风格配色（深青绿主色 + 660nm 红 / 940nm 蓝双波形配色），卡片式布局，统一的按钮 / 输入框 / 状态指示样式。
- **零第三方图表依赖**：波形完全由原生 Canvas 实现，便于裁剪与移植。

---

## 界面预览

| 首页 | 无线连接 | 实时监测 | 关于 |
| --- | --- | --- | --- |
| 标题 + 三个功能入口（无线连接 / 开始监测 / 关于），统一卡片化按钮风格 | IP / 端口可编辑，带格式校验与错误提示，自动回填上次配置 | 顶部状态指示灯（未连接/连接中/已连接/失败），四路波形卡片，开始/停止切换 | 软件简介与三步操作说明，卡片化排版 |

> 仓库内 `preview.html`（如已包含）为基于实际布局还原的静态界面预览，可直接用浏览器打开查看四个页面效果。

---

## 技术栈与环境

| 项目 | 说明 |
| --- | --- |
| 语言 | Java（100%） |
| 构建工具 | Gradle（Android Gradle Plugin 3.2.0） |
| compileSdk / targetSdk | 28 |
| minSdk | 19（Android 4.4 KitKat） |
| Java 兼容性 | Java 8（`compileOptions` 已开启，支持 Lambda） |
| UI 依赖 | `com.android.support:appcompat-v7:28.0.0`、`constraint-layout:1.1.3` |
| 测试 | JUnit 4.12、Espresso 3.0.2 |
| 无线模块 | 汇承 HC ESP8266（亦可替换为其它 Wi-Fi 模块，原理一致） |
| 下位机 MCU | STM32F405VGT6（12 位 ADC） |

> 注：项目使用的是较早的 Android Support 库（非 AndroidX）。如需在最新 Android Studio / AGP 上构建，建议先执行 AndroidX 迁移（Refactor → Migrate to AndroidX）。

---

## 通信协议

App 通过 `DataInputStream.readFully()` 每次读取一个 **10 字节定长帧**，按 **大端（高字节在前）** 解析出四个 16 位无符号整数：

| 字节偏移 | 字段 | 解析方式 | 对应波形 |
| --- | --- | --- | --- |
| `buffer[0..1]` | 帧头 / 保留 | — | — |
| `buffer[2..3]` | 数据 1 | `(buf[2]&0xFF)<<8 \| (buf[3]&0xFF)` | 660nm DC |
| `buffer[4..5]` | 数据 2 | `(buf[4]&0xFF)<<8 \| (buf[5]&0xFF)` | 940nm DC |
| `buffer[6..7]` | 数据 3 | `(buf[6]&0xFF)<<8 \| (buf[7]&0xFF)` | 660nm AC |
| `buffer[8..9]` | 数据 4 | `(buf[8]&0xFF)<<8 \| (buf[9]&0xFF)` | 940nm AC |

- 每个数据值范围对应 STM32 的 12 位 ADC，即 `0–4095`；上位机绘制时按纵轴量程缩放。
- `readFully()` 会阻塞直到读满 10 字节，或在连接断开时抛出 `EOFException`（被捕获后自动停止接收并提示"连接失败"）。

> 若需修改协议（如增加校验位、变更帧长或字节序），请同步调整 `SecondActivity` 中的 `FRAME_SIZE` 与解包逻辑，以及下位机封包逻辑。

---

## 代码结构

```
SpO2-Wireless-Android/
├── app/
│   └── src/main/
│       ├── java/com/example/natha/myapplication000/
│       │   ├── SplashActivity.java   # 启动闪屏（2s 后进入主菜单）
│       │   ├── MainActivity.java     # 主菜单，三个入口按钮
│       │   ├── FirstActivity.java    # 连接配置页：IP/端口校验、持久化、回填
│       │   ├── SecondActivity.java   # 监测核心：TCP 连接、定长帧接收、状态管理
│       │   ├── ThirdActivity.java    # 关于页
│       │   └── SpO2.java             # 自定义波形 View（线程安全绘制核心）
│       ├── res/
│       │   ├── layout/               # 各 Activity 布局（卡片化重构）
│       │   ├── values/
│       │   │   ├── colors.xml        # 医疗科技风格配色体系
│       │   │   ├── styles.xml        # 按钮 / 输入框 / 卡片统一样式
│       │   │   ├── strings.xml       # 全部文案（含状态、错误提示）
│       │   │   └── attrs.xml         # SpO2 控件自定义属性
│       │   ├── drawable/             # 按钮、输入框、卡片、状态指示灯背景
│       │   └── mipmap-*/             # 图标资源
│       └── AndroidManifest.xml
├── app/build.gradle                  # 已开启 Java 8 compileOptions
├── build.gradle / settings.gradle / gradle.properties
└── gradlew / gradlew.bat
```

---

## 核心模块说明

### `SpO2.java` —— 自定义波形控件（线程安全）

继承自 `android.view.View`，是整个项目的绘制核心：

- **背景网格**：首次绘制时缓存横纵网格坐标（`listXLine` / `listYLine`），避免重复计算。
- **数据缓冲与同步**：`points[]`、`wavePointNum`、`curX`、`drawFloatCount` 等状态由数据接收线程（子线程）写入、UI 线程读取，所有访问均通过 `synchronized(lock)` 保护；绘制前拷贝快照，避免读写竞争与数组越界。
- **满屏滚动覆盖**：`drawFloatCount` 统一描述当前可绘制的有效数据量，修复了原版本满屏后绘制陈旧/越界数据导致的视觉撕裂问题。
- **纵轴缩放**：`ySize` 控制纵坐标最大值；ADC 为 12 位（最大 4096），绘制前按 `MAX_ADC_VALUE / (ySize * unitLength)` 缩放。
- **可配置属性**：通过 `attrs.xml` 中的 `declare-styleable`（`BackLineColor` / `TitleColor` / `PointerLineColor` / `TitleSize` / `XYTextSize`）在布局 XML 中配置外观；运行时调用 `setLineColor(r, g, b)` 设置波形颜色。
- **数据入口**：`setLinePoint(int value)` 向控件追加一个采样点，内部完成同步更新后切回主线程触发重绘，可安全地从子线程调用。

### `SecondActivity.java` —— 无线接收与状态管理

- 从 `SharedPreferences` 读取连接页保存的 IP / 端口（默认 `192.168.4.1:8080`）。
- 点击"开始"后启动接收线程：建立 `Socket` 连接（5 秒超时）→ `DataInputStream.readFully()` 循环读取 10 字节帧 → 解包 → 驱动四个 `SpO2` 控件。
- `isRunning` 标志防止重复点击产生多条并发线程；点击"停止"或 `onDestroy()` 时主动关闭 socket、终止线程。
- 连接状态（未连接 / 连接中 / 已连接 / 连接失败）通过状态点 + 文字实时反馈，所有 UI 更新均切回主线程。

### `FirstActivity.java` —— 连接配置页

- IP 地址使用 IPv4 正则校验，端口校验范围 1–65535，输入为空或非法时给出明确提示。
- 校验通过后保存到 `SharedPreferences`，下次进入页面自动回填。

### 界面流转

`SplashActivity`（闪屏）→ `MainActivity`（主菜单：无线连接 / 开始监测 / 关于）→ `FirstActivity`（连接配置）/ `SecondActivity`（监测）/ `ThirdActivity`（关于）。

---

## 快速开始

### 环境要求

- Android Studio（建议支持 Gradle 3.2.0 / AGP 3.2.0 的版本，或自行升级配置）
- JDK 8
- 一台 Android 4.4（API 19）及以上的真机

### 构建与运行

```bash
# 1. 克隆仓库
git clone https://github.com/Nathanielguo/SpO2-Wireless-Android.git
cd SpO2-Wireless-Android

# 2. 命令行构建 Debug 包
./gradlew assembleDebug          # Windows: gradlew.bat assembleDebug

# 3. 安装到已连接的设备
./gradlew installDebug
```

或直接用 Android Studio 打开工程，点击 **Run** 部署到真机。

> 由于涉及 Wi-Fi Socket 连接，**建议使用真机调试**，模拟器需自行处理网络可达性。

---

## 硬件联调指引

1. 给下位机上电，确认 ESP8266 进入 **SoftAP（热点）模式** 并正常广播热点。
2. 手机 **连接 ESP8266 的 Wi-Fi 热点**（确保手机获得 `192.168.4.x` 网段地址，网关为 `192.168.4.1`）。
3. 打开 App，进入"无线连接"页，确认/修改 IP 与端口（默认 `192.168.4.1` / `8080`）后点击"确认"。
4. 返回首页，进入"开始监测"页，点击"开始"建立连接；状态指示灯变为"已连接"后，四路波形开始实时滚动。

**排查建议**

- 状态显示"连接失败"：先确认手机已连接到 ESP8266 热点而非其它网络；用 `ping 192.168.4.1` 验证可达性，并检查端口是否与下位机一致。
- 波形长时间无变化但状态显示"已连接"：检查下位机是否按 10 字节定长帧持续发送数据。
- 波形幅度异常：检查 `SpO2` 控件的 `ySize` 缩放与下位机 ADC 量程（0–4095）是否匹配。

---

## 本次重构说明

相较早期版本，本次重构修复了以下关键问题：

1. **TCP 读取健壮性**：`is.read(buffer)` → `DataInputStream.readFully(buffer)`，避免半帧/错位数据；新增 EOF 检测与连接失败提示。
2. **线程安全**：`SpO2` 控件的数据写入（子线程）与绘制（主线程）之间补全同步机制，修复潜在的数组越界与绘制撕裂。
3. **重复连接防护**：新增 `isRunning` 状态位，避免反复点击"开始"产生多条并发线程争用同一 Socket。
4. **资源生命周期管理**：`onDestroy()` 中正确关闭 socket、终止接收线程，避免连接泄漏。
5. **连接配置可用化**：连接页的 IP / 端口输入框从"摆设"变为真正生效的配置项，带校验与持久化。
6. **AndroidManifest 修复**：修正 XML 语法错误，`label` 改为标准字符串资源引用。
7. **构建兼容性**：新增 `compileOptions`（Java 8），支持代码中的 Lambda 表达式。
8. **界面现代化**：统一配色与组件样式，四路波形分通道标注（660nm / 940nm，DC / AC），新增连接状态可视化。
9. **代码清理**：移除未使用的 import、冗余初始化与死代码。

---

## 二次开发建议

- **协议增强**：增加帧头校验、CRC、时间戳，提升抗丢包能力。
- **算法落地**：在四路 DC/AC 基础上计算 R 值与 SpO₂、心率（PR），并增加数值显示与异常报警。
- **数据持久化**：增加波形/数值的本地存储与导出（已声明读写存储权限）。
- **AndroidX 迁移**：迁移至 AndroidX、升级 Gradle/AGP，便于在最新工具链上构建。
- **断线重连**：在 `SecondActivity` 中增加自动重连与心跳保活机制。

---

## 已知限制

- 使用旧版 Android Support 库（非 AndroidX），在最新工具链上需迁移后才能顺利构建。
- 当前仅实现波形显示，**未包含 SpO₂ / 心率的数值计算与报警逻辑**。
- 数据帧格式（10 字节定长）为硬编码，缺少协议版本协商机制。
- 包名 `com.example.natha.myapplication000` 为脚手架默认命名，正式发布前建议规范化。

---

## 许可与免责声明

本项目为**本科毕业设计教学/研究用途**源码，仅供学习与算法验证参考。

> ⚠️ 本软件**不是医疗器械**，输出结果不得用于任何临床诊断或健康决策。如需用于医疗场景，必须满足相应法规与认证要求。

仓库未附带显式许可证；如需复用或二次发布，请先联系作者确认授权。
