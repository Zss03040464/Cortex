# Agy 运行环境与设备端配置 (agy-environment)

本项目目录收录了在 Android Root 设备（Debian chroot）上运行 Antigravity CLI (Agy) 作为唯一 Agent 决策主脑所需的全部运行环境脚本、代理与鉴权配置、守护进程管理及安全隔离方案。

---

## 目录结构

```
agy-environment/
├── config/
│   ├── AGENTS.md                                # 全局 Agent 行为准则与任务完结验收规则
│   ├── mcp_config.json                          # Agy MCP 客户端配置 (指向 Eta 宿主 Loopback HTTP Server)
│   └── jetski-standalone-oauth-token.template   # Google OAuth Token 格式模板
├── scripts/
│   ├── agy-wrapper                              # Agy 启动包装器 (代理/Keyring/D-Bus/Token 注入)
│   ├── run_chroot.sh                            # chroot 挂载与独立命名空间隔离运行脚本
│   └── setup-debian-chroot.sh                   # Debian arm64 chroot 一键自动化部署脚本
└── README.md                                    # 本说明文档
```

---

## 核心机制说明

### 1. 内核命名空间隔离 (`run_chroot.sh`)
- **问题**：在 Android 上挂载 chroot 时，如果直接在全局 mount 空间执行，会导致几十个挂载点泄漏到 Android 宿主，可能被某些银行/金融 App 检测出环境异常。
- **解法**：使用 `busybox unshare -m` 创建隔离的 Mount 命名空间，并对根执行 `mount --make-rprivate /`。在 chroot 退出或子进程结束后，宿主 `/proc/mounts` 恒为 0 泄漏。

### 2. 独立联网与 Loopback 旁路 (`agy-wrapper`)
- **问题**：手机通常通过 VPN/代理（如 FlClash `127.0.0.1:7890`）访问 Google 服务；但如果所有流量都走代理，Agy 访问宿主 Eta 的 MCP 服务（`127.0.0.1:<port>`）就会被代理劫持导致连接失败。
- **解法**：`agy-wrapper` 自动探测 `127.0.0.1:7890`，并强制设置：
  ```bash
  export no_proxy="localhost,127.0.0.1"
  export NO_PROXY="localhost,127.0.0.1"
  ```
  使外网模型请求直通 FlClash，内网 MCP RPC 稳定直通 Android 宿主。

### 3. 无头 D-Bus 与 GNOME Keyring 鉴权
- **问题**：Agy CLI 依赖 GNOME Keyring 保存和读取 OAuth Token。Android chroot 属于无头环境，没有 X11/Wayland 桌面会话，默认无法访问 Secret Service。
- **解法**：在 `agy-wrapper` 中自动维护独立的 D-Bus 守护进程：
  ```bash
  eval $(dbus-launch --sh-syntax)
  export DBUS_SESSION_BUS_ADDRESS="unix:path=/tmp/dbus-agy-sock"
  ```
  配合 `gnome-keyring-daemon --unlock`，实现免桌面无缝访问官方账号鉴权。

### 4. 任务完成判定准则 (`AGENTS.md`)
针对 Android GUI Agent 常见的“输入完联想词即误判完成”的问题，定义了严格的完结准则：
- 搜索任务必须真正提交并进入正式搜索结果列表；
- Native App 优先，禁止无故回退到浏览器；
- 必须基于屏幕视觉结果进行主动验收。

---

## 快速安装与配置

1. **一键搭建 chroot**：
   在手机 Root Shell 下执行：
   ```bash
   sh /data/local/tmp/setup-debian-chroot.sh
   ```
2. **部署包装脚本**：
   将 `agy-wrapper` 放置在 chroot 内 `/usr/local/bin/agy`。
3. **配置 Token**：
   将已授权的 OAuth Token 放置在 `~/.config/antigravity/jetski-standalone-oauth-token`。
4. **配置 MCP**：
   将 `mcp_config.json` 复制到 `~/.gemini/config/mcp_config.json`。
