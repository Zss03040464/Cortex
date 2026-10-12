# Cortex: Android 原生具身智能体主脑系统

> **Cortex 是专为 Android 移动设备研发的原生具身智能体主脑系统。通过将深度推理 Agent 决策中枢与 Android 内部原生 Debian 容器、ColorOS Mirage 离屏副屏驱动及全双工 NDJSON 事件流深度咬合，让移动终端拥有真正的思考、感知与全自主任务闭环能力。**

---

## 🌟 核心架构与技术创新

1. **单主脑具身架构 (Single Agent Brain)**：
   - 告别传统“外层机械调度 + 内部简单调用”的双层嵌套与上下文失真。
   - Cortex 决策主脑直接驱动推理、规划、步骤拆解、工具调用与现场结果求证。
   - 宿主系统提供高精度的系统 API、无障碍执行与硬件感知支持。

2. **全双工流式思维链与多步骤排版 (Streaming Thinking & Steps)**：
   - **双向 NDJSON 流式管道**：实时解析大模型流式输出与思维链（Thinking block）。
   - **分步清晰排版**：严格区分“起手意图说明”、“工具调用卡片（折叠进度）”与“阶段性事实结论”，杜绝信息流拥挤与粘连。
   - **权威落盘复验**：在每一轮任务执行完毕后，权威同步底层执行记录，展开即可回溯完整的推理细节。

3. **ColorOS Mirage 幻镜原生离屏副屏 (Zero-Disturbance Virtual Display)**：
   - **底层 PC 互联驱动注入**：基于 ColorOS 官方系统服务 `OplusMirageWindowManager` 构建独立虚拟显示容器。
   - **物理主屏零干扰底线**：所有自动化 GUI 操作、前台测试均在后台副屏（Display ID >= 10000）内独立运行，主屏不抢占焦点、不弹输入法、不干扰用户正常使用。
   - **物理防冲撞安全看门狗**：检测到主屏正在使用目标应用时主动避让，绝不打断用户操作。

4. **无头 Linux 容器底座与 PTY 交互终端 (Debian Rootfs & PTY Terminal)**：
   - **内核命名空间隔离**：`run_chroot.sh` 配合命名空间隔离与私有挂载，退出后挂载点 0 泄漏，对宿主系统绝对纯净。
   - **本地全栈运行环境**：容器内置完整 Node.js、Python、终端工具链与密钥守护服务，手机端可完全脱离电脑脱机自主工作。
   - **全功能 PTY 终端 Dock**：支持 TUI 界面、快捷操作键栏、ANSI 高亮渲染与后台持久任务监控。

5. **多模态与双向文件系统穿透网关 (Cross-Boundary File Gateway)**：
   - **容器与宿主透明转换**：原生支持直接输入或挂载容器内绝对路径（如 `/root/...`），自动穿透校验物理映射。
   - **多模态附件无缝上屏**：支持图片选择、SAF 文档导入与文件夹挂载，自动转换为容器可读路径并作为多模态上下文喂给主脑。

6. **严密的工程执行协议与自闭环准则**：
   - 严格遵循“现状无偏采样 $\to$ 假说池显式展开 $\to$ 现场双向证伪 $\to$ 证据链收敛”的故障排查协议。
   - 拒绝未经验证的推测，一切结论必须依托真实的截图、日志或命令输出作为直接物理证据。

---

## 📐 系统拓扑图

```
用户交互入口
  ├─ 物理按键 / 系统手势唤醒 (长按电源键 / 快捷手势)
  │    └─ 系统级 Hook 拦截
  │         └─ IPC 通信
  │              └─ Cortex 核心运行服务
  │
  └─ Cortex App 原生交互界面 (MainActivity.kt)
       ├─ Antigravity 风格多项目与会话隔离抽屉 (按最近使用排序、独立归档/删除)
       ├─ 多模态附件上传与路径穿透网关 (AgentFileReferenceGateway.kt)
       └─ Cortex 流式会话渲染器 (CortexRuntime.kt)
            │
            │ 1. 启动 chroot 内核隔离环境 (run_chroot.sh)
            │ 2. 动态启动 Loopback MCP Server (127.0.0.1:40357/mcp)
            │ 3. 双向 NDJSON 流式管道 (--input-format stream-json --output-format stream-json)
            ▼
    ========================================================================
    【Android 本机 Debian arm64 chroot 环境】
        ┌──────────────────────────────────────────────────────────────┐
        │ 代理探测 (127.0.0.1:7890) / Keyring 解锁 / D-Bus 守护进程     │
        │                              ↓                               │
        │ Cortex 核心 Agent 决策主脑                                   │
        │   - 实时思维链推理 (Thinking Block)                          │
        │   - 全局行为准则与端到端真实核验协议                          │
        └──────────────────────────────────────────────────────────────┘
                  │
                  │ MCP HTTP JSON-RPC (tools/list, tools/call)
                  │ 环回免代理 (no_proxy="localhost,127.0.0.1")
                  ▼
    【Cortex MCP Server (Loopback HTTP Server)】
        │
        ├─ launch_app (应用唤起与任务栈隔离)
        ├─ tap / swipe / input_text / press_key (抗风控拟人化 GUI 操作)
        ├─ run_shell_command (Root / User Shell 执行)
        ├─ get_device_info / get_system_state (设备状态感知)
        ├─ take_screenshot / read_screen (副屏/主屏视觉捕获)
        └─ retrieve_notifications / get_clipboard (通知与剪贴板读取)
```

---

## 📂 项目结构导览

- **`app/`**：Cortex Android 宿主源码 (Kotlin / Jetpack Compose / LibXposed)。
  - `app/src/main/kotlin/com/cortex/ai/agent/agy/`：Cortex 单主脑核心集成层（`AgyRuntime`、`CortexMcpServer`、`AgyProjectRepository`、`AgyStreamSession`）。
  - `app/src/main/kotlin/com/cortex/ai/agent/device/`：设备控制与文件路径穿透网关（`AgentFileReferenceGateway`、`RootShellDeviceController`）。
  - `app/src/main/kotlin/com/cortex/ai/agent/terminal/`：Debian chroot 容器与终端服务。
  - `app/src/main/kotlin/com/cortex/ai/ui/`：Cortex 交互界面与视觉体系。
  - `app/src/main/kotlin/com/cortex/ai/hook/`：厂商系统入口接管与电源键拦截。
- **`agy-environment/`**：手机端 chroot 运行环境与主脑配置。
  - `scripts/run_chroot.sh`：命名空间隔离运行脚本。
  - `scripts/setup-debian-chroot.sh`：自动化容器部署脚本。
  - `config/AGENTS.md`：全局 Agent 执行协议与判定准则。
- **`docs/`**：系统架构白皮书与实机验收矩阵。
  - `docs/AGY_ANDROID_HANDOFF.md`：全量验收矩阵与技术交接规范。
  - `docs/TECHNICAL.md`：底层技术实现与 Hook 原理说明。
