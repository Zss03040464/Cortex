#!/bin/bash
# ==============================================================================
# Android Debian chroot 环境一键安装与初始化脚本
# 适用平台: Android arm64 (OnePlus 15T / Android 16 / KernelSU-Next)
# 作用:
#   1. 在 chroot 内部安装 Agy CLI 所需的核心系统依赖：
#      - dbus, dbus-x11, gnome-keyring, libsecret-tools (Keyring 认证体系)
#      - python3, python3-pip, curl, ca-certificates, jq, busybox
#   2. 配置 /usr/local/bin/agy 与 /usr/local/bin/agy-wrapper
#   3. 配置 /root/.gemini 目录结构、AGENTS.md 规则与 mcp_config.json
# ==============================================================================

set -e

echo "[+] 开始配置 Debian arm64 chroot 环境..."

# 1. 更新 APT 并安装核心认证与运行依赖
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y \
    curl \
    ca-certificates \
    dbus \
    dbus-x11 \
    gnome-keyring \
    libsecret-tools \
    python3 \
    python3-pip \
    jq \
    procps \
    git

# 2. 部署 agy-wrapper
WRAPPER_SRC="$(dirname "$0")/agy-wrapper"
if [ -f "$WRAPPER_SRC" ]; then
    cp "$WRAPPER_SRC" /usr/local/bin/agy-wrapper
    chmod 755 /usr/local/bin/agy-wrapper
    echo "[+] 已安装 /usr/local/bin/agy-wrapper"
fi

# 3. 初始化 ~/.gemini 目录
mkdir -p /root/.gemini/config
mkdir -p /root/.gemini/antigravity-cli

# 4. 拷贝 AGENTS.md
RULES_SRC="$(dirname "$0")/../config/AGENTS.md"
if [ -f "$RULES_SRC" ]; then
    cp "$RULES_SRC" /root/.gemini/config/AGENTS.md
    chmod 644 /root/.gemini/config/AGENTS.md
    echo "[+] 已配置 /root/.gemini/config/AGENTS.md"
fi

# 5. 提示 Token 导入
echo "================================================================================"
echo "[+] Debian chroot 依赖安装与环境初始化完成！"
echo "[!] 注意：请将 Antigravity OAuth Token 写入："
echo "    /root/.gemini/jetski-standalone-oauth-token"
echo "    权限设置为 600 (chmod 600)。"
echo "    首次运行时，agy-wrapper 会自动将该 Token 导入 gnome-keyring。"
echo "================================================================================"
