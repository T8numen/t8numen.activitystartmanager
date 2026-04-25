# Xposed Modules Repo 提交说明

官方要求以 Xposed Modules Repo 当前说明为准：

- https://github.com/Xposed-Modules-Repo
- https://github.com/Xposed-Modules-Repo/submission
- https://modules.lsposed.org/submission/

## 本模块提交信息

- 模块名：`Activity启动管理`
- 包名：`t8numen.activitystartmanager`
- GitHub 仓库名：`t8numen.activitystartmanager`
- GitHub 仓库描述：`Activity启动管理`
- 当前候选版本：`1.6.8`
- 当前候选 versionCode：`39`
- Release tag：`39-1.6.8`

## 首次发布步骤

1. 在 GitHub 创建公开仓库 `t8numen.activitystartmanager`。
2. 确认仓库描述为 `Activity启动管理`。
3. 推送源码，仓库根目录保留 `README.md` 和 `SUMMARY`。
4. 创建 GitHub Release，tag 使用 `39-1.6.8`。
5. 上传 release APK、SHA256 校验文件和源码归档。
6. 通过 https://modules.lsposed.org/submission/ 提交包名。

## Release 文案模板

```text
Activity启动管理 1.6.8

测试环境：
- LSPosed 1.9.2
- ColorOS / Android 15

主要变化：
- 新增两段式参与方规则：`agree A`、`disagree A`、`ask A`。
- 修复规则编辑区右侧空白无法像普通编辑框一样点按定位的问题。
- 底部空白区继续保留防误选最后一行文本的保护逻辑。

安装/更新：
- 安装后在 LSPosed 中启用模块。
- 涉及 system_server hook，建议重启手机。

风险提示：
- 错误规则可能影响应用启动。
- 如误配导致异常，可禁用模块并重启后再修改规则。
```

## 注意事项

- 不要上传 debug APK。
- 不要提交 `release-signing.properties` 或 `local-signing/`。
- 后续每个版本的 Release tag 都应使用 `VERSION_CODE-VERSION_NAME`。
