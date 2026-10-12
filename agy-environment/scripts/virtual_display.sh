#!/bin/sh
# ==============================================================================
# Android 真·离屏 Headless 虚拟屏幕管理工具 (Zero-Overlay, No-PIP, No-IME Grab)
# 
# 严禁使用 settings put global overlay_display_devices（该命令会强弹画中画并抢键盘）！
# 本工具通过 app_process 加载 headless_server.dex，将 VirtualDisplay 绑定至纯内存
# ImageReader Off-screen Surface，物理主屏 (Display 0) 绝对零浮窗、零遮挡、零键盘抢占。
#
# 用法:
#   virtual_display.sh create [width] [height] [dpi]  # 启动后台无感虚拟屏并输出 ID
#   virtual_display.sh capture <output_png_path>     # 抓取虚拟屏画面
#   virtual_display.sh tap <x> <y> [duration_ms]     # 拟人化抗风控点击虚拟屏
#   virtual_display.sh swipe <x1> <y1> <x2> <y2> [dur] # 虚拟屏手势滑动
#   virtual_display.sh text <string>                 # 虚拟屏文字输入
#   virtual_display.sh destroy                       # 销毁虚拟屏并清理临时资源
# ==============================================================================

ACTION="$1"
WIDTH="${2:-1080}"
HEIGHT="${3:-2400}"
DPI="${4:-420}"

PID_FILE="/data/local/tmp/headless_display.pid"
ID_FILE="/data/local/tmp/headless_display.id"
DEX_PATH="/data/local/tmp/headless_server.dex"

case "$ACTION" in
  create|start)
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE" 2>/dev/null)" 2>/dev/null; then
      ID=$(cat "$ID_FILE" 2>/dev/null)
      echo "{\"status\":\"OK\",\"display_id\":${ID:-1},\"resumed\":true}"
      exit 0
    fi
    rm -f "$PID_FILE" "$ID_FILE"
    export CLASSPATH="$DEX_PATH"
    nohup app_process / HeadlessDisplayServer "$WIDTH" "$HEIGHT" "$DPI" > /data/local/tmp/headless_server.log 2>&1 &

    for i in $(seq 1 50); do
      if [ -f "$ID_FILE" ] && [ -s "$ID_FILE" ]; then
        ID=$(cat "$ID_FILE")
        PID=$(cat "$PID_FILE")
        echo "{\"status\":\"OK\",\"display_id\":$ID,\"pid\":$PID}"
        exit 0
      fi
      sleep 0.1
    done
    echo "{\"error\":\"Failed to start headless display\",\"log\":\"$(cat /data/local/tmp/headless_server.log 2>/dev/null)\"}"
    exit 1
    ;;

  launch)
    PKG_OR_CMP="$2"
    if [ -z "$PKG_OR_CMP" ]; then
      echo "{\"error\":\"Missing package or component to launch\"}"
      exit 1
    fi
    ID=$(cat "$ID_FILE" 2>/dev/null)
    if [ -z "$ID" ]; then
      echo "{\"error\":\"No active headless display\"}"
      exit 1
    fi
    if echo "$PKG_OR_CMP" | grep -q "/"; then
      am start --display "$ID" --activity-new-task --activity-multiple-task -n "$PKG_OR_CMP" >/dev/null 2>&1
    else
      MAIN_ACT=$(cmd package resolve-activity --brief "$PKG_OR_CMP" 2>/dev/null | tail -n 1)
      if [ -n "$MAIN_ACT" ] && [ "$MAIN_ACT" != "No activity found" ]; then
        am start --display "$ID" --activity-new-task --activity-multiple-task -n "$MAIN_ACT" >/dev/null 2>&1
      else
        monkey -p "$PKG_OR_CMP" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
      fi
    fi
    echo "{\"status\":\"OK\",\"action\":\"launch\",\"target\":\"$PKG_OR_CMP\",\"display_id\":$ID}"
    ;;

  capture)
    if [ -n "$3" ]; then
      OUT_PATH="$3"
    else
      OUT_PATH="$2"
    fi
    if [ -z "$OUT_PATH" ]; then
      echo "{\"error\":\"Missing output path\"}"
      exit 1
    fi
    ID=$(cat "$ID_FILE" 2>/dev/null)
    if [ -z "$ID" ]; then
      echo "{\"error\":\"No active headless display\"}"
      exit 1
    fi
    SF_ID=$(dumpsys SurfaceFlinger --display-id 2>/dev/null | grep "Virtual display" | head -n 1 | awk '{print $2}')
    if [ -n "$SF_ID" ]; then
      screencap -d "$SF_ID" "$OUT_PATH" 2>/dev/null
    else
      screencap -d "$ID" "$OUT_PATH" 2>/dev/null
    fi
    if [ -f "$OUT_PATH" ] && [ -s "$OUT_PATH" ]; then
      echo "{\"status\":\"OK\",\"path\":\"$OUT_PATH\"}"
    else
      echo "{\"error\":\"Capture failed\"}"
      exit 1
    fi
    ;;

  tap)
    ID=$(cat "$ID_FILE" 2>/dev/null)
    if [ -z "$ID" ]; then
      echo "{\"error\":\"No active headless display\"}"
      exit 1
    fi
    if [ -n "$5" ] || [ "$2" = "$ID" ]; then
      X="$3"
      Y="$4"
      DUR="${5:-75}"
    else
      X="$2"
      Y="$3"
      DUR="${4:-75}"
    fi
    X2=$((X + 1))
    Y2=$((Y + 1))
    input -d "$ID" swipe "$X" "$Y" "$X2" "$Y2" "$DUR"
    echo "{\"status\":\"OK\",\"action\":\"tap\",\"display_id\":$ID,\"x\":$X,\"y\":$Y,\"duration\":$DUR}"
    ;;

  swipe)
    ID=$(cat "$ID_FILE" 2>/dev/null)
    if [ -n "$6" ] || [ "$2" = "$ID" ]; then
      X1="$3"
      Y1="$4"
      X2="$5"
      Y2="$6"
      DUR="${7:-350}"
    else
      X1="$2"
      Y1="$3"
      X2="$4"
      Y2="$5"
      DUR="${6:-350}"
    fi
    input -d "$ID" swipe "$X1" "$Y1" "$X2" "$Y2" "$DUR"
    echo "{\"status\":\"OK\",\"action\":\"swipe\",\"display_id\":$ID}"
    ;;

  text)
    ID=$(cat "$ID_FILE" 2>/dev/null)
    if [ -n "$3" ] || [ "$2" = "$ID" ]; then
      TXT="$3"
    else
      TXT="$2"
    fi
    input -d "$ID" text "$TXT"
    echo "{\"status\":\"OK\",\"action\":\"text\",\"display_id\":$ID}"
    ;;

  destroy|stop)
    if [ -f "$PID_FILE" ]; then
      PID=$(cat "$PID_FILE" 2>/dev/null)
      kill -15 "$PID" 2>/dev/null
      sleep 0.3
      kill -9 "$PID" 2>/dev/null
      rm -f "$PID_FILE" "$ID_FILE"
    fi
    echo "{\"status\":\"OK\",\"message\":\"Headless display destroyed\"}"
    ;;

  *)
    echo "Usage: $0 {create|capture|tap|swipe|text|destroy}"
    exit 1
    ;;
esac

