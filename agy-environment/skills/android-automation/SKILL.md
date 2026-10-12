---
name: android-automation
description: Specialized protocol and best practices for Android device automation, ADB commands, Root shell, virtual display management, and background App control. Use whenever interacting with Android devices, running ADB operations, managing phone displays, or automating mobile apps.
---

# Android 手机操作规范：后台执行优先

凡涉及 Android 手机、ADB、Root、应用控制、自动化、UI 操作时，必须优先选择不会干扰用户当前前台使用的后台执行方案。

## 一、核心原则

**能后台就后台；必须 GUI 就放到独立虚拟 Display；只有无法实现后台或隔离操作时，才允许占用用户手机前台。后台化不能以污染主桌面、克隆 App 或无必要创建额外用户环境为代价。**

## 二、执行优先级

1. **纯后台操作优先**
   - 优先使用 ADB shell、Root shell、Intent、Broadcast、Service、ContentProvider、文件/数据库修改、系统 API、包管理、权限管理等无需切换手机前台界面的方式。
   - 如果任务能够完全通过后台命令实现，不得启动或切换用户当前前台 App。

2. **必须操作 GUI 时，优先使用独立虚拟 Display**
   - 如果任务确实需要点击、滑动、输入、读取 GUI 等交互，优先将目标 App 启动到独立虚拟 Display 中执行。
   - 用户的物理主屏幕 Display 0 应保持可正常使用，不得因为 Agent 操作其他 App 而抢占、切换或遮挡用户当前界面。
   - 优先直接将现有 App 的 Activity 启动到虚拟 Display，而不是通过主屏幕切换 App。

3. **保持系统干净**
   - 不得仅为了后台操作而：
     - 克隆 App；
     - 修改 App package name；
     - 创建第二 Android 用户；
     - 创建 Work Profile；
     - 创建长期存在的 App 副本；
     - 在用户主桌面新增快捷方式或同名图标。
   - 如果某种方案必须创建额外对象，应优先使用临时对象，并在任务完成后清理。
   - 如果必须保留 Agent 专用入口，应集中放在独立 Agent 文件夹、独立虚拟桌面或其他不会污染用户日常主桌面的区域。

4. **虚拟 Display 生命周期与技术实现红线（严防画中画与键盘抢占）**
   - **严禁使用 `settings put global overlay_display_devices`**：
     该命令对应 Android 开发者选项中的“模拟辅助显示屏”。其底层机制是 AOSP `OverlayDisplayAdapter`，系统硬编码会在物理主屏幕 Display 0 强制贴出半透明画中画浮窗（`OverlayDisplayWindow`），且搜索框获焦时会唤起软键盘并抢夺物理主屏焦点，严重干扰前台用户！**全系统绝对禁止使用**！
   - **唯一合规方案：离屏纯内存渲染 Headless VirtualDisplay（`virtual_display.sh`）**：
     必须通过底层 `IDisplayManager.createVirtualDisplay` 并将 Surface 绑定至纯内存 `ImageReader`（Off-screen Surface）。物理主屏 Display 0 **绝对零浮窗、零画中画、零覆盖层**。
   - **输入法键盘防抢占红线（`DISPLAY_IME_POLICY_HIDE = 2`）**：
     在 Android 多屏机制中，副屏默认采用 `DISPLAY_IME_POLICY_FALLBACK_DISPLAY (1)`，当副屏中的输入框（EditText）获焦时，系统会自动回退到主屏幕 Display 0 弹出软键盘，严重干扰前台用户！
     **核心技术标准**：创建虚拟 Display 后，必须立即通过 `IWindowManager.setDisplayImePolicy(displayId, 2)`（`DISPLAY_IME_POLICY_HIDE = 2`）将该副屏的输入法策略设为完全静默隐藏，彻底杜绝回退弹窗与软键盘抢占。文字输入通过底层的直接按键或节点注入完成，无需弹出软键盘。
   - **Activity 启动多任务隔离规范**：
     在虚拟屏启动应用时，必须附带 `--activity-new-task --activity-multiple-task` 参数（例如 `am start --display <id> --activity-new-task --activity-multiple-task -n <cmp>`），防止 ActivityManager 将主屏幕已存在的任务栈重排到前台。
   - **生命周期管理**：
     - 创建后台屏：`virtual_display create [width] [height] [dpi]`
     - 截取副屏画面：`virtual_display capture <path.png>`
     - 拟人抗风控点击：`virtual_display tap <x> <y> [duration_ms]`
     - 销毁并清理资源：`virtual_display destroy`
     - 优先按需创建临时虚拟 Display；任务完成后，如没有持续运行需求，必须显式调用 `virtual_display destroy` 销毁 Display 并清理临时资源。
     - 不得因为方便而无意义地长期保留多个 Display、Launcher、快捷方式或 App 实例。


5. **只有无法后台完成时才允许前台操作**
   - 在确认 ADB/Root 后台方案和虚拟 Display 方案均不可行、目标 App 不兼容或任务本身必须作用于用户物理主屏幕后，才允许切换到 Display 0 前台执行。
   - 在切换用户前台之前，应先判断是否存在其他不抢占前台的实现方式。
   - 不得把“直接 `adb shell input tap/swipe` 操作主屏幕”作为默认方案。

6. **操作选择原则**
   默认按以下顺序选择：
   `ADB/Root 无界面后台操作`
   → `独立虚拟 Display GUI 操作`
   → `Agent 专用隔离环境`
   → `用户物理主屏幕前台操作`

7. **用户正在使用手机时的防打扰要求**
   - 默认认为用户可能正在 Display 0 上刷视频、聊天、游戏或进行其他操作。
   - Agent 的任务不得无必要中断用户当前界面、抢焦点、返回桌面、切 App、弹出覆盖层或进行可避免的前台交互。

## 三、设备唤醒与自动解锁

当任务确需在物理主屏幕（Display 0）执行，且设备当前处于息屏或锁屏状态时，遵循以下解锁规范：

1. **凭据与脚本配置**：
   - **密码凭据文件**：`~/.config/android/pin.txt`（仅用户私有权限 `chmod 600`，存储 6 位数字 PIN 码）
   - **自动化解锁脚本**：`~/.config/android/unlock.sh`
2. **执行规则与特性**：
   - **智能防重入**：脚本会自动读取系统 Keyguard 和 Trust 状态；若设备已处于解锁状态，会直接跳过退出，严禁无必要地重入或打扰当前运行的应用。
   - **双模自适应（日常锁屏 & 重启开机）**：
     - *日常锁屏*：检测到支持指纹时，先点击切换按钮呼出数字键盘并注入密码；
     - *重启开机（Direct Boot / Strong Auth）*：系统强制禁用生物识别并直出数字键盘，脚本自动识别该状态跳过切换动作，避免误触数字键 2。
   - **自动进桌面**：解锁后自动注入 HOME 键直达 Launcher 桌面。
   - **调用时机**：在确认需要操作 Display 0 且检测到设备处于锁屏/休眠状态时，优先直接执行 `~/.config/android/unlock.sh` 恢复可用状态。

## 四、无线 ADB 与远程投屏规范

手机底层已通过 KernelSU 开机常驻 Linux 原生 Tailscale 与 TCP 5555 端口（与 flclash 零冲突，不依赖 Windows 开机，且通过后台 Watchdog 完全与系统“开发者选项”解耦，即使手机系统设置中关闭“开发者选项”也稳定常驻可用）：
- **无线连接命令**：若当前未接入设备，执行 `adb connect 100.76.32.56:5555`。
- **多设备指定**：当 USB 与无线同时在线时，执行命令指定 `-s 100.76.32.56:5555`。
- **远程全速投屏**：Mac 终端直接运行 `scrcpy -m 1080 -b 4M --max-fps 60`（若需息屏操作追加 `--turn-screen-off`）。

## 五、拟人化抗风控触控与系统级仿真规范

为大幅降低社交、电商及内容平台（如小红书、微信、美团等）的风控判定与脚本拦截，严禁使用粗暴的机械式命令，必须遵循拟人化触控标准：

1. **严禁瞬间零延迟点击（Anti-0ms Tap）**：
   - 传统的 `input tap x y` 会在毫秒级内连续抛出 `ACTION_DOWN` 与 `ACTION_UP`（持续时长几乎为 0ms），极易被应用内置的风控 SDK 识别为自动化外挂。
   - **拟人化替代方案**：使用带有自然接触时长（50ms ~ 120ms）与微小接触位移（1~2px）的滑动命令来模拟真实人手指腹按压过程：
     ```bash
     # 物理屏幕拟人点击（时长 75ms，微位移 1px）
     input swipe <x> <y> <x+1> <y+1> 75
     # 虚拟屏幕拟人点击：
     input -d <displayId> swipe <x> <y> <x+1> <y+1> 75
     ```
2. **滑动与滚动仿真**：
   - 翻页与长列表浏览禁止使用瞬时短滑；正常手势滑动时长应控制在 `300ms ~ 650ms` 之间，起始点与终点应带有随机像素偏差，避免完美的绝对直线轨迹。
3. **虚拟 Display 触控隔离**：
   - 使用 `input -d <displayId>` 派发事件时，Android 原生 `InputDispatcher` 将触控精准派发给目标虚拟屏活动窗口，物理主屏幕 Display 0 完全不会丢失焦点或受到任何干扰。

## 六、安全红线防护（防误重启与不可逆操作）

1. **绝对禁止未经确认的重启与关机**：
   - Agent 严禁在日常诊断或排错中私自执行 `reboot`、`shutdown`、`svc power reboot`、`setprop ctl.restart` 或格式化分区的危险操作。
   - 若系统级变更（如模块激活、系统属性生效）确实必须依赖系统重启，Agent 必须停下来向用户提交清晰报告，严禁擅作主张。
2. **具备自动解锁时的受控重启**：
   - 仅当任务流程中已获得用户明确授权重启，且本地具备有效的 `~/.config/android/unlock.sh` 脚本和 PIN 码时，才允许执行重启，并在系统重新上线后立即通过 Tailscale/ADB 执行解锁脚本恢复可用状态。
