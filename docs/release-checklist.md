# 发布检查清单

适用于 `Activity启动管理`，包名为 `t8numen.activitystartmanager`。

## 版本准备

1. 更新 `gradle.properties` 中的 `VERSION_CODE` 和 `VERSION_NAME`。
2. 在 `history.md` 顶部新增本版本变更记录。
3. 同步更新 `README.md` 中的当前版本和 Xposed Modules Repo Release tag。
4. 确认 `SUMMARY` 是一句简短模块描述。

## 签名准备

发布 APK 必须使用稳定 release 签名，不应使用 debug/test 签名。

仓库根目录需要本地文件 `release-signing.properties`：

```properties
storeFile=local-signing/activitystartmanager-release.jks
storePassword=...
keyAlias=activitystartmanager
keyPassword=...
```

注意：

- `release-signing.properties` 和 `local-signing/` 已加入 `.gitignore`。
- 发布前应离线备份 `local-signing/` 和 `release-signing.properties`。
- 如果丢失 release keystore，后续版本将无法覆盖安装旧版本。

## 构建验证

运行：

```powershell
$env:GRADLE_USER_HOME = (Join-Path (Get-Location) '.gradle-local')
.\gradlew.bat :activitystartmanager:testDebugUnitTest :activitystartmanager:assembleRelease
```

确认：

- 单元测试通过。
- `apks/` 中生成 `activitystartmanager-release-v版本号.apk`。
- APK 使用 release 签名，而不是 debug/test 签名。

## 实机验证

安装 release APK 到测试机：

```powershell
adb install -r apks/activitystartmanager-release-v版本号.apk
```

至少确认：

- 应用可打开。
- 版本号正确。
- 悬浮窗权限状态正确。
- LSPosed 中模块可识别。
- 重启后 `agree/disagree/ask` 三类规则都能命中。
- `ask` 悬浮窗可以同意、拒绝、复制 Activity 文本。

## GitHub Release

Release tag 使用：

```text
VERSION_CODE-VERSION_NAME
```

例如：

```text
40-1.6.9
```

Release 至少上传：

- `activitystartmanager-release-v版本号.apk`
- `SHA256SUMS-v版本号.txt`
- 源码归档 zip

Release 文案应包含：

- 版本号和日期。
- 主要变化。
- 测试环境。
- 是否需要重启手机或 LSPosed 作用域。
- 已知风险和恢复方式。

## Xposed Modules Repo 提交

首次发布后，向 Xposed Modules Repo 提交：

- 仓库名：`t8numen.activitystartmanager`
- 仓库描述：`Activity启动管理`
- issue 标题：`[submission] t8numen.activitystartmanager`

提交入口：

- https://modules.lsposed.org/submission/
- https://github.com/Xposed-Modules-Repo/submission
