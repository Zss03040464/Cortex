#!/bin/sh
# ==============================================================================
# Android Root Shell -> Debian chroot 环境启动脚本
# 部署路径: /data/local/tmp/run_chroot.sh
# 作用: 通过 unshare 和 busybox 挂载系统核心虚拟文件系统，chroot 进入 Debian 环境执行命令
# ==============================================================================

ROOTFS=/data/data/com.cortex.ai/files/terminal/debian/rootfs
BB=/data/adb/ksu/bin/busybox

unshare -m sh -c '
  ROOTFS="$1"
  BB="$2"
  shift 2
  "$BB" mount --make-rprivate /
  mount -t proc proc "$ROOTFS/proc" 2>/dev/null
  "$BB" mount --rbind /dev "$ROOTFS/dev" 2>/dev/null
  mount -t sysfs sysfs "$ROOTFS/sys" 2>/dev/null
  "$BB" mount --bind /storage/emulated/0 "$ROOTFS/storage/emulated/0" 2>/dev/null
  "$BB" mount --bind /storage/emulated/0 "$ROOTFS/sdcard" 2>/dev/null
  "$BB" mount --bind /storage/emulated/0 "$ROOTFS/root/sdcard" 2>/dev/null
  "$BB" mount --bind /data/local/tmp "$ROOTFS/data/local/tmp" 2>/dev/null
  export TERM=xterm-256color
  export HOME=/root
  export USER=root
  export ANDROID_SERIAL="127.0.0.1:5555"
  export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
  chroot "$ROOTFS" "$@"
' -- "$ROOTFS" "$BB" "$@"
