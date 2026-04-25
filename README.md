# Activity启动管理

`Activity启动管理` 是一个 LSPosed 模块，用于在系统框架层按规则管理 Android Activity 之间的启动行为。

当前支持三类规则：

- `agree`：允许来源 Activity 启动目标 Activity。
- `disagree`：拒绝来源 Activity 启动目标 Activity。
- `ask`：在未被 `agree/disagree` 命中时弹出底部悬浮窗，允许用户本次同意或拒绝。

`allow` 可作为 `agree` 的别名。

## 基本信息

- 应用名：`Activity启动管理`
- 包名：`t8numen.activitystartmanager`
- 当前版本：`1.6.6`
- Xposed Modules Repo Release tag：`37-1.6.6`
- 推荐环境：LSPosed 1.9.2、已 Root 设备
- 已测试环境：ColorOS / Android 15

## LSPosed 作用域

模块声明的作用域：

- `android`
- `com.oplus.securitypermission`
- `com.coloros.securitypermission`
- `com.android.permissioncontroller`

`android` 用于 hook 系统框架 Activity 启动链路；权限管理器相关包用于尽量绕过 ColorOS / OPlus 的二次拉起确认弹窗。

## 权限说明

- `SYSTEM_ALERT_WINDOW`：用于显示 `ask` 规则命中后的底部悬浮询问窗。
- `QUERY_ALL_PACKAGES`：用于读取来源/目标应用名称和图标，便于在询问窗与记录中展示。

模块不主动联网，规则和最近记录保存在本地。

## 规则格式

每行一条规则：

```text
<agree|disagree|ask> <来源> <目标>
```

支持写法：

- `*`：匹配任意 Activity。
- `system`：匹配系统 UID、系统预装应用或更新系统应用。
- `com.example.app`：匹配包名。
- `com.example.app.*`：匹配该包及子包。
- `com.example.app/.MainActivity`：匹配具体 Activity。

示例：

```text
agree * com.android.intentresolver/.ChooserActivityLauncher
allow * com.android.documentsui/.picker.PickActivity
agree * com.sspai.cuto.android
ask bin.mt.plus com.openai.chatgpt
ask * org.videolan.vlc
ask * *
```

## 执行顺序

规则按顺序匹配。先匹配 `agree/disagree`，两者都未命中时再匹配 `ask`。

应用内 Activity 跳转默认同意，不需要写规则；如果需要接管应用内跳转，可显式添加：

```text
ask com.example.app com.example.app
disagree com.example.app com.example.app
```

## 风险与恢复

这是系统框架层模块，错误规则可能影响应用正常打开。公开使用前建议保留以下基础放行规则：

```text
allow com.android.launcher *
agree * com.android.intentresolver/.ChooserActivityLauncher
allow * com.android.documentsui/.picker.PickActivity
```

如果误配规则导致无法正常使用：

1. 在 LSPosed 中禁用模块。
2. 重启手机或重启作用域进程。
3. 打开模块应用修改规则。

如果系统权限管理器弹窗仍优先出现，可先在系统弹窗中选择永远允许，再交给模块规则管理。

## 构建

调试构建：

```powershell
$env:GRADLE_USER_HOME = (Join-Path (Get-Location) '.gradle-local')
.\gradlew.bat :activitystartmanager:testDebugUnitTest :activitystartmanager:assembleDebug
```

发布构建：

```powershell
$env:GRADLE_USER_HOME = (Join-Path (Get-Location) '.gradle-local')
.\gradlew.bat :activitystartmanager:testDebugUnitTest :activitystartmanager:assembleRelease
```

发布签名配置读取仓库根目录的 `release-signing.properties`，该文件和本地 keystore 不应提交到 Git。

## 发布

Xposed Modules Repo 要求：

- GitHub 仓库名：`t8numen.activitystartmanager`
- 仓库描述：`Activity启动管理`
- Release tag：`37-1.6.6`
- Release 资产：上传 release APK
- 仓库根目录保留 `SUMMARY` 和 `README.md`

提交入口：

- https://modules.lsposed.org/submission/
- https://github.com/Xposed-Modules-Repo/submission

更多发布步骤见：

- `docs/release-checklist.md`
- `docs/release-assets.md`
- `docs/xposed-modules-repo-submission.md`

## 许可证

本项目采用 `GPL-3.0-only` 许可证，详见 [LICENSE](LICENSE)。

## 致谢

本项目早期构建和开发参考了 [MagicianGuo/Android-XposedTest](https://github.com/MagicianGuo/Android-XposedTest) 的项目结构和 Xposed 模块示例思路，感谢原作者提供的学习参考。
