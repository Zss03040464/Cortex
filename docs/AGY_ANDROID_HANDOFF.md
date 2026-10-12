# Root Android / Cortex / Agy CLI 集成任务阶段性交付报告与验收矩阵

## 一、当前里程碑总览 (All Core Milestones PASS)

本阶段已完成**缺陷修复、官方接口真实性核验、网络独立性验证与端到端闭环验证**。
设备环境（一加 15T，Android 16 / SDK 36，KernelSU Next 33214，Zygisk Next 1.5.0，Vector 2.2，SELinux Enforcing）稳定干净，各层级均有详实 trace 凭证落盘。

### 核心架构
```
长按电源键 / Assistant 入口
        ↓
小布语音助手 (com.heytap.speechassist) [Vector Hook 注入]
        ↓
BreenoHooks (Cortex 适配层)
        ↓ (Android Messenger IPC / AgentRuntimeConnection)
AgentRuntimeService (com.cortex.ai)
        ↓
AgyRuntime (Debian chroot 管理器)
        ↓ (官方 NDJSON stream-json 双向流式)
Agy CLI (唯一主脑 / Debian arm64 chroot)
        ↓ (MCP Loopback HTTP RPC: 127.0.0.1:<dynamic_port>/<token>)
CortexMcpServer (Loopback HTTP Server)
        ↓ (本地 Tool 调度)
Cortex Android Tools (Accessibility / Root Shell / 设备直接调用等)
        ↓ (MCP JSON-RPC 结果回传)
Agy 整合上下文并流式返回 AssistantBlock 事件流
        ↓ (Messenger IPC)
BreenoHooks / StreamRenderer 渲染浮动卡片
```

核心原则贯彻：
1. **单一主脑**：移除对三方 LLM (Kimi/Sensenova 等) 的回退与判断，Agy 是唯一主脑，杜绝双 Agent 套娃。
2. **官方接口**：完全使用 Antigravity 官方公开的 `--input-format stream-json --output-format stream-json` 与标准 MCP JSON-RPC 协议。
3. **安全与后台优先**：全程后台执行，保护金融/银行隐藏环境；`run_chroot.sh` 实施 mount private 隔离，挂载泄漏完全归零。

---

## 二、六大重点验收项结论与证据索引

| 验收项 | 结论 | 核心凭证与说明 |
| :--- | :---: | :--- |
| **A. Cortex App 闪退修复** | **PASS** | Datastore UID 权限精准修复为 `10401:10401`，冷启动完全正常无崩溃，UI 渲染稳定。 |
| **B. 官方 stream-json 协议接口** | **PASS** | `diagnostics/agy-evidence/official_stream_json.log`<br>记录标准 NDJSON 双向通信：`init` -> `user_input` -> `agent_response` (delta) -> `result` (SUCCESS)。 |
| **C. 官方 MCP 协议工具调用** | **PASS** | `diagnostics/agy-evidence/mcp_protocol_trace.log`<br>记录标准 MCP 握手：`initialize` -> `tools/list` -> `tools/call: get_device_info` -> 宿主执行返回 `{"brand":"OnePlus","model":"PLZ110","android_version":"16"}`。 |
| **D. 官方账号认证与真实模型调用** | **PASS** | `diagnostics/agy-evidence/auth_real_request.json`<br>`gnome-keyring` + `jetski-standalone-oauth-token` 官方鉴权，真实模型请求成功返回。 |
| **E. FlClash 本地代理独立联网** | **PASS** | `agy-wrapper` 自动探测手机本机 FlClash（`127.0.0.1:7890`），且配置 `no_proxy="localhost,127.0.0.1"` 保证 Loopback MCP 互通，脱离电脑 `adb reverse` 独立联网。 |
| **F. 长按电源键唤醒端到端链路** | **READY FOR USER TEST** | 后台监听脚本实时就绪（`task-4044`），日志落盘至 `diagnostics/agy-evidence/power_long_press_trace.log`。等待用户手动长按一次电源键触发最终实测。 |

---

## 三、25 项全量验收矩阵

| 序号 | 验收项目 | 状态 | 证据 / 路径 / 判定依据 |
| :---: | :--- | :---: | :--- |
| 1 | Cortex App 冷启动无崩溃 | **PASS** | `am start -n com.cortex.ai/.ui.MainActivity` 正常进入 Compose UI，PID 保持存活。 |
| 2 | Cortex DataStore 权限正常 | **PASS** | `/data/data/com.cortex.ai/files/datastore` 属主为 `10401:10401`。 |
| 3 | KernelSU Next 授权保留 | **PASS** | `.allowlist` 包含 `10401`，UID 10401 执行 `su` 静默通过无弹窗。 |
| 4 | Debian chroot rootfs 完整 | **PASS** | `/data/data/com.cortex.ai/files/terminal/debian/rootfs` 结构完整可用。 |
| 5 | 内核命名空间隔离无泄漏 | **PASS** | `mount --make-rprivate /`，宿主 `grep debian /proc/mounts \| wc -l` 恒为 0。 |
| 6 | Agy CLI 可执行文件正常 | **PASS** | chroot 内 `/usr/local/bin/agy` 支持完整参数。 |
| 7 | Agy Keyring D-Bus 守护正常 | **PASS** | `/tmp/dbus-agy-sock` 守护正常，`secret-tool` 能够检索到 OAuth Token。 |
| 8 | 官方 OAuth 鉴权有效 | **PASS** | `auth_real_request.json`，无 API_KEY 环境变量，纯 Token 认证调通。 |
| 9 | 官方 Headless stream-json 协议 | **PASS** | `official_stream_json.log`，NDJSON 单行事件流完整覆盖。 |
| 10 | Loopback HTTP MCP Server | **PASS** | `CortexMcpServer.kt` 动态监听 `127.0.0.1:<port>`，Token 路径隔离鉴权。 |
| 11 | MCP Config 动态注入 | **PASS** | `/root/.gemini/config/mcp_config.json` 自动写入 `serverUrl`，权限对齐 UID 10401。 |
| 12 | 官方 MCP 协议握手 | **PASS** | `mcp_protocol_trace.log` 捕获 `initialize` 与 `protocolVersion: 2025-03-26`。 |
| 13 | 官方 MCP 工具列表发现 | **PASS** | `mcp_protocol_trace.log` 捕获 `tools/list` 并返回 Cortex 注册的 Android 专用工具。 |
| 14 | 官方 MCP 工具执行调用 | **PASS** | `mcp_protocol_trace.log` 捕获 `tools/call` 并成功读取设备参数。 |
| 15 | FlClash 本地代理直连 | **PASS** | 手机脱离电脑 `adb reverse`，通过 `127.0.0.1:7890` 独立连接 Google Cloud Code。 |
| 16 | FlClash Loopback 旁路豁免 | **PASS** | `agy-wrapper` 配置 `no_proxy="localhost,127.0.0.1"`，杜绝本地 MCP 流量被 VPN 劫持。 |
| 17 | 单一主脑架构强制锁定 | **PASS** | `BreenoHooks.kt` 移除旧模型 fallback，统一使用 `AgyBridgePolicy.config()`。 |
| 18 | 宿主 Unit Tests 全部通过 | **PASS** | `./gradlew :app:testDebugUnitTest --tests "com.cortex.ai.agent.agy.*"` 100% PASS。 |
| 19 | 真机覆盖安装无数据损失 | **PASS** | `adb install -r -d` 成功，chroot 及配置保持原样。 |
| 20 | Vector Module 保持启用 | **PASS** | `/data/adb/lspd/cli modules ls` 显示 `com.cortex.ai 10401 enabled`。 |
| 21 | Vector Scope 最小化限制 | **PASS** | `/data/adb/lspd/cli scope ls` 显示仅作用于 `com.heytap.speechassist 0`。 |
| 22 | 宿主 Formal Runtime 端到端 | **PASS** | `formal_runtime_trace.log`，UID 10401 下 `AgyVerifyMain` 成功完成全链路闭环返回 `CORTEX_AGY_READY`。 |
| 23 | 流式事件渲染派发正常 | **PASS** | `AssistantBlockStart`、`AssistantBlockDelta`、`AssistantBlockEnd` 顺序准确。 |
| 24 | 金融与隐藏环境零干扰 | **PASS** | 未修改 SELinux permissive，未修改 Zygote 全局环境，隐藏应用无感知。 |
| 25 | 长按电源键触发小布到 Agy | **READY** | 后台实时监听就绪，落盘 `power_long_press_trace.log`，等待用户手动按压电源键验证。 |

---

## 四、真实性证据文件清单

所有凭证均持久化保存在 `/Users/vivian/Workspace/code/root/diagnostics/agy-evidence/`：
- `auth_real_request.json`：证明官方账号与 Keyring 鉴权调用真实性。
- `official_stream_json.log`：证明使用 Antigravity 官方公开的 NDJSON 双向流式协议。
- `mcp_protocol_trace.log`：证明使用 Antigravity 官方 MCP 工具发现与 RPC 调用标准。
- `formal_runtime_trace.log`：证明在真实 Cortex UID 10401 环境下，`AgyRuntime` 启动、MCP 连接、流式生成全流程正常。
- `power_long_press_trace.log`：长按电源键全链路端到端实时捕获日志。
